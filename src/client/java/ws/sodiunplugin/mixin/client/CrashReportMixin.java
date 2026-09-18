package ws.sodiunplugin.mixin.client;

import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.config.ShakeConfigEntryPoint;

@Mixin(CrashReport.class)
public class CrashReportMixin {

    private static final String DANGER_DESCRIPTION = "你为什么要那么做？";

    @Shadow
    @Final
    @Mutable
    private String message;

    @Inject(method = "<init>(Ljava/lang/String;Ljava/lang/Throwable;)V", at = @At("TAIL"))
    private void overrideDescription(String description, Throwable cause, CallbackInfo ci) {
        if (ShakeConfigEntryPoint.DANGER_ARMED) {
            this.message = DANGER_DESCRIPTION;
        }
    }
}
