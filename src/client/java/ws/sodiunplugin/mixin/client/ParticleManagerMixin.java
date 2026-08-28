package ws.sodiunplugin.mixin.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ws.sodiunplugin.config.ShakeConfig;

/**
 * 粒子数量百分比控制。
 *
 * 拦截 ParticleManager.addParticle(ParticleEffect, ...) 入口（所有粒子生成都经由它，
 * 内部 createParticle 成功后才加入队列），按配置的概率丢弃粒子：
 * 返回 null 与 vanilla "createParticle 失败" 的语义一致，调用方已安全处理。
 */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void filterParticles(CallbackInfoReturnable<Particle> cir) {
        int percentage = ShakeConfig.getParticlePercentage();
        if (percentage >= 100) {
            return;
        }
        if (percentage <= 0 || Math.random() * 100.0 >= percentage) {
            cir.setReturnValue(null);
        }
    }
}
