package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.combat.ComboTracker;
import ws.sodiunplugin.combat.FloatingDamageTexts;
import ws.sodiunplugin.config.ShakeConfig;

@Mixin(InGameHud.class)
public class InGameHudCombatMixin {

    @Inject(method = "renderStatusBars(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("RETURN"))
    private void onRenderStatusBarsCombat(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        renderFloatingDamage(client, context);
        renderCombo(client, context);
        renderProjectileCooldown(client, context);
    }

    private void renderFloatingDamage(MinecraftClient client, DrawContext context) {
        if (!ShakeConfig.getDamageFloatEnabled()) {
            return;
        }
        var entries = FloatingDamageTexts.collect();
        if (entries.isEmpty()) {
            return;
        }
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int anchorX = width / 2;
        int anchorY = (int) (height * 0.40);
        int i = 0;
        for (var e : entries) {
            float amount = e[0];
            float alpha = e[1];
            int alphaByte = (int) (alpha * 255f) & 0xFF;
            int color = (alphaByte << 24) | 0xFF4444;
            String text = String.format("%.1f", amount);
            int textW = client.textRenderer.getWidth(text);
            int x = anchorX - textW / 2 + (i % 2 == 0 ? -12 : 12);
            int y = (int) (anchorY - i * 4 - (1f - alpha) * FloatingDamageTexts.risePx());
            context.drawText(client.textRenderer, text, x, y, color, true);
            i++;
        }
    }

    private void renderCombo(MinecraftClient client, DrawContext context) {
        if (!ShakeConfig.getComboEnabled()) {
            return;
        }
        int combo = ComboTracker.INSTANCE.getCombo();
        if (combo < 2) {
            return;
        }
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int x = width - 12;
        int y = 14;
        String comboText = "连击 x" + combo;
        String peakText = "最高 x" + ComboTracker.INSTANCE.getMaxCombo();
        int cw = client.textRenderer.getWidth(comboText);
        int pw = client.textRenderer.getWidth(peakText);
        context.drawText(client.textRenderer, comboText, x - cw, y, 0xFFFF55, true);
        context.drawText(client.textRenderer, peakText, x - pw, y + 12, 0xAAAAAA, true);
    }

    private void renderProjectileCooldown(MinecraftClient client, DrawContext context) {
        if (!ShakeConfig.getProjectileCooldownEnabled()) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        var cm = player.getItemCooldownManager();
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int barW = 120;
        int barX = width - 12 - barW;
        int drawn = 0;
        for (ItemStack stack : new ItemStack[]{player.getMainHandStack(), player.getOffHandStack()}) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!cm.isCoolingDown(stack)) {
                continue;
            }
            float progress = cm.getCooldownProgress(stack, 0f);
            int pct = (int) (progress * 100f);
            int barY = height - 54 - drawn * 20;
            String label = stack.getName().getString() + " " + pct + "%";
            int labelW = client.textRenderer.getWidth(label);
            context.drawText(client.textRenderer, label, barX + barW - labelW, barY - 10, 0xFFFFFF, true);
            context.fill(barX, barY, barX + barW, barY + 6, 0x66000000);
            int fillW = (int) (barW * progress);
            context.fill(barX, barY, barX + fillW, barY + 6, 0xFF55CCFF);
            drawn++;
        }
    }
}
