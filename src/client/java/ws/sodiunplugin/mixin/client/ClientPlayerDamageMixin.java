package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.hitreplay.HitReplayLog;

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
