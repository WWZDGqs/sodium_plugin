package ws.sodiunplugin.feature

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object PlayerHighlightConfig {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    @Volatile
    var enabled: Boolean = false

    @JvmStatic
    @Volatile
    var range: Int = 64
        set(value) {
            field = value.coerceIn(8, 128)
        }

    @JvmStatic
    @Volatile
    var color: HighlightColor = HighlightColor.WHITE

    private val configPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("sodium_plugin/player_highlight.json")

    @JvmStatic
    fun load() {
        val path = configPath
        if (!Files.exists(path)) {
            save()
            return
        }
        try {
            val root = gson.fromJson(Files.readString(path), JsonObject::class.java) ?: return
            if (root.has("enabled")) enabled = root.get("enabled").asBoolean
            if (root.has("range")) range = root.get("range").asInt
            if (root.has("color")) {
                val name = root.get("color").asString
                color = runCatching { HighlightColor.valueOf(name) }.getOrDefault(HighlightColor.WHITE)
            }
        } catch (e: Exception) {
            save()
        }
    }

    @JvmStatic
    fun save() {
        try {
            val root = JsonObject()
            root.addProperty("enabled", enabled)
            root.addProperty("range", range)
            root.addProperty("color", color.name)
            val path = configPath
            path.parent?.let { Files.createDirectories(it) }
            Files.writeString(path, gson.toJson(root))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
