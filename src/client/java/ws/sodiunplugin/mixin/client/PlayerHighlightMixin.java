package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ws.sodiunplugin.feature.PlayerHighlightConfig;

@Mixin(Entity.class)
public class PlayerHighlightMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void highlightNearbyPlayers(CallbackInfoReturnable<Boolean> cir) {
        if (shouldHighlight()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void applyHighlightColor(CallbackInfoReturnable<Integer> cir) {
        if (shouldHighlight()) {
            cir.setReturnValue(PlayerHighlightConfig.getColor().getColorValue());
        }
    }

    private boolean shouldHighlight() {
        if (!PlayerHighlightConfig.getEnabled()) {
            return false;
        }

        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity) || !self.isAlive()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity viewer = client.player;
        if (viewer == null || self == viewer) {
            return false;
        }

        double range = PlayerHighlightConfig.getRange();
        double dx = self.getX() - viewer.getX();
        double dy = self.getY() - viewer.getY();
        double dz = self.getZ() - viewer.getZ();
        return dx * dx + dy * dy + dz * dz <= range * range;
    }
}
