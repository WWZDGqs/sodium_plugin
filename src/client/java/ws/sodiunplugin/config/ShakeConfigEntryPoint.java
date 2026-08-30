package ws.sodiunplugin.config;

import java.util.Set;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import ws.sodiunplugin.feature.ChestEspConfig;
import ws.sodiunplugin.feature.HighlightColor;
import ws.sodiunplugin.feature.PlayerHighlightConfig;
import ws.sodiunplugin.hitreplay.HitReplayConfig;
import ws.sodiunplugin.hitreplay.ReplayLogScreen;


public class ShakeConfigEntryPoint implements ConfigEntryPoint {

    private static final String MOD_ID = "sodium_plugin";

    private static final StorageEventHandler STORAGE = () -> {
        ShakeConfig.save();
        HitReplayConfig.save();
        PlayerHighlightConfig.save();
        ChestEspConfig.save();
    };

    private static final ControlValueFormatter PERCENT_FORMATTER = value -> Text.literal(value + "%");

    private static final ControlValueFormatter PLAIN_FORMATTER = value -> Text.literal(String.valueOf(value));

    @Override
    public void registerConfigEarly(ConfigBuilder builder) {
    }

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        var modOptions = builder.registerOwnModOptions()
                .setName("Sodium View Shake Control")
                .setIcon(Identifier.of(MOD_ID, "textures/gui/config-icon.png"));

