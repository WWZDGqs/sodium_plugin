package ws.sodiunplugin.hitreplay

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path


object HitReplayConfig {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    @Volatile
    var recordEnabled: Boolean = true


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