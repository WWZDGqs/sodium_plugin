package ws.sodiunplugin.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.feature.EnderChestButton;
import ws.sodiunplugin.feature.EnderChestConfig;
import ws.sodiunplugin.feature.EnderChestSnapshot;
import ws.sodiunplugin.feature.EnderChestSnapshotScreen;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Shadow
    protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement);

    @Inject(method = "init", at = @At("TAIL"))
    private void addEnderChestButton(CallbackInfo ci) {
        if (!EnderChestConfig.getEnabled() || !EnderChestConfig.getShowSnapshotButton()) {
            return;
        }
        InventoryScreen self = (InventoryScreen) (Object) this;
        Screen screen = (Screen) (Object) this;
        int x = screen.width / 2 + 104;
        int y = screen.height / 2 + 40;
        EnderChestButton button = new EnderChestButton(x, y, () -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) {
                return;
            }
            MinecraftClient.getInstance().setScreen(new EnderChestSnapshotScreen(self, player));
        });
        button.active = EnderChestSnapshot.hasSnapshot();
        this.addDrawableChild(button);
    }
}
