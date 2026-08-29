package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.config.ShakeConfig;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    private static final float SPRINT_TO_WALK_RATIO = 0.77f;


    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void cancelDamageTilt(CallbackInfo ci) {
        if (!ShakeConfig.getDamageShakeEnabled()) {
            ci.cancel();
        }
    }


    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void maybeCancelViewBob(CallbackInfo ci) {
        if (ShakeConfig.strengthFactor() <= 0.0f) {
            ci.cancel();
        }
    }


    @ModifyVariable(method = "bobView", at = @At("STORE"), ordinal = 1)
    private float scaleBobMagnitude(float g) {
        float factor = ShakeConfig.strengthFactor();

        if (!ShakeConfig.getSprintShakeEnabled()) {
            AbstractClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && player.isSprinting()) {
                factor *= SPRINT_TO_WALK_RATIO;
            }
        }

        return g * factor;
    }
}
