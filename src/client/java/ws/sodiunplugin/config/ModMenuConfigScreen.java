package ws.sodiunplugin.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import ws.sodiunplugin.feature.AutoScreenshotConfig;
import ws.sodiunplugin.feature.HighlightColor;
import ws.sodiunplugin.feature.PlayerHighlightConfig;
import ws.sodiunplugin.hitreplay.HitReplayConfig;
import ws.sodiunplugin.hitreplay.ReplayLogScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

public class ModMenuConfigScreen extends Screen {

    private final Screen parent;
    private final List<ClickableWidget> options = new ArrayList<>();
    private int scroll = 0;
    private static final int TOP = 32;
    private static final int ROW = 24;
    private static final int BOTTOM = 56;

    public ModMenuConfigScreen(Screen parent) {
        super(Text.literal("Sodium View Shake Control 设置"));
        this.parent = parent;
    }

    private ClickableWidget boolRow(int x, int w, String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        return CyclingButtonWidget.onOffBuilder(Text.literal("开启"), Text.literal("关闭"))
                .initially(getter.getAsBoolean())
                .build(x, 0, w, 20, Text.translatable(key), (btn, val) -> setter.accept(val));
    }

    private ClickableWidget intRow(int x, int w, String key, int min, int max, IntSupplier getter, Consumer<Integer> setter, Function<Integer, Text> fmt) {
        return new IntSlider(x, 0, w, min, max, getter.getAsInt(), setter::accept, fmt);
    }

    private ClickableWidget enumRow(int x, int w, String key, Supplier<HighlightColor> getter, Consumer<HighlightColor> setter) {
        List<HighlightColor> values = new ArrayList<>(java.util.Arrays.asList(HighlightColor.values()));
        return CyclingButtonWidget.builder((HighlightColor c) -> Text.literal(c.getTranslationSuffix()))
                .values(values)
                .initially(getter.get())
                .build(x, 0, w, 20, Text.translatable(key), (btn, val) -> setter.accept(val));
    }

    static class IntSlider extends SliderWidget {
        private final int min;
        private final int max;
        private final IntConsumer onChange;
        private final Function<Integer, Text> fmt;
        private int current;

        IntSlider(int x, int y, int w, int min, int max, int initial, IntConsumer onChange, Function<Integer, Text> fmt) {
            super(x, y, w, 20, Text.literal(""), (double) (initial - min) / (max - min));
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            this.fmt = fmt;
            this.current = initial;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(fmt.apply(current));
        }

        @Override
        protected void applyValue() {
            current = min + (int) Math.round(this.value * (max - min));
            onChange.accept(current);
            updateMessage();
        }
    }

