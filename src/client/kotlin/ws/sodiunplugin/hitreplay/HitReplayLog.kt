package ws.sodiunplugin.hitreplay

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

/**
 * 受击回放的数据源：记录玩家受到的伤害（伤害量、时间、来源、死亡信息）。
 *
 * 捕获链路：
 *  - [ws.sodiunplugin.mixin.client.ClientPlayNetworkHandlerDamageMixin] 在收到以本地玩家为目标实体的
 *    [net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket] 时调用 [beginEvent]，记录受击来源；
 *  - [ws.sodiunplugin.mixin.client.ClientPlayerDamageMixin] 在本地玩家实体的
 *    [net.minecraft.entity.LivingEntity.setHealth] / [net.minecraft.entity.LivingEntity.setAbsorptionAmount]
 *    被服务端同步下降时累加实际伤害量；
 *  - [ws.sodiunplugin.mixin.client.DeathScreenMixin] 在死亡界面打开时调用 [recordDeath] 记录死亡信息。
 *
 * 实际伤害量来自"同步前旧值 − 同步后新值"的精确差分，与包到达顺序无关，
 * 因此金血条（吸收血）场景下也能稳定记录受到的伤害。
 */
object HitReplayLog {

    /** 临时诊断开关：定位"无受击记录"问题时置 true，确认后改回 false。 */
    private const val DEBUG = true
    private fun dbg(msg: String) {
        if (DEBUG) println("[HitReplay] $msg")
    }

    /** 单条受击记录。文本在记录时即组装好，避免渲染线程重复访问实体。 */
    data class HitRecord(
        @JvmField val time: Long,
        @JvmField val amount: Float,
        @JvmField val source: String,
        @JvmField val death: Boolean,
        @JvmField val deathMessage: String?,
    )

    /** 结算窗口（毫秒）：一次受击的多次同步包在此窗口内累加，窗口结束后生成记录。 */
    private const val SETTLE_WINDOW_MS = 250L

    // ---- 当前受击事件的临时累加器（仅客户端主线程访问） ----
    @Volatile
    private var eventActive = false
    private var eventHealthLoss = 0f
    private var eventAbsorptionLoss = 0f
    private var eventSource = "环境伤害"
    private var eventStartTime = 0L
    private var eventLastUpdate = 0L
    private var eventDeathMessage: String? = null

    private val records: MutableList<HitRecord> = mutableListOf()
    private val lock = Any()

    // ---- 受击事件生命周期 ----

    /** 开始一次受击事件（记录来源）。若已有未结算事件则先结算它。 */
    @JvmStatic
    fun beginEvent(source: String, time: Long) {
        if (!HitReplayConfig.recordEnabled) {
            dbg("beginEvent 被跳过：recordEnabled=false")
            return
        }
        synchronized(lock) {
            if (eventActive) finalizeEventLocked()
            beginEventLocked(source, time)
            dbg("beginEvent: source=$source, eventActive=$eventActive")
        }
    }

    /** 累加主血减少量（来自 setHealth 下降）。 */
    @JvmStatic
    fun addHealthLoss(amount: Float) {
        if (amount <= 0f || !HitReplayConfig.recordEnabled) {
            dbg("addHealthLoss 被跳过：amount=$amount, recordEnabled=${HitReplayConfig.recordEnabled}")
            return
        }
        synchronized(lock) {
            if (!eventActive) beginEventLocked("环境伤害", System.currentTimeMillis())
            eventHealthLoss += amount
            eventLastUpdate = System.currentTimeMillis()
            dbg("addHealthLoss: +$amount, 累计主血=$eventHealthLoss, 吸收=$eventAbsorptionLoss, eventActive=$eventActive")
        }
    }

    /** 累加吸收血（金血条）减少量（来自 setAbsorptionAmount 下降）。
     *  仅在已有进行中的受击事件时累加——吸收血下降既可能来自受伤，也可能来自喝牛奶清除，
     *  后者没有伤害包、不应记为受击；而真实的受伤一定先有伤害包激活事件。 */
    @JvmStatic
    fun addAbsorptionLoss(amount: Float) {
        if (amount <= 0f || !HitReplayConfig.recordEnabled) return
        synchronized(lock) {
            if (!eventActive) {
                dbg("addAbsorptionLoss 被跳过：eventActive=false（无进行中的受击事件）")
                return
            }
            eventAbsorptionLoss += amount
            eventLastUpdate = System.currentTimeMillis()
            dbg("addAbsorptionLoss: +$amount, 累计吸收=$eventAbsorptionLoss, 主血=$eventHealthLoss")
        }
    }

