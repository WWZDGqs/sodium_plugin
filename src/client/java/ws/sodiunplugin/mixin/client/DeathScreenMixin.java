package ws.sodiunplugin.mixin.client;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.hitreplay.HitReplayLog;

/**
 * 在死亡界面打开时，把死亡信息（死亡提示文本）记录到受击回放中。
 */
@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    @Inject(method = "<init>(Lnet/minecraft/text/Text;ZLnet/minecraft/client/network/ClientPlayerEntity;)V", at = @At("HEAD"))
    private static void onInit(Text title, boolean isHardcore, ClientPlayerEntity clientPlayer, CallbackInfo ci) {
        if (title != null) {
            HitReplayLog.recordDeath(title.getString());
        }
    }
}
