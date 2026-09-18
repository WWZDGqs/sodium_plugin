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

import java.util.ArrayList;
import java.util.List;

@Mixin(InGameHud.class)
public class InGameHudCombatMixin {

    private static final double AIM_RANGE = 64.0;
    private static final double AIM_PADDING = 0.2;

    private static final int LINE_H = 12;
    private static final int PAD = 4;
    private static final int X_OFFSET = 14;
    private static final int Y_OFFSET = 6;

    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_DISTANCE = 0xFF99CCFF;
    private static final int COLOR_HEALTH = 0xFF66FF66;
    private static final int COLOR_BOX = 0x55000000;

    @Inject(method = "renderStatusBars(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("RETURN"))
    private void onRenderStatusBarsCombat(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }
        renderFloatingDamage(client, context);
        Entity target = findAimedPlayer(client, player);
        if (target != null) {
            renderTargetStatusHud(client, context, player, target);
        }
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

    private void renderTargetStatusHud(MinecraftClient client, DrawContext context,
                                       ClientPlayerEntity player, Entity target) {
        if (!ShakeConfig.getTargetStatusHudEnabled()) {
            return;
        }
        if (!(target instanceof PlayerEntity other)) {
            return;
        }

        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        lines.add(other.getName().getString());
        colors.add(COLOR_NAME);
        lines.add(String.format("距离 %.1f m", player.distanceTo(other)));
        colors.add(COLOR_DISTANCE);
        lines.add(String.format("HP %.1f/%.0f   护甲 %d",
                other.getHealth() + other.getAbsorptionAmount(), other.getMaxHealth(), other.getArmor()));
        colors.add(COLOR_HEALTH);

        int contentW = 0;
        for (String l : lines) {
            contentW = Math.max(contentW, client.textRenderer.getWidth(l));
        }

        int x = context.getScaledWindowWidth() / 2 + X_OFFSET;
        int y = context.getScaledWindowHeight() / 2 + Y_OFFSET;
        int boxW = contentW + PAD * 2;
        int boxH = lines.size() * LINE_H + PAD * 2;
        context.fill(x - PAD, y - PAD, x - PAD + boxW, y - PAD + boxH, COLOR_BOX);

        int ty = y;
        for (int i = 0; i < lines.size(); i++) {
            context.drawText(client.textRenderer, lines.get(i), x, ty, colors.get(i), true);
            ty += LINE_H;
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
        Vec3d from = player.getCameraPosVec(1.0F);
        Vec3d to = from.add(player.getRotationVec(1.0F).multiply(AIM_RANGE));
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlayerEntity other : world.getPlayers()) {
            if (other == player || other.isInvisible()) {
                continue;
            }
            var intersection = other.getBoundingBox().expand(AIM_PADDING).raycast(from, to);
            if (intersection.isEmpty()) {
                continue;
            }
            double dist = intersection.get().squaredDistanceTo(from);
            if (dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }
}
