package ws.warzoneutils.feature;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;

public final class EnderChestTracker {

    private static volatile ScreenHandler tracked = null;

    private EnderChestTracker() {
    }

    public static void reset() {
        tracked = null;
    }

    public static void beginTracking(ScreenHandler handler) {
        tracked = handler;
    }

    public static void captureOnClose(PlayerEntity player) {
        ScreenHandler handler = tracked;
        tracked = null;
        if (handler == null || player == null) {
            return;
        }
        if (!(handler instanceof GenericContainerScreenHandler container)) {
            return;
        }
        Inventory inventory = container.getInventory();
        List<ItemStack> contents = new ArrayList<>(EnderChestSnapshot.SIZE);
        for (int i = 0; i < EnderChestSnapshot.SIZE; i++) {
            contents.add(inventory.getStack(i).copy());
        }
        RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, player.getRegistryManager());
        EnderChestSnapshot.capture(contents, ops);
    }
}
