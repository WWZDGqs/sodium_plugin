package ws.sodiunplugin.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

/**
 * Mod Menu 集成：在 Mod Menu 的模组列表中为本模组提供"配置"按钮。
 *
 * 本模组的所有设置都注册在 Sodium 视频设置界面内的独立一页（通过
 * {@code sodium:config_api_user} 入口点），因此这里的"配置"按钮直接打开
 * Sodium 的视频设置界面，进入后即可看到本模组专属设置列。
 *
 * 为避免编译期依赖 Sodium 的内部 GUI 类（它不在 sodium-fabric-api 中，只在
 * 运行时由 Sodium 主 jar 提供），这里用反射构造 {@code SodiumOptionsGUI}。
 * 若 Sodium 未安装或类名变更，则安全回退到原版视频设置界面，保证不崩溃。
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::createConfigScreen;
    }

    private static Screen createConfigScreen(Screen parent) {
        try {
            Class<?> guiClass = Class.forName("net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI");
            var constructor = guiClass.getConstructor(Screen.class);
            return (Screen) constructor.newInstance(parent);
        } catch (Throwable t) {
            // Sodium 不可用或内部类名变更：回退到原版视频设置界面
            try {
                Class<?> optionsScreen = Class.forName("net.minecraft.client.gui.screen.options.OptionsScreen");
                var ctor = optionsScreen.getConstructor(Screen.class, net.minecraft.client.option.GameOptions.class);
                return (Screen) ctor.newInstance(parent, net.minecraft.client.MinecraftClient.getInstance().options);
            } catch (Throwable ignored) {
                return parent;
            }
        }
    }
}
