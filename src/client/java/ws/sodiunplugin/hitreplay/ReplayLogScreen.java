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

    private static final int LINE_HEIGHT = 36;
    private static final int TOP = 44;
    private static final int BOTTOM_MARGIN = 44;

    public ReplayLogScreen(Screen parent) {
        super(Text.literal("受击回放记录"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        records = HitReplayLog.getRecords();
        System.out.println("[HitReplay] 打开记录页：recordEnabled=" + HitReplayConfig.getRecordEnabled() + " 记录数=" + records.size());
        scroll = 0;
        lastLoggedCount = -1;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("返回"), b -> this.close())
                .dimensions(this.width / 2 - 100, this.height - 28, 98, 20)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("清空记录"), b -> {
            HitReplayLog.clear();
            System.out.println("[HitReplay] 已点击清空记录，当前记录数=0");
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
            StringBuilder sb = new StringBuilder();
            sb.append("[HitReplay] render 绘制记录数=").append(records.size());
            for (int i = 0; i < Math.min(3, records.size()); i++) {
                HitRecord r = records.get(i);
                sb.append(" | #").append(i).append(" ").append(HitReplayLog.formatTime(r.time))
                        .append(" dmg=").append(String.format("%.1f", r.amount))
                        .append(" src=").append(r.source);
            }
            System.out.println(sb);
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
            String head = "[" + time + "] " + (r.death ? "☠ " : "") + "受到 " + String.format("%.1f", r.amount) + " 点伤害";
            context.drawTextWithShadow(this.textRenderer, Text.literal(head), 24, y, r.death ? 0xFFFF8A8A : 0xFFFFFFFF);
            context.drawTextWithShadow(this.textRenderer, Text.literal("来源：" + r.source), 36, y + 12, 0xFFCFCFCF);
            if (r.deathMessage != null) {
                context.drawTextWithShadow(this.textRenderer, Text.literal("死因：" + r.deathMessage), 36, y + 24, 0xFFFFB0B0);
            }
        }

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("滚轮滚动 · 共 " + records.size() + " 条"),
                this.width / 2, this.height - 52, 0xFF999999);

        if (records.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("暂无受击记录（受伤后才会产生）"),
                    this.width / 2, this.height / 2, 0xFFAAAAAA);
        }
    }
}
