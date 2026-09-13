package ws.sodiunplugin.mixin.client;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.hitreplay.HitReplayLog;

@Mixin(DeathScreen.class)
public class DeathScreenMixin {

    @Inject(method = "<init>(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"))
    private void onInit(Text title, boolean isHardcore, CallbackInfo ci) {
        if (title != null) {
            HitReplayLog.recordDeath(title.getString());
        }
    }
}
