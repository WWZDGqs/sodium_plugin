package ws.sodiunplugin.hitreplay

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * 受击回放的设置项，持久化到本模组配置文件（hit_replay.json）。
 */
object HitReplayConfig {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** 是否记录玩家受到的伤害（受击回放总开关）。 */
    @JvmStatic
    @Volatile
    var recordEnabled: Boolean = true

    /** 最多保留的受击记录条数（超出后丢弃最旧）。 */
    @JvmStatic
    @Volatile
    var maxRecords: Int = 50
        set(value) {
            field = value.coerceIn(10, 500)
        }

    private val configPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("sodium_plugin/hit_replay.json")

    @JvmStatic
    fun load() {
        val path = configPath
        if (!Files.exists(path)) {
            save()
            return
        }
        try {
            val root = gson.fromJson(Files.readString(path), JsonObject::class.java) ?: return
            if (root.has("recordEnabled")) recordEnabled = root.get("recordEnabled").asBoolean
            if (root.has("maxRecords")) maxRecords = root.get("maxRecords").asInt
        } catch (e: Exception) {
            save()
        }
    }

    @JvmStatic
    fun save() {
        try {
            val root = JsonObject()
            root.addProperty("recordEnabled", recordEnabled)
            root.addProperty("maxRecords", maxRecords)
            val path = configPath
            path.parent?.let { Files.createDirectories(it) }
            Files.writeString(path, gson.toJson(root))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
