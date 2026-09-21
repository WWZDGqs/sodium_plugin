package ws.warzoneutils.mixin.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.warzoneutils.config.ShakeConfig;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V",
            at = @At("HEAD"), cancellable = true)
    private void filterParticles(Particle particle, CallbackInfo ci) {
        int percentage = ShakeConfig.getParticlePercentage();
        if (percentage >= 100) {
            return;
        }
        if (percentage <= 0 || Math.random() * 100.0 >= percentage) {
            ci.cancel();
        }
    }
}
