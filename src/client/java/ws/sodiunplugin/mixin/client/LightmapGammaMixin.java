package ws.sodiunplugin.mixin.client;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ws.sodiunplugin.config.ShakeConfig;

/**
 * 把光照图（亮度）计算使用的伽马值重定向到本模组的配置项。
 *
 * 原版亮度选项（GameOptions.gamma）使用 DoubleSliderCallbacks，强制把值夹取在 0.0–1.0，
 * 导致模组伽马值超过 200（=1.0）时高亮部分完全无效；且原版 options 是亮度的“真源”，
 * 容易被原版视频设置或 UI 覆盖。这里仅拦截 LightmapTextureManager.update 内读取伽马值的
 * 那一次 SimpleOption.getValue() 调用，直接返回本模组的伽马值（gammaValue / 200.0），
 * 支持 1–3000（gamma 0.005–15）的真正超亮，并实时响应 Sodium 设置界面里的修改。
 * 原版视频设置界面的亮度滑块不受任何影响。
 */
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
        return ShakeConfig.getGammaDouble();
    }
}
