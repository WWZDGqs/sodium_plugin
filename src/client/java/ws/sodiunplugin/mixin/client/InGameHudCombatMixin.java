package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.combat.FloatingDamageTexts;
import ws.sodiunplugin.config.ShakeConfig;

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
        renderDistanceDisplay(client, context);
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

    private void renderDistanceDisplay(MinecraftClient client, DrawContext context) {
        if (!ShakeConfig.getDistanceDisplayEnabled()) {
            return;
        }
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        Entity target = findAimedPlayer(client, player);
        if (target == null) {
            return;
        }
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        float dist = player.distanceTo(target);
        String text = String.format("%.1f", dist);
        int x = width / 2 + 14;
        int y = height / 2 - 4;
        context.drawText(client.textRenderer, text, x, y, 0xFFFFFFFF, true);
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
        Vec3d from = player.getEntityPos();
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
            Vec3d aimPoint = other.getEntityPos().add(0.0, other.getHeight() * 0.5, 0.0);
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
