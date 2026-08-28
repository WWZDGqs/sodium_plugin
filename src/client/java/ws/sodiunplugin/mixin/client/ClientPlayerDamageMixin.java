package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.hitreplay.HitReplayLog;

/**
 * 监听本地玩家实体被服务端同步血量([LivingEntity.setHealth])/吸收血([LivingEntity.setAbsorptionAmount])下降的时刻，
 * 向 [HitReplayLog] 累加实际伤害量。
 *
 * 做法：在方法 HEAD 读取同步前旧值，RETURN 读取新值，二者之差即为本次同步实际扣减量。
 * 与 [net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket] 的到达顺序无关，
 * 因此金血条（吸收血）场景下也能稳定记录受到的伤害。仅对本地玩家生效。
 */
@Mixin(LivingEntity.class)
public class ClientPlayerDamageMixin {

    private transient float preHealth = Float.NaN;
    private transient float preAbsorption = Float.NaN;

    private static boolean isLocalPlayer(LivingEntity self) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.player == self;
    }

    @Inject(method = "setHealth(F)V", at = @At("HEAD"))
    private void onSetHealthHead(float health, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!isLocalPlayer(self)) {
            return;
        }
        preHealth = self.getHealth();
    }

    @Inject(method = "setHealth(F)V", at = @At("RETURN"))
    private void onSetHealthReturn(float health, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!isLocalPlayer(self)) {
            return;
        }
        if (!Float.isNaN(preHealth)) {
            float now = self.getHealth();
            if (now < preHealth - 1e-4f) {
                float loss = preHealth - now;
                System.out.println("[HitReplay] 本地玩家 setHealth 下降: " + preHealth + " -> " + now + " 损失=" + loss);
                HitReplayLog.addHealthLoss(loss);
            }
        }
        preHealth = Float.NaN;
    }

    @Inject(method = "setAbsorptionAmount(F)V", at = @At("HEAD"))
    private void onSetAbsorptionHead(float absorptionAmount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!isLocalPlayer(self)) {
            return;
        }
        preAbsorption = self.getAbsorptionAmount();
    }

    @Inject(method = "setAbsorptionAmount(F)V", at = @At("RETURN"))
    private void onSetAbsorptionReturn(float absorptionAmount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!isLocalPlayer(self)) {
            return;
        }
        if (!Float.isNaN(preAbsorption)) {
            float now = self.getAbsorptionAmount();
            if (now < preAbsorption - 1e-4f) {
                HitReplayLog.addAbsorptionLoss(preAbsorption - now);
            }
        }
        preAbsorption = Float.NaN;
    }
}
