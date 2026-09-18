package ws.sodiunplugin.mixin.client;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.registry.RegistryOps;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ws.sodiunplugin.feature.EnderChestConfig;
import ws.sodiunplugin.feature.EnderChestSnapshot;

@Mixin(ClientPlayNetworkHandler.class)
public class EnderChestCaptureMixin {

    private static final String ENDER_CHEST_KEY = "container.enderchest";

    private static int pendingSyncId = -1;

    @Inject(method = "onOpenScreen", at = @At("HEAD"))
    private void onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
        pendingSyncId = -1;
        if (!EnderChestConfig.getEnabled()) {
            return;
        }
        ScreenHandlerType<?> type = packet.getScreenHandlerType();
        if (type != ScreenHandlerType.GENERIC_9X3) {
            return;
        }
        Text name = packet.getName();
        if (name == null) {
            return;
        }
        if (!isEnderChestTitle(name.getString())) {
            return;
        }
        pendingSyncId = packet.getSyncId();
    }

    @Inject(method = "onInventory", at = @At("TAIL"))
    private void onInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        if (pendingSyncId < 0 || packet.syncId() != pendingSyncId) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        ScreenHandler handler = player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler)) {
            return;
        }
        List<ItemStack> contents = packet.contents();
        if (contents.size() < EnderChestSnapshot.SIZE) {
            return;
        }
        RegistryOps<NbtElement> ops = RegistryOps.of(
                net.minecraft.nbt.NbtOps.INSTANCE, player.getRegistryManager());
        EnderChestSnapshot.capture(contents, ops);
        pendingSyncId = -1;
    }

    @Inject(method = "onCloseScreen", at = @At("HEAD"))
    private void onCloseScreen(net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket packet, CallbackInfo ci) {
        pendingSyncId = -1;
    }

    private static boolean isEnderChestTitle(String title) {
        if (ENDER_CHEST_KEY.equals(title)) {
            return true;
        }
        try {
            Identifier id = Identifier.tryParse(title);
            if (id != null && "minecraft".equals(id.getNamespace())) {
                return ENDER_CHEST_KEY.equals(id.getPath());
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }
}
