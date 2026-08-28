package ws.sodiunplugin

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import ws.sodiunplugin.config.ShakeConfig
import ws.sodiunplugin.hitreplay.HitReplayConfig
import ws.sodiunplugin.hitreplay.HitReplayLog
import ws.sodiunplugin.hud.DamageDisplayStore

object Ws_sodium_pluginClient : ClientModInitializer {
	override fun onInitializeClient() {
		// 读取已保存的配置与受击回放记录，供 Sodium 设置界面与 Mixin 使用。
		ShakeConfig.load()
		HitReplayConfig.load()
		HitReplayLog.load()
		println("[HitReplay] 启动：recordEnabled=${HitReplayConfig.recordEnabled}, 已加载记录=${HitReplayLog.getRecords().size}")

		// 伽马值不再直接写入原版 options（原版亮度有 0.0–1.0 硬限制且易被子覆盖），
		// 而是由 LightmapGammaMixin 把亮度计算重定向到 ShakeConfig.gammaOption，实时生效。
		ClientTickEvents.END_CLIENT_TICK.register {
			// 每客户端 tick 结算待处理的伤害显示（对比实体最新同步血量）。
			if (ShakeConfig.damageDisplayEnabled) {
				DamageDisplayStore.tickSettle()
			}
			// 结算受击回放中超过窗口仍未更新的受击事件。
			HitReplayLog.tickSettle()
		}
	}
}
