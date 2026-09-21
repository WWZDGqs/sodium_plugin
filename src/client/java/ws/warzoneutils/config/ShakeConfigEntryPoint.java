package ws.warzoneutils.config;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import ws.warzoneutils.feature.AutoScreenshotConfig;
import ws.warzoneutils.feature.EnderChestConfig;
import ws.warzoneutils.feature.EnderChestSnapshotScreen;
import ws.warzoneutils.hitreplay.HitReplayConfig;
import ws.warzoneutils.hitreplay.ReplayLogScreen;


public class ShakeConfigEntryPoint implements ConfigEntryPoint {

    private static final String MOD_ID = "warzoneutils";

    private static final StorageEventHandler STORAGE = () -> {
        ShakeConfig.save();
        HitReplayConfig.save();
        AutoScreenshotConfig.save();
        EnderChestConfig.save();
    };

    private static final ControlValueFormatter PERCENT_FORMATTER = value -> Text.literal(value + "%");

    private static final ControlValueFormatter PLAIN_FORMATTER = value -> Text.literal(String.valueOf(value));

    public static volatile boolean DANGER_ARMED = false;

    @Override
    public void registerConfigEarly(ConfigBuilder builder) {
    }

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        var modOptions = builder.registerOwnModOptions()
                .setName("Warzone Utils")
                .setIcon(Identifier.of(MOD_ID, "textures/gui/config-icon.png"));

