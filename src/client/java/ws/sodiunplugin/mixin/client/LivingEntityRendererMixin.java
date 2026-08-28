package ws.sodiunplugin.mixin.client;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ws.sodiunplugin.config.ShakeConfig;

/**
 * 显示隐身玩家。
 *
 * vanilla 通过 LivingEntity.isInvisibleTo(PlayerEntity) 判断实体对当前视角玩家是否隐形，
 * 该结果决定渲染状态（身体透明度）与名字标签是否显示。开关开启时，对玩家实体
 * 强制返回 false，使隐身玩家像正常玩家一样完全可见。
 *
 * 注意：仅影响渲染判断，不改变游戏逻辑（碰撞、仇恨、目标选择等仍按原版处理）。
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Redirect(method = {
            "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z"
    }, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"))
    private boolean revealInvisiblePlayers(LivingEntity entity, PlayerEntity viewer) {
        if (ShakeConfig.getShowInvisiblePlayers() && entity instanceof PlayerEntity) {
            return false;
        }
        return entity.isInvisibleTo(viewer);
    }
}
