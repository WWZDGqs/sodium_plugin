package ws.sodiunplugin.feature

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object ChestEspConfig {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    @Volatile
    var enabled: Boolean = false

    @JvmStatic
    @Volatile
    var range: Int = 32
        set(value) {
            field = value.coerceIn(8, 128)
        }

    @JvmStatic
    @Volatile
    var showChest: Boolean = true

    @JvmStatic
    @Volatile
    var showShulkerBox: Boolean = true

    @JvmStatic
    @Volatile
    var showEnderChest: Boolean = true

    private val configPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("sodium_plugin/chest_esp.json")

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
            if (root.has("showChest")) showChest = root.get("showChest").asBoolean
            if (root.has("showShulkerBox")) showShulkerBox = root.get("showShulkerBox").asBoolean
            if (root.has("showEnderChest")) showEnderChest = root.get("showEnderChest").asBoolean
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
            root.addProperty("showChest", showChest)
            root.addProperty("showShulkerBox", showShulkerBox)
            root.addProperty("showEnderChest", showEnderChest)
            val path = configPath
            path.parent?.let { Files.createDirectories(it) }
            Files.writeString(path, gson.toJson(root))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
