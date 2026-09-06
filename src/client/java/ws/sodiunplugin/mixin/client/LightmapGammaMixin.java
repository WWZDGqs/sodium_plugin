package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ws.sodiunplugin.config.ShakeConfig;

@Mixin(LightmapTextureManager.class)
public class LightmapGammaMixin {

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"
            )
    )
    private Object redirectGammaGetValue(SimpleOption<?> instance) {
        if (instance == MinecraftClient.getInstance().options.getGamma()) {
            return ShakeConfig.getGammaDouble();
        }
        return instance.getValue();
    }
}
