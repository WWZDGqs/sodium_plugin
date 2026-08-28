package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.config.ShakeConfig;
import ws.sodiunplugin.hud.DamageDisplayStore;

/**
 * 在 HUD 的 {@link InGameHud#renderStatusBars} 之后，于血量与饱食度上方绘制伤害显示条。
 *
 * 坐标依据原版 {@code renderStatusBars} 的常量：
 *   - 屏幕中心 x = width / 2
 *   - 血量区中心 x = width / 2 - 91，食物区中心 x = width / 2 + 91
 *   - 底部基线 y = height - 39
 * 伤害条绘制在基线上方（y = height - 39 - 10 起），即血/食图标正上方。
 */
@Mixin(InGameHud.class)
public class InGameHudDamageMixin {

    @Inject(
            method = "renderStatusBars(Lnet/minecraft/client/gui/DrawContext;)V",
            at = @At("RETURN")
    )
    private void onRenderStatusBars(DrawContext context, CallbackInfo ci) {
        if (!ShakeConfig.getDamageDisplayEnabled()) {
            return;
        }
        if (!DamageDisplayStore.isInGame()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        var entries = DamageDisplayStore.collectActive();
        if (entries.isEmpty()) {
            return;
        }
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        int centerX = width / 2;
        // 血/食条底部基线，参考原版 renderStatusBars 中的 height - 39
        int baseY = height - 39;
        // 伤害条绘制在基线上方约 10 像素处，多行向上堆叠
        int lineHeight = 10;
        int startY = baseY - 10;

        long nowMs = System.currentTimeMillis();

        int index = 0;
        for (var entry : entries) {
            float alpha = DamageDisplayStore.alphaFor(entry, nowMs);
            if (alpha <= 0.01f) {
                index++;
                continue;
            }
            int alphaByte = (int) (alpha * 255.0f) & 0xFF;
            // 颜色：白底 + alpha。drawText 的 shadow 模式会用此色绘制描边，alpha 生效。
            int color = (alphaByte << 24) | 0xFFFFFF;

            int y = startY - index * lineHeight;
            // 文本水平居中于屏幕中心（血/食条中间）
            int textWidth = client.textRenderer.getWidth(entry.text);
            int x = centerX - textWidth / 2;

            context.drawText(client.textRenderer, entry.text, x, y, color, true);
            index++;
        }
    }
}
