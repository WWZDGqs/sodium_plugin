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
        int baseY = height - 39;
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
            int color = (alphaByte << 24) | 0xFFFFFF;

            int y = startY - index * lineHeight;
            int textWidth = client.textRenderer.getWidth(entry.text);
            int x = centerX - textWidth / 2;

            context.drawText(client.textRenderer, entry.text, x, y, color, true);
            index++;
        }
    }
}
