package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ws.sodiunplugin.config.ShakeConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 在 HUD 的药水效果图标区域（右手边竖排图标）叠加两层增强：
 *
 * 1. 剩余时间：对每个效果在图标下方绘制剩余秒数（基于 1.21.11 反编译验证的布局，
 *    背景 24x24 在 (x,y)，图标 18x18 在 (x+3,y+3)）。
 * 2. 临期闪烁彩色边框：当效果剩余时间 <= 200 tick（=10 秒）时，在该图标外侧绘制
 *    随时间循环色相 + 正弦呼吸透明度的彩色边框。
 *
 * 注入点选择 renderStatusEffectOverlay 的 RETURN，自行按 vanilla 的相同排序与计数器
 * 复刻每个图标的 (x,y)，从而避免对原版内联 per-effect 绘制逻辑的脆弱单指令注入。
 *
 * 注意：注入方法不声明任何 MatrixStack/PoseStack 参数，否则会与原版方法签名不匹配导致运行时崩溃。
 */
@Mixin(InGameHud.class)
public class InGameHudPotionMixin {

    /** 触发边框的剩余时间阈值：200 tick = 10 秒。 */
    private static final int BORDER_TICK_THRESHOLD = 200;

    /** 图标背景尺寸，与 vanilla 保持一致。 */
    private static final int CELL = 24;

    /** 相邻两个图标之间的水平/垂直间距，与 vanilla 保持一致。 */
    private static final int STEP = 25;
    private static final int ROW_STEP = 26;

    @Inject(method = "renderStatusEffectOverlay",
            at = @At("RETURN"))
    private void onRenderStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        if (!ShakeConfig.getPotionTimeEnabled() && !ShakeConfig.getPotionBorderEnabled()) {
            return;
        }

        Collection<StatusEffectInstance> effects = client.player.getStatusEffects();
        if (effects.isEmpty()) {
            return;
        }

        // 复刻 vanilla 的排序：Ordering.natural().reverse()（按 StatusEffectInstance 自然序降序）。
        List<StatusEffectInstance> sorted = new ArrayList<>(effects);
        sorted.sort(Comparator.reverseOrder());

        int windowWidth = context.getScaledWindowWidth();
        int startY = 1;
        if (client.isDemo()) {
            startY += 15;
        }

        int beneficial = 0;
        int nonBeneficial = 0;

        // 用真实流逝时间驱动闪烁，不依赖游戏暂停/刻。
        long now = System.currentTimeMillis();
        float hue = (now % 2000L) / 2000.0f;                 // 每 2 秒循环一次完整色相
        float pulse = (float) (Math.sin(now / 110.0) * 0.5 + 0.5); // 0..1 呼吸
        int borderRgb = hsbToRgb(hue, 1.0f, 1.0f);
        int borderColor = ((int) (0x80 + 0x7F * pulse) << 24) | (borderRgb & 0x00FFFFFF);

        TextRenderer textRenderer = client.textRenderer;

        for (StatusEffectInstance se : sorted) {
            if (!se.shouldShowIcon()) {
                continue;
            }

            int x;
            int y;
            if (se.getEffectType().value().isBeneficial()) {
                beneficial++;
                x = windowWidth - STEP * beneficial;
                y = startY;
            } else {
                nonBeneficial++;
                x = windowWidth - STEP * nonBeneficial;
                y = startY + ROW_STEP * nonBeneficial;
            }

            int duration = se.getDuration();

            // ---- 临期彩色闪烁边框 ----
            if (ShakeConfig.getPotionBorderEnabled() && duration <= BORDER_TICK_THRESHOLD) {
                drawBorder(context, x, y, borderColor);
            }

            // ---- 剩余时间文本 ----
            if (ShakeConfig.getPotionTimeEnabled()) {
                int seconds = (duration + 19) / 20; // 向上取整到整秒
                if (seconds <= 0) {
                    seconds = 1;
                }
                String label = seconds + "s";
                int textWidth = textRenderer.getWidth(label);
                int tx = x + CELL / 2 - textWidth / 2;
                int ty = y + 22; // 基线靠近图标底部
                // 半透明深色条，保证文本在图标之上可读
                context.fill(x + 2, y + 14, x + CELL - 2, y + 23, 0xB0000000);
                context.drawText(textRenderer, Text.literal(label), tx, ty, 0xFFFFFFFF, false);
            }
        }
    }

    /** 在 24x24 图标外侧绘制 2px 彩色边框（top/bottom/left/right 四条矩形）。 */
    private static void drawBorder(DrawContext context, int x, int y, int color) {
        int bx = x - 2;
        int by = y - 2;
        int size = CELL + 4; // 28
        int th = 2;
        // 上
        context.fill(bx, by, bx + size, by + th, color);
        // 下
        context.fill(bx, by + size - th, bx + size, by + size, color);
        // 左
        context.fill(bx, by, bx + th, by + size, color);
        // 右
        context.fill(bx + size - th, by, bx + size, by + size, color);
    }

    /** HSB -> RGB（0xRRGGBB），纯计算，避免依赖 AWT。 */
    private static int hsbToRgb(float h, float s, float b) {
        h = (h % 1.0f + 1.0f) % 1.0f;
        int i = (int) (h * 6.0f);
        float f = h * 6.0f - i;
        float p = b * (1.0f - s);
        float q = b * (1.0f - f * s);
        float t = b * (1.0f - (1.0f - f) * s);
        float r, g, bl;
        switch (i % 6) {
            case 0 -> { r = b; g = t; bl = p; }
            case 1 -> { r = q; g = b; bl = p; }
            case 2 -> { r = p; g = b; bl = t; }
            case 3 -> { r = p; g = q; bl = b; }
            case 4 -> { r = t; g = p; bl = b; }
            default -> { r = b; g = p; bl = q; }
        }
        int ri = (int) (r * 255.0f + 0.5f);
        int gi = (int) (g * 255.0f + 0.5f);
        int bi = (int) (bl * 255.0f + 0.5f);
        return (ri << 16) | (gi << 8) | bi;
    }
}
