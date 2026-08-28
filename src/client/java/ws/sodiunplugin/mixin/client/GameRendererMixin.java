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

/**
 * 拦截原版的视角晃动（bobView）与受击倾斜（tiltViewWhenHurt）。
 *
 * 实现说明（基于 1.21.11 yarn 映射反编译）：
 * - bobView 内部所有位移/旋转幅度都线性正比于局部变量 g（lerpMovement，即速度差帧增量），
 *   因此只需在 STORE 点缩放该变量即可整体缩放视角晃动幅度，实现 0%–100% 强度。
 * - bobView 内并无 isSprinting 分支，疾跑时晃动增强来自速度差更大（g 变大）。
 *   疾跑开关关闭时，对正在疾跑的玩家额外压低系数，使疾跑视角晃动接近走路水平。
 * - tiltViewWhenHurt 在受击时倾斜屏幕，关闭时直接取消该方法即可彻底去除受击抖动。
 *
 * 注意：注入方法不声明 MatrixStack 参数（其 yarn 名为 net.minecraft.client.util.math.MatrixStack，
 * intermediary class_4587），避免 yarn 方法签名在运行时与 remap 后的目标不匹配导致崩溃。
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // 走路与疾跑速度比（4.317 / 5.612 ≈ 0.77），用于疾跑开关关闭时把疾跑晃动压低到接近走路水平。
    private static final float SPRINT_TO_WALK_RATIO = 0.77f;

    // ---------- 受击抖动开关 ----------

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void cancelDamageTilt(CallbackInfo ci) {
        if (!ShakeConfig.getDamageShakeEnabled()) {
            ci.cancel();
        }
    }

    // ---------- 视角抖动强度（0% = 完全关闭） ----------

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void maybeCancelViewBob(CallbackInfo ci) {
        if (ShakeConfig.strengthFactor() <= 0.0f) {
            ci.cancel();
        }
    }

    // ---------- 视角抖动强度缩放 + 疾跑抖动开关 ----------

    /**
     * 在 bobView 的第二个 float 局部变量（g，即 lerpMovement）被 STORE 时拦截，
     * 按强度系数缩放整体晃动幅度；若为疾跑且疾跑开关关闭，额外压低到接近走路水平。
     */
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
