package ws.sodiunplugin.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;


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