    /** 记录死亡信息：若有进行中的受击事件则一并结算为死亡记录；否则单独记一条死亡记录。 */
    @JvmStatic
    fun recordDeath(message: String?) {
        if (!HitReplayConfig.recordEnabled) {
            dbg("recordDeath 被跳过：recordEnabled=false")
            return
        }
        synchronized(lock) {
            if (eventActive) {
                eventDeathMessage = message
                finalizeEventLocked()
            } else {
                addRecordLocked(HitRecord(System.currentTimeMillis(), 0f, "死亡", true, message))
                dbg("recordDeath（独立死亡记录）：msg=$message")
            }
        }
    }

    private fun beginEventLocked(source: String, time: Long) {
        eventActive = true
        eventHealthLoss = 0f
        eventAbsorptionLoss = 0f
        eventSource = source
        eventStartTime = time
        eventLastUpdate = time
        eventDeathMessage = null
    }

    private fun finalizeEventLocked() {
        if (!eventActive) return
        val total = eventHealthLoss + eventAbsorptionLoss
        dbg("finalizeEvent: total=$total, source=$eventSource, death=${eventDeathMessage != null}")
        if (total > 0f || eventDeathMessage != null) {
            addRecordLocked(
                HitRecord(
                    eventStartTime,
                    total,
                    eventSource,
                    eventDeathMessage != null,
                    eventDeathMessage,
                )
            )
            dbg("record 已添加，当前记录数=${records.size}")
        }
        eventActive = false
        eventHealthLoss = 0f
        eventAbsorptionLoss = 0f
        eventDeathMessage = null
    }

    private fun addRecordLocked(record: HitRecord) {
        records.add(0, record)
        while (records.size > HitReplayConfig.maxRecords) {
            records.removeAt(records.size - 1)
        }
        save()
    }

    /** 每客户端 tick 调用：结算超过窗口仍未更新的受击事件。 */
    @JvmStatic
    fun tickSettle() {
        if (!HitReplayConfig.recordEnabled) return
        synchronized(lock) {
            if (eventActive && System.currentTimeMillis() - eventLastUpdate > SETTLE_WINDOW_MS) {
                dbg("tickSettle 触发结算（事件静默超过 ${SETTLE_WINDOW_MS}ms）")
                finalizeEventLocked()
            }
        }
    }

    @JvmStatic
    fun getRecords(): List<HitRecord> = synchronized(lock) { records.toList() }

    @JvmStatic
    fun clear() {
        synchronized(lock) {
            records.clear()
            save()
        }
    }

    @JvmStatic
    fun formatTime(time: Long): String {
        return SimpleDateFormat("MM-dd HH:mm:ss").format(Date(time))
    }

    // ---- 持久化 ----
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val logPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("sodium_plugin/hit_replay_log.json")

    @JvmStatic
    fun load() {
        val path = logPath
        if (!Files.exists(path)) return
        try {
            val arr = gson.fromJson(Files.readString(path), JsonArray::class.java) ?: return
            synchronized(lock) {
                records.clear()
                for (el in arr) {
                    if (!el.isJsonObject) continue
                    val o = el.asJsonObject
                    val rec = HitRecord(
                        if (o.has("time")) o.get("time").asLong else 0L,
                        if (o.has("amount")) o.get("amount").asFloat else 0f,
                        if (o.has("source")) o.get("source").asString else "未知",
                        o.has("death") && o.get("death").asBoolean,
                        if (o.has("deathMessage") && !o.get("deathMessage").isJsonNull) o.get("deathMessage").asString else null,
                    )
                    records.add(rec)
                }
                records.sortByDescending { it.time }
                while (records.size > HitReplayConfig.maxRecords) records.removeAt(records.size - 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun save() {
        try {
            val arr = JsonArray()
            synchronized(lock) {
                for (r in records) {
                    val o = JsonObject()
                    o.addProperty("time", r.time)
                    o.addProperty("amount", r.amount)
                    o.addProperty("source", r.source)
                    o.addProperty("death", r.death)
                    if (r.deathMessage != null) {
                        o.addProperty("deathMessage", r.deathMessage)
                    } else {
                        o.add("deathMessage", JsonNull.INSTANCE)
                    }
                    arr.add(o)
                }
            }
            val path = logPath
            path.parent?.let { Files.createDirectories(it) }
            Files.writeString(path, gson.toJson(arr))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