        modOptions.addPage(
                builder.createOptionPage()
                        .setName(Text.translatable("sodium_plugin.page.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("sodium_plugin.group.shake"))
                                        .addOption(
                                                builder.createIntegerOption(Identifier.of(MOD_ID, "shake_strength"))
                                                        .setName(Text.translatable("sodium_plugin.option.shake_strength"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.shake_strength.tooltip"))
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
                                                        .setName(Text.translatable("sodium_plugin.option.sprint_shake"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.sprint_shake.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setSprintShakeEnabled(value),
                                                                ShakeConfig::getSprintShakeEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "damage_shake"))
                                                        .setName(Text.translatable("sodium_plugin.option.damage_shake"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.damage_shake.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setDamageShakeEnabled(value),
                                                                ShakeConfig::getDamageShakeEnabled)
                                        )
                        )
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("sodium_plugin.group.camera_fx"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "nausea_shake"))
                                                        .setName(Text.translatable("sodium_plugin.option.nausea_shake"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.nausea_shake.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setNauseaEnabled(value),
                                                                ShakeConfig::getNauseaEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "potion_fov"))
                                                        .setName(Text.translatable("sodium_plugin.option.potion_fov"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.potion_fov.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setPotionFovEnabled(value),
                                                                ShakeConfig::getPotionFovEnabled)
                                        )
                                        .addOption(
                                                builder.createIntegerOption(Identifier.of(MOD_ID, "particle_percentage"))
                                                        .setName(Text.translatable("sodium_plugin.option.particle_percentage"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.particle_percentage.tooltip"))
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
                                                        .setName(Text.translatable("sodium_plugin.option.fov_effect"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.fov_effect.tooltip"))
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
                                                        .setName(Text.translatable("sodium_plugin.option.gamma_value"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.gamma_value.tooltip"))
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
                                                        .setName(Text.translatable("sodium_plugin.option.show_invisible_players"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.show_invisible_players.tooltip"))
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
                        .setName(Text.translatable("sodium_plugin.page.highlight.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("sodium_plugin.group.highlight"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "highlight_players"))
                                                        .setName(Text.translatable("sodium_plugin.option.highlight_players"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.highlight_players.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(false)
                                                        .setBinding(
                                                                value -> PlayerHighlightConfig.setEnabled(value),
                                                                PlayerHighlightConfig::getEnabled)
                                        )
                                        .addOption(
                                                builder.createIntegerOption(Identifier.of(MOD_ID, "highlight_range"))
                                                        .setName(Text.translatable("sodium_plugin.option.highlight_range"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.highlight_range.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setRange(8, 128, 1)
                                                        .setDefaultValue(64)
                                                        .setValueFormatter(PLAIN_FORMATTER)
                                                        .setBinding(
                                                                value -> PlayerHighlightConfig.setRange(value),
                                                                PlayerHighlightConfig::getRange)
                                        )
                                        .addOption(
                                                builder.createEnumOption(Identifier.of(MOD_ID, "highlight_color"), HighlightColor.class)
                                                        .setName(Text.translatable("sodium_plugin.option.highlight_color"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.highlight_color.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setAllowedValues(Set.of(HighlightColor.values()))
                                                        .setElementNameProvider(
                                                                color -> Text.translatable(
                                                                        "sodium_plugin.option.highlight_color." + color.getTranslationSuffix()))
                                                        .setDefaultValue(HighlightColor.WHITE)
                                                        .setBinding(
                                                                value -> PlayerHighlightConfig.setColor(value),
                                                                PlayerHighlightConfig::getColor)
                                        )
                        )
        );

        modOptions.addPage(
                builder.createOptionPage()
                        .setName(Text.translatable("sodium_plugin.page.hud.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("sodium_plugin.group.hud"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "potion_time"))
                                                        .setName(Text.translatable("sodium_plugin.option.potion_time"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.potion_time.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setPotionTimeEnabled(value),
                                                                ShakeConfig::getPotionTimeEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "potion_border"))
                                                        .setName(Text.translatable("sodium_plugin.option.potion_border"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.potion_border.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setPotionBorderEnabled(value),
                                                                ShakeConfig::getPotionBorderEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "damage_display"))
                                                        .setName(Text.translatable("sodium_plugin.option.damage_display"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.damage_display.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setDamageDisplayEnabled(value),
                                                                ShakeConfig::getDamageDisplayEnabled)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "explosion_credit"))
                                                        .setName(Text.translatable("sodium_plugin.option.explosion_credit"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.explosion_credit.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ShakeConfig.setExplosionCreditEnabled(value),
                                                                ShakeConfig::getExplosionCreditEnabled)
                                        )
                        )
        );

        modOptions.addPage(
                builder.createOptionPage()
                        .setName(Text.translatable("sodium_plugin.page.chestesp.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("sodium_plugin.group.chestesp"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "chest_esp_enabled"))
                                                        .setName(Text.translatable("sodium_plugin.option.chest_esp_enabled"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.chest_esp_enabled.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(false)
                                                        .setBinding(
                                                                value -> ChestEspConfig.setEnabled(value),
                                                                ChestEspConfig::getEnabled)
                                        )
                                        .addOption(
                                                builder.createIntegerOption(Identifier.of(MOD_ID, "chest_esp_range"))
                                                        .setName(Text.translatable("sodium_plugin.option.chest_esp_range"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.chest_esp_range.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setRange(8, 128, 1)
                                                        .setDefaultValue(32)
                                                        .setValueFormatter(PLAIN_FORMATTER)
                                                        .setBinding(
                                                                value -> ChestEspConfig.setRange(value),
                                                                ChestEspConfig::getRange)
                                        )
                        )
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("sodium_plugin.group.chestesp.types"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "chest_esp_chest"))
                                                        .setName(Text.translatable("sodium_plugin.option.chest_esp_chest"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.chest_esp_chest.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ChestEspConfig.setShowChest(value),
                                                                ChestEspConfig::getShowChest)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "chest_esp_shulker"))
                                                        .setName(Text.translatable("sodium_plugin.option.chest_esp_shulker"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.chest_esp_shulker.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ChestEspConfig.setShowShulkerBox(value),
                                                                ChestEspConfig::getShowShulkerBox)
                                        )
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "chest_esp_ender"))
                                                        .setName(Text.translatable("sodium_plugin.option.chest_esp_ender"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.chest_esp_ender.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> ChestEspConfig.setShowEnderChest(value),
                                                                ChestEspConfig::getShowEnderChest)
                                        )
                        )
        );

        modOptions.addPage(
                builder.createOptionPage()
                        .setName(Text.translatable("sodium_plugin.page.hitreplay.title"))
                        .addOptionGroup(
                                builder.createOptionGroup()
                                        .setName(Text.translatable("sodium_plugin.group.hitreplay"))
                                        .addOption(
                                                builder.createBooleanOption(Identifier.of(MOD_ID, "hit_replay_enabled"))
                                                        .setName(Text.translatable("sodium_plugin.option.hit_replay_enabled"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.hit_replay_enabled.tooltip"))
                                                        .setStorageHandler(STORAGE)
                                                        .setDefaultValue(true)
                                                        .setBinding(
                                                                value -> HitReplayConfig.setRecordEnabled(value),
                                                                HitReplayConfig::getRecordEnabled)
                                        )
                                        .addOption(
                                                builder.createIntegerOption(Identifier.of(MOD_ID, "hit_replay_max"))
                                                        .setName(Text.translatable("sodium_plugin.option.hit_replay_max"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.hit_replay_max.tooltip"))
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
                                                        .setName(Text.translatable("sodium_plugin.option.hit_replay_view"))
                                                        .setTooltip(Text.translatable("sodium_plugin.option.hit_replay_view.tooltip"))
                                                        .setScreenConsumer(currentScreen -> MinecraftClient.getInstance().setScreen(new ReplayLogScreen(currentScreen)))
                                        )
                        )
        );
    }
}
//怎么全是bug啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊
