package ws.warzoneutils.hitreplay

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

    data class HitRecord(
        @JvmField val time: Long,
        @JvmField val amount: Float,
        @JvmField val source: String,
        @JvmField val weaponName: String?,
        @JvmField val damageType: String,
        @JvmField val distance: Float,
        @JvmField val critical: Boolean,
        @JvmField val death: Boolean,
        @JvmField val deathMessage: String?,
    )

    private const val SETTLE_WINDOW_MS = 250L

    @Volatile
    private var eventActive = false
    private var eventHealthLoss = 0f
    private var eventAbsorptionLoss = 0f
    private var eventSource = ""
    private var eventWeapon: String? = null
    private var eventDamageType = ""
    private var eventDistance = 0f
    private var eventCritical = false
    private var eventStartTime = 0L
    private var eventLastUpdate = 0L
    private var eventDeathMessage: String? = null

    private val records: MutableList<HitRecord> = mutableListOf()
    private val lock = Any()

    @JvmStatic
    fun beginEvent(source: String, time: Long, weaponName: String?, damageType: String, distance: Float, critical: Boolean) {
        if (!HitReplayConfig.recordEnabled) {
            return
        }
        synchronized(lock) {
            if (eventActive) finalizeEventLocked()
            eventSource = source
            eventWeapon = weaponName
            eventDamageType = damageType
            eventDistance = distance
            eventCritical = critical
            eventHealthLoss = 0f
            eventAbsorptionLoss = 0f
            eventStartTime = time
            eventLastUpdate = time
            eventDeathMessage = null
            eventActive = true
        }
    }

    @JvmStatic
    fun addHealthLoss(amount: Float) {
        if (amount <= 0f || !HitReplayConfig.recordEnabled) {
            return
        }
        synchronized(lock) {
            if (!eventActive) {
                return
            }
            eventHealthLoss += amount
            eventLastUpdate = System.currentTimeMillis()
        }
    }

    @JvmStatic
    fun addAbsorptionLoss(amount: Float) {
        if (amount <= 0f || !HitReplayConfig.recordEnabled) return
        synchronized(lock) {
            if (!eventActive) {
                return
            }
            eventAbsorptionLoss += amount
            eventLastUpdate = System.currentTimeMillis()
        }
    }

    @JvmStatic
    fun recordDeath(message: String?) {
        if (!HitReplayConfig.recordEnabled) {
            return
        }
        synchronized(lock) {
            if (eventActive) {
                eventDeathMessage = message
                finalizeEventLocked()
            }
        }
    }

    private fun finalizeEventLocked() {
        if (!eventActive) return
        val total = eventHealthLoss + eventAbsorptionLoss
        if (total > 0f || eventDeathMessage != null) {
            addRecordLocked(
                HitRecord(
                    eventStartTime,
                    total,
                    eventSource,
                    eventWeapon,
                    eventDamageType,
                    eventDistance,
                    eventCritical,
                    eventDeathMessage != null,
                    eventDeathMessage,
                )
            )
        }
        eventActive = false
        eventHealthLoss = 0f
        eventAbsorptionLoss = 0f
        eventDeathMessage = null
        eventWeapon = null
        eventDamageType = ""
        eventDistance = 0f
        eventCritical = false
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

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val logPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("warzoneutils/hit_replay_log.json")

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
                    val source = if (o.has("source")) o.get("source").asString else "未知"
                    if (source == "环境伤害") continue
                    val rec = HitRecord(
                        if (o.has("time")) o.get("time").asLong else 0L,
                        if (o.has("amount")) o.get("amount").asFloat else 0f,
                        source,
                        if (o.has("weaponName") && !o.get("weaponName").isJsonNull) o.get("weaponName").asString else null,
                        if (o.has("damageType")) o.get("damageType").asString else "",
                        if (o.has("distance")) o.get("distance").asFloat else 0f,
                        o.has("critical") && o.get("critical").asBoolean,
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
                    if (r.weaponName != null) o.addProperty("weaponName", r.weaponName) else o.add("weaponName", JsonNull.INSTANCE)
                    o.addProperty("damageType", r.damageType)
                    o.addProperty("distance", r.distance)
                    o.addProperty("critical", r.critical)
                    o.addProperty("death", r.death)
                    if (r.deathMessage != null) o.addProperty("deathMessage", r.deathMessage) else o.add("deathMessage", JsonNull.INSTANCE)
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
