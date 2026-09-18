package ws.sodiunplugin

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtOps
import net.minecraft.registry.RegistryOps
import ws.sodiunplugin.combat.FloatingDamageTexts
import ws.sodiunplugin.config.ShakeConfig
import ws.sodiunplugin.config.ShakeConfigEntryPoint
import ws.sodiunplugin.feature.AutoScreenshot
import ws.sodiunplugin.feature.AutoScreenshotConfig
import ws.sodiunplugin.feature.EnderChestConfig
import ws.sodiunplugin.feature.EnderChestSnapshot
import ws.sodiunplugin.feature.PlayerHighlightConfig
import ws.sodiunplugin.hitreplay.HitReplayConfig
import ws.sodiunplugin.hitreplay.HitReplayLog
import ws.sodiunplugin.hud.DamageDisplayStore

object Ws_sodium_pluginClient : ClientModInitializer {

	private var snapshotLoaded = false

	override fun onInitializeClient() {

		ShakeConfig.load()
		HitReplayConfig.load()
		PlayerHighlightConfig.load()
		AutoScreenshotConfig.load()
		EnderChestConfig.load()
		HitReplayLog.load()
		println("[HitReplay] 启动：recordEnabled=${HitReplayConfig.recordEnabled}, 已加载记录=${HitReplayLog.getRecords().size}")

		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { client, world ->
			if (snapshotLoaded) return@register
			val player = client.player ?: return@register
			snapshotLoaded = true
			val ops = RegistryOps.of(NbtOps.INSTANCE, player.getRegistryManager())
			EnderChestSnapshot.load(ops)
		}

		ClientTickEvents.END_CLIENT_TICK.register {
			if (ShakeConfig.damageDisplayEnabled) {
				DamageDisplayStore.tickSettle()
			}
			HitReplayLog.tickSettle()
			AutoScreenshot.tick()
			if (ShakeConfigEntryPoint.DANGER_ARMED) {
				throw RuntimeException("你为什么要那么做？")
			}
		}
	}
}
