package ws.sodiunplugin.mixin.client;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ws.sodiunplugin.config.ShakeConfig;

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
