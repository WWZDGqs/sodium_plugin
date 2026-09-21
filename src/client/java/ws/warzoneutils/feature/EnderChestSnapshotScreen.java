package ws.warzoneutils.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class EnderChestSnapshotScreen extends HandledScreen<GenericContainerScreenHandler> {

    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");
    private static final int SLOT_SIZE = 18;
    private static final int ROWS = 3;
    private static final int CONTAINER_SLOTS = ROWS * 9;
    private static final int LOCAL_SYNC_ID = -1;

    private final Screen parent;
    private final Inventory mirror;

    public EnderChestSnapshotScreen(Screen parent, PlayerEntity player) {
        super(buildHandler(player), player.getInventory(),
                Text.translatable("warzoneutils.screen.ender_chest.title"));
        this.parent = parent;
        this.mirror = this.handler.getInventory();
        this.backgroundHeight = 114 + ROWS * SLOT_SIZE;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    private static GenericContainerScreenHandler buildHandler(PlayerEntity player) {
        SimpleInventory inventory = new SimpleInventory(EnderChestSnapshot.SIZE);
        return GenericContainerScreenHandler.createGeneric9x3(LOCAL_SYNC_ID, player.getInventory(), inventory);
    }

    @Override
    protected void init() {
        super.init();
        fillMirror();
        makeContainerSlotsReadOnly();
    }

    private void fillMirror() {
        for (int i = 0; i < EnderChestSnapshot.SIZE; i++) {
            this.mirror.setStack(i, EnderChestSnapshot.getStack(i).copy());
        }
    }

    private void makeContainerSlotsReadOnly() {
        for (int i = 0; i < CONTAINER_SLOTS; i++) {
            Slot original = this.handler.slots.get(i);
            this.handler.slots.set(i, new ReadOnlySlot(original.inventory, original.getIndex(), original.x, original.y));
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED, TEXTURE,
                this.x, this.y,
                0.0F, 0.0F,
                this.backgroundWidth, ROWS * SLOT_SIZE + 17,
                256, 256);
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED, TEXTURE,
                this.x, this.y + ROWS * SLOT_SIZE + 17,
                0.0F, 126.0F,
                this.backgroundWidth, 96,
                256, 256);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (!EnderChestSnapshot.hasSnapshot()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("warzoneutils.screen.ender_chest.empty"),
                    this.width / 2,
                    this.y + this.backgroundHeight + 8,
                    0xFFFFAA00);
        }
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    public static class ReadOnlySlot extends Slot {

        public ReadOnlySlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return false;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }
}
