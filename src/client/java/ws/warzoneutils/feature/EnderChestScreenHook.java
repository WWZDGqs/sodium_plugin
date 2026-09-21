package ws.warzoneutils.feature;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class EnderChestScreenHook {

    private static final Identifier BUTTON = Identifier.ofVanilla("widget/button");
    private static final Identifier BUTTON_HIGHLIGHTED = Identifier.ofVanilla("widget/button_highlighted");
    private static final ItemStack ICON = new ItemStack(Items.ENDER_CHEST);
    private static final int SIZE = 20;
    private static final int OFFSET_X = -10;
    private static final int OFFSET_Y = -107;

    private static final Set<Screen> HOOKED = Collections.newSetFromMap(new WeakHashMap<>());

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen)) {
                return;
            }
            if (!HOOKED.add(screen)) {
                return;
            }
            ScreenEvents.afterRender(screen).register(
                    (s, context, mouseX, mouseY, tickDelta) -> renderButton(s, context, mouseX, mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register(EnderChestScreenHook::onClick);
            ScreenEvents.remove(screen).register(s -> HOOKED.remove(s));
        });
    }

    private static void renderButton(Screen screen, DrawContext context, int mouseX, int mouseY) {
        if (!isEnabled()) {
            return;
        }
        int x = buttonX(screen);
        int y = buttonY(screen);
        boolean hovered = isInside(mouseX, mouseY, x, y);
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                hovered ? BUTTON_HIGHLIGHTED : BUTTON,
                x, y, SIZE, SIZE);
        context.drawItem(ICON, x + 2, y + 2);
    }

    private static boolean onClick(Screen screen, Click click) {
        if (!isEnabled() || click.button() != 0) {
            return true;
        }
        int x = buttonX(screen);
        int y = buttonY(screen);
        if (!isInside(click.x(), click.y(), x, y)) {
            return true;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return true;
        }
        MinecraftClient.getInstance().setScreen(new EnderChestSnapshotScreen(screen, player));
        return false;
    }

    private static boolean isEnabled() {
        return EnderChestConfig.getEnabled() && EnderChestConfig.getShowSnapshotButton();
    }

    private static int buttonX(Screen screen) {
        return screen.width / 2 + OFFSET_X;
    }

    private static int buttonY(Screen screen) {
        return Math.max(2, screen.height / 2 + OFFSET_Y);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
    }
}
