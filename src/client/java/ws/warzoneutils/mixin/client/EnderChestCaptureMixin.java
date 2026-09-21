package ws.warzoneutils.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.warzoneutils.feature.EnderChestConfig;
import ws.warzoneutils.feature.EnderChestTracker;

@Mixin(ClientPlayNetworkHandler.class)
public class EnderChestCaptureMixin {

    private static final String ENDER_CHEST_KEY = "container.enderchest";

    private static int pendingSyncId = -1;

    @Inject(method = "onOpenScreen", at = @At("HEAD"))
    private void onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
        pendingSyncId = -1;
        EnderChestTracker.reset();
        if (!EnderChestConfig.getEnabled()) {
            return;
        }
        if (packet.getScreenHandlerType() != ScreenHandlerType.GENERIC_9X3) {
            return;
        }
        if (!isEnderChestTitle(packet.getName())) {
            return;
        }
        pendingSyncId = packet.getSyncId();
    }

    @Inject(method = "onInventory", at = @At("TAIL"))
    private void onInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        if (pendingSyncId < 0 || packet.syncId() != pendingSyncId) {
            return;
        }
        pendingSyncId = -1;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        ScreenHandler handler = player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler)) {
            return;
        }
        EnderChestTracker.beginTracking(handler);
    }

    @Inject(method = "onCloseScreen", at = @At("HEAD"))
    private void onCloseScreen(net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket packet, CallbackInfo ci) {
        pendingSyncId = -1;
    }

    private static boolean isEnderChestTitle(Text name) {
        if (name == null) {
            return false;
        }
        if (name.getContent() instanceof TranslatableTextContent translatable) {
            return ENDER_CHEST_KEY.equals(translatable.getKey());
        }
        return ENDER_CHEST_KEY.equals(name.getString());
    }
}
