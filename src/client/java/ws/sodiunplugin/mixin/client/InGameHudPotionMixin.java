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

@Mixin(InGameHud.class)
public class InGameHudPotionMixin {

    private static final int BORDER_TICK_THRESHOLD = 200;

    private static final int CELL = 24;

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
        List<StatusEffectInstance> sorted = new ArrayList<>(effects);
        sorted.sort(Comparator.reverseOrder());

        int windowWidth = context.getScaledWindowWidth();
        int startY = 1;
        if (client.isDemo()) {
            startY += 15;
        }

        int beneficial = 0;
        int nonBeneficial = 0;

        long now = System.currentTimeMillis();
        float hue = (now % 2000L) / 2000.0f;                 
        float pulse = (float) (Math.sin(now / 110.0) * 0.5 + 0.5); 
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

            if (ShakeConfig.getPotionBorderEnabled() && duration <= BORDER_TICK_THRESHOLD) {
                drawBorder(context, x, y, borderColor);
            }

            if (ShakeConfig.getPotionTimeEnabled()) {
                int seconds = (duration + 19) / 20; 
                if (seconds <= 0) {
                    seconds = 1;
                }
                String label = seconds + "s";
                int textWidth = textRenderer.getWidth(label);
                int tx = x + CELL / 2 - textWidth / 2;
                int ty = y + 22; 
                context.fill(x + 2, y + 14, x + CELL - 2, y + 23, 0xB0000000);
                context.drawText(textRenderer, Text.literal(label), tx, ty, 0xFFFFFFFF, false);
            }
        }
    }

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
