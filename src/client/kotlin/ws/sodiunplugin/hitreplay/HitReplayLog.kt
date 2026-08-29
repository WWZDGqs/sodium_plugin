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


object HitReplayLog {


    private const val DEBUG = true
    private fun dbg(msg: String) {
        if (DEBUG) println("[HitReplay] $msg")
    }


    data class HitRecord(
        @JvmField val time: Long,
        @JvmField val amount: Float,
        @JvmField val source: String,
        @JvmField val death: Boolean,
        @JvmField val deathMessage: String?,
    )


    private const val SETTLE_WINDOW_MS = 250L

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
