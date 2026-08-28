package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.config.ShakeConfig;
import ws.sodiunplugin.hitreplay.HitReplayConfig;
import ws.sodiunplugin.hitreplay.HitReplayLog;
import ws.sodiunplugin.hud.DamageDisplayStore;

/**
 * 在客户端收到 [EntityDamageS2CPacket] 时：
 *  - 若目标为本地玩家：调用 [HitReplayLog.beginEvent] 记录"受击来源"，实际伤害量由
 *    [ws.sodiunplugin.mixin.client.ClientPlayerDamageMixin] 通过血量/吸收血下降累加；
 *  - 否则（目标为其他实体）：原有"我造成的伤害"显示逻辑（[DamageDisplayStore.markPending]）。
 */
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

        // 提取来源信息（玩家受击与"我造成的伤害"都需要）
        String weaponName = null;
        String damageTypeName = "";
        String sourceTypeMsgId = "";
        try {
            DamageSource source = packet.createDamageSource(client.world);
            if (source != null) {
                var weaponStack = source.getWeaponStack();
                if (weaponStack != null && !weaponStack.isEmpty()) {
                    weaponName = weaponStack.getName().getString();
                }
                damageTypeName = source.getName();
            }
        } catch (Throwable ignored) {
            // 来源重建失败时使用类型名兜底
        }
        try {
            sourceTypeMsgId = packet.sourceType().value().msgId();
            if (damageTypeName.isEmpty()) {
                damageTypeName = sourceTypeMsgId;
            }
        } catch (Throwable ignored) {
            // 拿不到则留空
        }
        boolean isExplosion = sourceTypeMsgId.equals("explosion")
                || sourceTypeMsgId.equals("player_explosion")
                || sourceTypeMsgId.equals("badRespawnPoint");

        // 玩家自己受到的伤害 → 受击回放
        if (targetId == playerId) {
            if (HitReplayConfig.getRecordEnabled()) {
                String sourceName = resolvePlayerDamageSource(client.world, sourceDirectId, sourceCauseId, isExplosion, weaponName);
                System.out.println("[HitReplay] 收到本地玩家受伤包：source=" + sourceName + " type=" + sourceTypeMsgId);
                HitReplayLog.beginEvent(sourceName, System.currentTimeMillis());
            } else {
                System.out.println("[HitReplay] 收到本地玩家受伤包，但 recordEnabled=false 未记录");
            }
            return;
        }

        // 否则：处理"我造成的伤害"显示（原有逻辑）
        if (!ShakeConfig.getDamageDisplayEnabled()) {
            return;
        }

        ClientWorld world = client.world;
        Entity target = world.getEntityById(targetId);
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }

        boolean byMe = (sourceDirectId == playerId || sourceCauseId == playerId);
        boolean byExplosion = ShakeConfig.getExplosionCreditEnabled() && isExplosion;
        if (!byMe && !byExplosion) {
            return;
        }

        String finalWeaponName = byMe ? weaponName : "爆炸";
        DamageDisplayStore.markPending(targetId, finalWeaponName, damageTypeName, livingTarget.getHealth(), livingTarget.getAbsorptionAmount());
    }

    /**
     * 解析玩家受击的来源显示名：
     *  - 爆炸类伤害统一显示"爆炸"；
     *  - 否则优先用直接/间接来源实体的显示名（攻击者名字/生物名）；
     *  - 都没有则用武器名兜底，最终兜底为"环境伤害"。
     */
    private static String resolvePlayerDamageSource(ClientWorld world, int directId, int causeId, boolean isExplosion, String weaponName) {
        if (isExplosion) {
            return "爆炸";
        }
        Entity attacker = null;
        if (directId != 0) {
            attacker = world.getEntityById(directId);
        }
        if (attacker == null && causeId != 0) {
            attacker = world.getEntityById(causeId);
        }
        if (attacker != null) {
            String name = attacker.getName().getString();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        if (weaponName != null && !weaponName.isEmpty()) {
            return weaponName;
        }
        return "环境伤害";
    }
}
