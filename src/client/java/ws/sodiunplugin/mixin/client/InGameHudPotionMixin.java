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
        if (!ShakeConfig.getPotionTimeEnabled()) {
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
