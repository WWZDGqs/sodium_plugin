package ws.sodiunplugin.feature

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.util.ScreenshotRecorder
import net.minecraft.registry.Registries
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import java.io.File

object AutoScreenshot {
    private var lastKillCount = -1
    private var wasDead = false
    private var tickCounter = 0

    fun tick() {
        if (!AutoScreenshotConfig.enabled) return
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        if (AutoScreenshotConfig.killEnabled) {
            if (tickCounter % 4 == 0) {
                val kills = totalKills(player)
                if (lastKillCount < 0) {
                    lastKillCount = kills
                } else if (kills > lastKillCount) {
                    lastKillCount = kills
                    takeScreenshot("kill")
                }
            }
        }

        if (AutoScreenshotConfig.deathEnabled) {
            val dead = player.isDead
            if (dead && !wasDead) {
                wasDead = true
                takeScreenshot("death")
            } else if (!dead) {
                wasDead = false
            }
        }

        tickCounter++
    }

    private fun totalKills(player: ClientPlayerEntity): Int {
        var total = 0
        for (entityType in Registries.ENTITY_TYPE) {
            total += player.statHandler.getStat(Stats.KILLED.getOrCreateStat(entityType))
        }
        return total
    }

    private fun takeScreenshot(prefix: String) {
        val client = MinecraftClient.getInstance()
        val dir = File(client.runDirectory, "screenshots")
        if (!dir.exists()) dir.mkdirs()
        client.execute {
            try {
                val receiver = java.util.function.Consumer<Text> { _ -> }
                ScreenshotRecorder.saveScreenshot(
                    dir,
                    "${prefix}_${System.currentTimeMillis()}.png",
                    client.framebuffer,
                    1,
                    receiver
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
