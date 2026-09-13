package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.combat.FloatingDamageTexts;
import ws.sodiunplugin.config.ShakeConfig;

import java.util.ArrayList;
import java.util.List;

@Mixin(InGameHud.class)
public class InGameHudCombatMixin {

    private static final double AIM_TOLERANCE = Math.toRadians(8.0);

    @Inject(method = "renderStatusBars(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("RETURN"))
    private void onRenderStatusBarsCombat(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        renderFloatingDamage(client, context);
        renderTargetStatusHud(client, context);
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

    private void renderTargetStatusHud(MinecraftClient client, DrawContext context) {
        if (!ShakeConfig.getTargetStatusHudEnabled()) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        Entity target = findAimedPlayer(client, player);
        if (!(target instanceof PlayerEntity other)) {
            return;
        }
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int x = width / 2 + 14;
        int y = height / 2 + 6;

        float hp = other.getHealth() + other.getAbsorptionAmount();
        float maxHp = other.getMaxHealth();
        int armor = other.getArmor();
        String name = other.getName().getString();

        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        lines.add(name);
        colors.add(0xFFFFFFFF);
        if (ShakeConfig.getDistanceDisplayEnabled()) {
            float dist = player.distanceTo(other);
            lines.add(String.format("距离 %.1f m", dist));
            colors.add(0xFF99CCFF);
        }
        lines.add(String.format("HP %.1f/%.0f   护甲 %d", hp, maxHp, armor));
        colors.add(0xFF66FF66);
        for (StatusEffectInstance inst : other.getStatusEffects()) {
            StatusEffect effect = inst.getEffectType();
            String effName = effect.getName().getString();
            int amplifier = inst.getAmplifier() + 1;
            int secs = inst.getDuration() / 20;
            String time = secs >= 60 ? String.format("%d:%02d", secs / 60, secs % 60) : (secs + "s");
            lines.add(String.format("%s %d  %s", effName, amplifier, time));
            colors.add(0xFFFFFF55);
        }

        int lineH = 12;
        int pad = 4;
        int boxW = 0;
        for (String l : lines) {
            boxW = Math.max(boxW, client.textRenderer.getWidth(l));
        }
        boxW += pad * 2;
        int boxH = lines.size() * lineH + pad * 2;
        context.fill(x - pad, y - pad, x - pad + boxW, y - pad + boxH, 0x55000000);
        int ty = y;
        for (int i = 0; i < lines.size(); i++) {
            context.drawText(client.textRenderer, lines.get(i), x, ty, colors.get(i), true);
            ty += lineH;
        }
    }

    private Entity findAimedPlayer(MinecraftClient client, ClientPlayerEntity player) {
        var hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit) {
            Entity e = entityHit.getEntity();
            if (e instanceof PlayerEntity && e != player) {
                return e;
            }
        }
        ClientWorld world = client.world;
        if (world == null) {
            return null;
        }
        Vec3d from = player.getPos();
        Vec3d look = player.getRotationVec(1.0F);
        Entity best = null;
        double bestDot = Math.cos(AIM_TOLERANCE);
        for (PlayerEntity other : world.getPlayers()) {
            if (other == player) {
                continue;
            }
            if (other.isInvisible()) {
                continue;
            }
            Vec3d aimPoint = other.getPos().add(0.0, other.getHeight() * 0.5, 0.0);
            Vec3d dir = aimPoint.subtract(from);
            double len = dir.length();
            if (len < 1.0E-6) {
                continue;
            }
            double dot = look.dotProduct(dir.multiply(1.0 / len));
            if (dot > bestDot) {
                bestDot = dot;
                best = other;
            }
        }
        return best;
    }
}
