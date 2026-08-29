package ws.sodiunplugin.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ws.sodiunplugin.config.ShakeConfig;

@Mixin(GameRenderer.class)
public class GameRendererEffectsMixin {


    @ModifyVariable(method = "renderWorld", at = @At("STORE"), index = 9)
    private float disableNauseaDistortion(float distortionScale) {
        return ShakeConfig.getNauseaEnabled() ? distortionScale : 0.0f;
    }


    @Redirect(method = "updateFovMultiplier",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getFovMultiplier(ZF)F"))
    private float redirectPotionFov(AbstractClientPlayerEntity player, boolean isFirstPerson, float fovEffectScale) {
        if (!ShakeConfig.getPotionFovEnabled()) {
            return 1.0f;
        }
        return player.getFovMultiplier(isFirstPerson, fovEffectScale);
    }


    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void scaleFovEffect(CallbackInfoReturnable<Float> cir) {
        float factor = ShakeConfig.fovFactor();
        if (factor == 1.0f) {
            return;
        }
        cir.setReturnValue(cir.getReturnValue() * factor);
    }
}
