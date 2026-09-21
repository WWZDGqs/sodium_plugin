package ws.warzoneutils

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.minecraft.nbt.NbtOps
import net.minecraft.registry.RegistryOps
import ws.warzoneutils.config.ShakeConfig
import ws.warzoneutils.config.ShakeConfigEntryPoint
import ws.warzoneutils.feature.AutoScreenshot
import ws.warzoneutils.feature.AutoScreenshotConfig
import ws.warzoneutils.feature.EnderChestConfig
import ws.warzoneutils.feature.EnderChestScreenHook
import ws.warzoneutils.feature.EnderChestSnapshot
import ws.warzoneutils.hitreplay.HitReplayConfig
import ws.warzoneutils.hitreplay.HitReplayLog
import ws.warzoneutils.hud.DamageDisplayStore

object WarzoneUtilsClient : ClientModInitializer {

	private var snapshotLoaded = false

	override fun onInitializeClient() {

		ShakeConfig.load()
		HitReplayConfig.load()
		AutoScreenshotConfig.load()
		EnderChestConfig.load()
		HitReplayLog.load()

		EnderChestScreenHook.register()

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
