package ws.warzoneutils.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.warzoneutils.config.ShakeConfig;
import ws.warzoneutils.hitreplay.HitReplayConfig;
import ws.warzoneutils.hitreplay.HitReplayLog;
import ws.warzoneutils.hud.DamageDisplayStore;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerDamageMixin {

    @Inject(
            method = "onEntityDamage(Lnet/minecraft/network/packet/s2c/play/EntityDamageS2CPacket;)V",
            at = @At("RETURN")
    )
    private void onEntityDamage(EntityDamageS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        int targetId = packet.entityId();
        int sourceDirectId = packet.sourceDirectId();
        int sourceCauseId = packet.sourceCauseId();
        int playerId = client.player.getId();

        if (targetId != playerId) {
            handleTargetDamage(client, packet, targetId, sourceDirectId, sourceCauseId, playerId);
            return;
        }

        if (!HitReplayConfig.getRecordEnabled()) {
            return;
        }

        ClientWorld world = client.world;
        Entity attacker = resolveEntity(world, sourceDirectId, sourceCauseId);
        if (!(attacker instanceof PlayerEntity) || attacker.getId() == playerId) {
            return;
        }

        String weaponName = null;
        String damageType = "";
        String sourceTypeMsgId = "";
        try {
            DamageSource source = packet.createDamageSource(world);
            if (source != null) {
                var weaponStack = source.getWeaponStack();
                if (weaponStack != null && !weaponStack.isEmpty()) {
                    weaponName = weaponStack.getName().getString();
                }
                damageType = source.getName();
            }
        } catch (Throwable ignored) {
        }
        try {
            sourceTypeMsgId = packet.sourceType().value().msgId();
            if (!sourceTypeMsgId.isEmpty()) {
                damageType = sourceTypeMsgId;
            }
        } catch (Throwable ignored) {
        }

        float distance = 0f;
        try {
            distance = (float) attacker.getEntityPos().distanceTo(client.player.getEntityPos());
        } catch (Throwable ignored) {
        }

        boolean critical = false;
        for (String name : new String[]{"isCritical", "getCritical", "critical"}) {
            try {
                var m = packet.getClass().getMethod(name);
                critical = (boolean) m.invoke(packet);
                break;
            } catch (Throwable ignored) {
            }
        }

        String attackerName = attacker.getName().getString();
        HitReplayLog.beginEvent(attackerName, System.currentTimeMillis(), weaponName, damageType, distance, critical);
    }

    private void handleTargetDamage(MinecraftClient client, EntityDamageS2CPacket packet, int targetId, int sourceDirectId, int sourceCauseId, int playerId) {
        if (!ShakeConfig.getDamageDisplayEnabled() && !ShakeConfig.getDamageFloatEnabled()) {
            return;
        }
        ClientWorld world = client.world;
        Entity target = world.getEntityById(targetId);
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        String weaponName = null;
        String damageTypeName = "";
        String sourceTypeMsgId = "";
        try {
            DamageSource source = packet.createDamageSource(world);
            if (source != null) {
                var weaponStack = source.getWeaponStack();
                if (weaponStack != null && !weaponStack.isEmpty()) {
                    weaponName = weaponStack.getName().getString();
                }
                damageTypeName = source.getName();
            }
        } catch (Throwable ignored) {
        }
        try {
            sourceTypeMsgId = packet.sourceType().value().msgId();
            if (damageTypeName.isEmpty()) {
                damageTypeName = sourceTypeMsgId;
            }
        } catch (Throwable ignored) {
        }
        boolean isExplosion = sourceTypeMsgId.equals("explosion")
                || sourceTypeMsgId.equals("player_explosion")
                || sourceTypeMsgId.equals("badRespawnPoint");

        boolean byMe = (sourceDirectId == playerId || sourceCauseId == playerId);
        boolean byExplosion = ShakeConfig.getExplosionCreditEnabled() && isExplosion;
        if (!byMe && !byExplosion) {
            return;
        }

        String finalWeaponName = byMe ? weaponName : "爆炸";
        boolean isAttack = byMe;
        DamageDisplayStore.markPending(targetId, finalWeaponName, damageTypeName, livingTarget.getHealth(), livingTarget.getAbsorptionAmount(), isAttack);
    }

    private static Entity resolveEntity(ClientWorld world, int directId, int causeId) {
        if (directId != 0) {
            Entity e = world.getEntityById(directId);
            if (e != null) return e;
        }
        if (causeId != 0) {
            Entity e = world.getEntityById(causeId);
            if (e != null) return e;
        }
        return null;
    }
}
