package ws.sodiunplugin.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ws.sodiunplugin.config.ShakeConfig;

@Mixin(GameRenderer.class)
public class GameRendererEffectsMixin {

    @Redirect(method = "updateFovMultiplier",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getFovMultiplier()F"))
    private float redirectPotionFov(AbstractClientPlayerEntity player) {
        if (!ShakeConfig.getPotionFovEnabled()) {
            return 1.0f;
        }
        return player.getFovMultiplier();
    }


    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void scaleFovEffect(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        double factor = ShakeConfig.fovFactor();
        if (factor == 1.0) {
            return;
        }
        cir.setReturnValue(cir.getReturnValue() * factor);
    }
}
