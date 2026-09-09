package ws.sodiunplugin.hitreplay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import ws.sodiunplugin.hitreplay.HitReplayLog.HitRecord;

import java.util.List;

public class ReplayLogScreen extends Screen {
    private final Screen parent;
    private List<HitRecord> records = List.of();
    private int scroll = 0;
    private int lastLoggedCount = -1;

    private static final int LINE_HEIGHT = 58;
    private static final int TOP = 44;
    private static final int BOTTOM_MARGIN = 44;

    public ReplayLogScreen(Screen parent) {
        super(Text.literal("PvP 受击记录"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        records = HitReplayLog.getRecords();
        scroll = 0;
        lastLoggedCount = -1;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("返回"), b -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 28, 98, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("清空记录"), b -> {
            HitReplayLog.clear();
            records = HitReplayLog.getRecords();
        }).dimensions(this.width / 2 + 2, this.height - 28, 98, 20).build());
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentHeight = records.size() * LINE_HEIGHT;
        int viewHeight = this.height - TOP - BOTTOM_MARGIN;
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - verticalAmount * LINE_HEIGHT));
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.records = HitReplayLog.getRecords();

        super.render(context, mouseX, mouseY, delta);
        context.fill(0, 36, this.width, this.height - 36, 0xC0353540);
        context.fill(0, 0, this.width, 36, 0xD5454550);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFE0E0E0);

        if (records.size() != lastLoggedCount) {
            lastLoggedCount = records.size();
        }

        int viewHeight = this.height - TOP - BOTTOM_MARGIN;
        int visible = viewHeight / LINE_HEIGHT + 1;
        int start = scroll / LINE_HEIGHT;
        int drawn = 0;
        for (int i = start; i < records.size() && drawn < visible; i++, drawn++) {
            HitRecord r = records.get(i);
            int y = TOP + i * LINE_HEIGHT - scroll;
            if (y < TOP - LINE_HEIGHT || y > this.height - BOTTOM_MARGIN) {
                continue;
            }
            String time = HitReplayLog.formatTime(r.time);
            String critTag = r.critical ? "（暴击）" : "";
            String head = "[" + time + "] " + (r.death ? "☠ " : "") + "受到 " + String.format("%.1f", r.amount) + " 点伤害" + critTag;
            context.drawTextWithShadow(this.textRenderer, Text.literal(head), 24, y, r.death ? 0xFFFF8A8A : 0xFFFFFFFF);
            context.drawTextWithShadow(this.textRenderer, Text.literal("来源：" + r.source), 36, y + 12, 0xFFCFCFCF);
            String detail = "武器：" + (r.weaponName != null ? r.weaponName : "徒手")
                    + " · 类型：" + (r.damageType.isEmpty() ? "未知" : r.damageType)
                    + " · 距离：" + String.format("%.1f", r.distance) + " m";
            context.drawTextWithShadow(this.textRenderer, Text.literal(detail), 36, y + 24, 0xFFBFBFEF);
            if (r.deathMessage != null) {
                context.drawTextWithShadow(this.textRenderer, Text.literal("死因：" + r.deathMessage), 36, y + 36, 0xFFFFB0B0);
            }
        }

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("滚轮滚动 · 共 " + records.size() + " 条（仅记录被其他玩家攻击）"),
                this.width / 2, this.height - 52, 0xFF999999);

        if (records.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("暂无 PvP 受击记录（被其他玩家攻击后才会产生）"),
                    this.width / 2, this.height / 2, 0xFFAAAAAA);
        }
    }
}
