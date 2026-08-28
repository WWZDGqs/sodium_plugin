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

/**
 * 镜头相关的附加效果控制（基于 1.21.11 yarn 映射反编译验证）：
 *
 * 1. 反胃（河豚）扭曲开关：renderWorld 中把 "distortion effect scale"（局部变量槽 9）
 *    置为 0 即可跳过整个扭曲渲染分支（原版已把扭曲量与该系数相乘）。
 * 2. 药水视场角缩放开关（速度/神龟等）：updateFovMultiplier 通过
 *    AbstractClientPlayerEntity.getFovMultiplier 计算药水导致的 FOV 倍率，
 *    关闭时返回 1.0（无缩放）即可。
 * 3. 视场角效果 50%–300%：getFov 的返回值整体乘系数（100% 为原版）。
 */
@Mixin(GameRenderer.class)
public class GameRendererEffectsMixin {

    // ---------- 反胃（河豚）视角扭曲开关 ----------

    @ModifyVariable(method = "renderWorld", at = @At("STORE"), index = 9)
    private float disableNauseaDistortion(float distortionScale) {
        return ShakeConfig.getNauseaEnabled() ? distortionScale : 0.0f;
    }

    // ---------- 药水视场角缩放开关（速度/神龟等） ----------

    @Redirect(method = "updateFovMultiplier",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getFovMultiplier(ZF)F"))
    private float redirectPotionFov(AbstractClientPlayerEntity player, boolean isFirstPerson, float fovEffectScale) {
        if (!ShakeConfig.getPotionFovEnabled()) {
            // 1.0 = 无药水导致的 FOV 缩放
            return 1.0f;
        }
        return player.getFovMultiplier(isFirstPerson, fovEffectScale);
    }

    // ---------- 视场角效果 50%–300% ----------

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void scaleFovEffect(CallbackInfoReturnable<Float> cir) {
        float factor = ShakeConfig.fovFactor();
        if (factor == 1.0f) {
            return;
        }
        cir.setReturnValue(cir.getReturnValue() * factor);
    }
}
