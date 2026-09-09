package ws.sodiunplugin.feature

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object AutoScreenshotConfig {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    @Volatile
    var enabled: Boolean = false

    @JvmStatic
    @Volatile
    var killEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var deathEnabled: Boolean = true

    private val configPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("sodium_plugin/auto_screenshot.json")

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
            if (root.has("killEnabled")) killEnabled = root.get("killEnabled").asBoolean
            if (root.has("deathEnabled")) deathEnabled = root.get("deathEnabled").asBoolean
        } catch (e: Exception) {
            save()
        }
    }

    @JvmStatic
    fun save() {
        try {
            val root = JsonObject()
            root.addProperty("enabled", enabled)
            root.addProperty("killEnabled", killEnabled)
            root.addProperty("deathEnabled", deathEnabled)
            val path = configPath
            path.parent?.let { Files.createDirectories(it) }
            Files.writeString(path, gson.toJson(root))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
