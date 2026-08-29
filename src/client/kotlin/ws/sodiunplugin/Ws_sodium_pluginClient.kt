package ws.sodiunplugin

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import ws.sodiunplugin.config.ShakeConfig
import ws.sodiunplugin.feature.PlayerHighlightConfig
import ws.sodiunplugin.hitreplay.HitReplayConfig
import ws.sodiunplugin.hitreplay.HitReplayLog
import ws.sodiunplugin.hud.DamageDisplayStore

object Ws_sodium_pluginClient : ClientModInitializer {
	override fun onInitializeClient() {

		ShakeConfig.load()
		HitReplayConfig.load()
		PlayerHighlightConfig.load()
		HitReplayLog.load()
		println("[HitReplay] 启动：recordEnabled=${HitReplayConfig.recordEnabled}, 已加载记录=${HitReplayLog.getRecords().size}")

		ClientTickEvents.END_CLIENT_TICK.register {
			if (ShakeConfig.damageDisplayEnabled) {
				DamageDisplayStore.tickSettle()
			}
			HitReplayLog.tickSettle()
		}
	}
}