        modOptions.addPage(
                builder.createOptionPage()
                        .setName(Text.translatable("warzoneutils.page.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("warzoneutils.group.shake"))
                                        .addOption(
                                                builder.createIntegerOption(Identifier.of(MOD_ID, "shake_strength"))
                                                        .setName(Text.translatable("warzoneutils.option.shake_strength"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.shake_strength.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setRange(0, 100, 1)
                                                        .setDefaultValue(100)
                                                        .setValueFormatter(PERCENT_FORMATTER)
                                                        .setBinding(
                                                                value -> ShakeConfig.setShakeStrength(value),
                                                                ShakeConfig::getShakeStrength)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "sprint_shake"))
                                                        .setName(Text.translatable("warzoneutils.option.sprint_shake"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.sprint_shake.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setSprintShakeEnabled(value),
                                                                ShakeConfig::getSprintShakeEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "damage_shake"))
                                                        .setName(Text.translatable("warzoneutils.option.damage_shake"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.damage_shake.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setDamageShakeEnabled(value),
                                                                ShakeConfig::getDamageShakeEnabled)
                                        )
                        )
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("warzoneutils.group.camera_fx"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "nausea_shake"))
                                                        .setName(Text.translatable("warzoneutils.option.nausea_shake"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.nausea_shake.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setNauseaEnabled(value),
                                                                ShakeConfig::getNauseaEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "potion_fov"))
                                                        .setName(Text.translatable("warzoneutils.option.potion_fov"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.potion_fov.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setPotionFovEnabled(value),
                                                                ShakeConfig::getPotionFovEnabled)
                                        )
                                        .addOption(
                                                builder.createIntegerOption(Identifier.of(MOD_ID, "particle_percentage"))
                                                        .setName(Text.translatable("warzoneutils.option.particle_percentage"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.particle_percentage.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setRange(0, 100, 1)
                                                        .setDefaultValue(100)
                                                        .setValueFormatter(PERCENT_FORMATTER)
                                                        .setBinding(
                                                                value -> ShakeConfig.setParticlePercentage(value),
                                                                ShakeConfig::getParticlePercentage)
                                        )
                                        .addOption(
                                                builder.createIntegerOption(Identifier.of(MOD_ID, "fov_effect"))
                                                        .setName(Text.translatable("warzoneutils.option.fov_effect"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.fov_effect.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setRange(50, 300, 1)
                                                        .setDefaultValue(100)
                                                        .setValueFormatter(PERCENT_FORMATTER)
                                                        .setBinding(
                                                                value -> ShakeConfig.setFovEffect(value),
                                                                ShakeConfig::getFovEffect)
                                        )
                                        .addOption(
                                                builder.createIntegerOption(Identifier.of(MOD_ID, "gamma_value"))
                                                        .setName(Text.translatable("warzoneutils.option.gamma_value"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.gamma_value.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setRange(1, 3000, 1)
                                                        .setDefaultValue(100)
                                                        .setValueFormatter(PLAIN_FORMATTER)
                                                        .setBinding(
                                                                value -> ShakeConfig.setGammaValue(value),
                                                                ShakeConfig::getGammaValue)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "show_invisible_players"))
                                                        .setName(Text.translatable("warzoneutils.option.show_invisible_players"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.show_invisible_players.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(false)
                                                        .setBinding(
                                                                value -> ShakeConfig.setShowInvisiblePlayers(value),
                                                                ShakeConfig::getShowInvisiblePlayers)
                                        )
                        )
        );

        modOptions.addPage(
                builder.createOptionPage()
                        .setName(Text.translatable("warzoneutils.page.hud.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("warzoneutils.group.hud"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "potion_time"))
                                                        .setName(Text.translatable("warzoneutils.option.potion_time"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.potion_time.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setPotionTimeEnabled(value),
                                                                ShakeConfig::getPotionTimeEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "damage_display"))
                                                        .setName(Text.translatable("warzoneutils.option.damage_display"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.damage_display.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setDamageDisplayEnabled(value),
                                                                ShakeConfig::getDamageDisplayEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "explosion_credit"))
                                                        .setName(Text.translatable("warzoneutils.option.explosion_credit"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.explosion_credit.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setExplosionCreditEnabled(value),
                                                                ShakeConfig::getExplosionCreditEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "damage_float"))
                                                        .setName(Text.translatable("warzoneutils.option.damage_float"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.damage_float.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setDamageFloatEnabled(value),
                                                                ShakeConfig::getDamageFloatEnabled)
                                        )
                        )
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("warzoneutils.group.combat"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "target_status_hud"))
                                                        .setName(Text.translatable("warzoneutils.option.target_status_hud"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.target_status_hud.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setTargetStatusHudEnabled(value),
                                                                ShakeConfig::getTargetStatusHudEnabled)
                                        )
                        )
        );

        modOptions.addPage(
                builder.createOptionPage()
                        .setName(Text.translatable("warzoneutils.page.hitreplay.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("warzoneutils.group.hitreplay"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "hit_replay_enabled"))
                                                        .setName(Text.translatable("warzoneutils.option.hit_replay_enabled"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.hit_replay_enabled.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> HitReplayConfig.setRecordEnabled(value),
                                                                HitReplayConfig::getRecordEnabled)
                                        )
                                        .addOption(
                                                builder.createIntegerOption(Identifier.of(MOD_ID, "hit_replay_max"))
                                                        .setName(Text.translatable("warzoneutils.option.hit_replay_max"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.hit_replay_max.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setRange(10, 500, 10)
                                                        .setDefaultValue(50)
                                                        .setValueFormatter(PLAIN_FORMATTER)
                                                        .setBinding(
                                                                value -> HitReplayConfig.setMaxRecords(value),
                                                                HitReplayConfig::getMaxRecords)
                                        )
                                        .addOption(
                                                builder.createExternalButtonOption(Identifier.of(MOD_ID, "hit_replay_view"))
                                                        .setName(Text.translatable("warzoneutils.option.hit_replay_view"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.hit_replay_view.tooltip"))
                                                        .setScreenConsumer(currentScreen -> MinecraftClient.getInstance().setScreen(new ReplayLogScreen(currentScreen)))
                                        )
                        )
        );

        modOptions.addPage(
                builder.createOptionPage()
                        .setName(Text.translatable("warzoneutils.page.autoscreenshot.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("warzoneutils.group.autoscreenshot"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "auto_screenshot_enabled"))
                                                        .setName(Text.translatable("warzoneutils.option.auto_screenshot_enabled"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.auto_screenshot_enabled.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(false)
                                                        .setBinding(
                                                                value -> AutoScreenshotConfig.setEnabled(value),
                                                                AutoScreenshotConfig::getEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "auto_screenshot_kill"))
                                                        .setName(Text.translatable("warzoneutils.option.auto_screenshot_kill"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.auto_screenshot_kill.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> AutoScreenshotConfig.setKillEnabled(value),
                                                                AutoScreenshotConfig::getKillEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "auto_screenshot_death"))
                                                        .setName(Text.translatable("warzoneutils.option.auto_screenshot_death"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.auto_screenshot_death.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> AutoScreenshotConfig.setDeathEnabled(value),
                                                                AutoScreenshotConfig::getDeathEnabled)
                                        )
                        )
        );

        modOptions.addPage(
                builder.createOptionPage()
                        .setName(Text.translatable("warzoneutils.page.enderchest.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("warzoneutils.group.enderchest"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "ender_chest_enabled"))
                                                        .setName(Text.translatable("warzoneutils.option.ender_chest_enabled"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.ender_chest_enabled.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> EnderChestConfig.setEnabled(value),
                                                                EnderChestConfig::getEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "ender_chest_button"))
                                                        .setName(Text.translatable("warzoneutils.option.ender_chest_button"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.ender_chest_button.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> EnderChestConfig.setShowSnapshotButton(value),
                                                                EnderChestConfig::getShowSnapshotButton)
                                        )
                                        .addOption(
                                                builder.createExternalButtonOption(Identifier.of(MOD_ID, "ender_chest_view"))
                                                        .setName(Text.translatable("warzoneutils.option.ender_chest_view"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.ender_chest_view.tooltip"))
                                                        .setScreenConsumer(currentScreen -> {
                                                            ClientPlayerEntity player = MinecraftClient.getInstance().player;
                                                            if (player == null) {
                                                                return;
                                                            }
                                                            MinecraftClient.getInstance().setScreen(
                                                                    new EnderChestSnapshotScreen(currentScreen, player));
                                                        })
                                        )
                        )
        );

        modOptions.addPage(
                builder.createOptionPage()
                        .setName(Text.translatable("warzoneutils.page.danger.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("warzoneutils.group.danger"))
                                        .addOption(
                                                builder.createExternalButtonOption(Identifier.of(MOD_ID, "danger_button"))
                                                        .setName(Text.translatable("warzoneutils.option.danger_button"))
                                                        .setTooltip(Text.translatable("warzoneutils.option.danger_button.tooltip"))
                                                        .setScreenConsumer(currentScreen -> {
                                                            DANGER_ARMED = true;
                                                            throw new RuntimeException("你为什么要那么做？");
                                                        })
                                        )
                        )
        );
    }
}
