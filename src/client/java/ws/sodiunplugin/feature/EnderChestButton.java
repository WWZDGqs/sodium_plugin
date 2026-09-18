package ws.sodiunplugin.feature;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.util.Identifier;

public class EnderChestButton extends TexturedButtonWidget {

    private static final Identifier ICON = Identifier.of("sodium_plugin", "textures/gui/ender_chest_button.png");
    private static final ButtonTextures TEXTURES = new ButtonTextures(
            Identifier.ofVanilla("widget/button"),
            Identifier.ofVanilla("widget/button_disabled"),
            Identifier.ofVanilla("widget/button_highlighted"));

    public EnderChestButton(int x, int y, Runnable onPress) {
        super(x, y, 20, 20, TEXTURES, button -> onPress.run(),
                net.minecraft.text.Text.translatable("sodium_plugin.button.ender_chest"));
    }

    @Override
    public void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED, ICON,
                this.getX() + 2, this.getY() + 2,
                0.0F, 0.0F,
                16, 16,
                16, 16);
    }
}