    @Override
    protected void init() {
        options.clear();
        int w = Math.min(400, this.width - 40);
        int x = (this.width - w) / 2;

        options.add(boolRow(x, w, "sodium_plugin.option.sprint_shake", ShakeConfig::getSprintShakeEnabled, ShakeConfig::setSprintShakeEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.damage_shake", ShakeConfig::getDamageShakeEnabled, ShakeConfig::setDamageShakeEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.nausea_shake", ShakeConfig::getNauseaEnabled, ShakeConfig::setNauseaEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.potion_fov", ShakeConfig::getPotionFovEnabled, ShakeConfig::setPotionFovEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.potion_time", ShakeConfig::getPotionTimeEnabled, ShakeConfig::setPotionTimeEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.damage_display", ShakeConfig::getDamageDisplayEnabled, ShakeConfig::setDamageDisplayEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.damage_float", ShakeConfig::getDamageFloatEnabled, ShakeConfig::setDamageFloatEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.explosion_credit", ShakeConfig::getExplosionCreditEnabled, ShakeConfig::setExplosionCreditEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.distance_display", ShakeConfig::getDistanceDisplayEnabled, ShakeConfig::setDistanceDisplayEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.target_status_hud", ShakeConfig::getTargetStatusHudEnabled, ShakeConfig::setTargetStatusHudEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.show_invisible_players", ShakeConfig::getShowInvisiblePlayers, ShakeConfig::setShowInvisiblePlayers));

        options.add(intRow(x, w, "sodium_plugin.option.shake_strength", 0, 100, ShakeConfig::getShakeStrength, ShakeConfig::setShakeStrength, v -> Text.literal(v + "%")));
        options.add(intRow(x, w, "sodium_plugin.option.particle_percentage", 0, 100, ShakeConfig::getParticlePercentage, ShakeConfig::setParticlePercentage, v -> Text.literal(v + "%")));
        options.add(intRow(x, w, "sodium_plugin.option.fov_effect", 50, 300, ShakeConfig::getFovEffect, ShakeConfig::setFovEffect, v -> Text.literal(v + "%")));
        options.add(intRow(x, w, "sodium_plugin.option.gamma_value", 1, 3000, ShakeConfig::getGammaValue, ShakeConfig::setGammaValue, v -> Text.literal(String.valueOf(v))));

        options.add(boolRow(x, w, "sodium_plugin.option.highlight_players", PlayerHighlightConfig::getEnabled, PlayerHighlightConfig::setEnabled));
        options.add(intRow(x, w, "sodium_plugin.option.highlight_range", 8, 128, PlayerHighlightConfig::getRange, PlayerHighlightConfig::setRange, v -> Text.literal(String.valueOf(v))));
        options.add(enumRow(x, w, "sodium_plugin.option.highlight_color", PlayerHighlightConfig::getColor, PlayerHighlightConfig::setColor));

        options.add(boolRow(x, w, "sodium_plugin.option.hit_replay_enabled", HitReplayConfig::getRecordEnabled, HitReplayConfig::setRecordEnabled));
        options.add(intRow(x, w, "sodium_plugin.option.hit_replay_max", 10, 500, HitReplayConfig::getMaxRecords, HitReplayConfig::setMaxRecords, v -> Text.literal(String.valueOf(v))));

        options.add(boolRow(x, w, "sodium_plugin.option.auto_screenshot_enabled", AutoScreenshotConfig::getEnabled, AutoScreenshotConfig::setEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.auto_screenshot_kill", AutoScreenshotConfig::getKillEnabled, AutoScreenshotConfig::setKillEnabled));
        options.add(boolRow(x, w, "sodium_plugin.option.auto_screenshot_death", AutoScreenshotConfig::getDeathEnabled, AutoScreenshotConfig::setDeathEnabled));

        for (ClickableWidget wgt : options) {
            this.addDrawableChild(wgt);
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("查看击杀回放记录"), b -> this.client.setScreen(new ReplayLogScreen(this)))
                .dimensions(8, this.height - 48, 180, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("完成"), b -> this.close())
                .dimensions(this.width / 2 - 60, this.height - 48, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("千万别点"), b -> {
                    throw new RuntimeException("你点击了『千万别点』按钮，游戏已崩溃。");
                })
                .dimensions(this.width - 188, this.height - 48, 180, 20).build());

        layout();
    }

    private void layout() {
        int maxScroll = Math.max(0, options.size() * ROW - (this.height - TOP - BOTTOM));
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
        int i = 0;
        for (ClickableWidget wgt : options) {
            wgt.setY(TOP + i * ROW - scroll);
            i++;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll -= (int) amount * ROW;
        int maxScroll = Math.max(0, options.size() * ROW - (this.height - TOP - BOTTOM));
        if (scroll < 0) {
            scroll = 0;
        }
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
        layout();
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        layout();
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
    }

    private void saveAll() {
        ShakeConfig.save();
        HitReplayConfig.save();
        PlayerHighlightConfig.save();
        AutoScreenshotConfig.save();
    }

    @Override
    public void close() {
        saveAll();
        super.close();
    }
}
