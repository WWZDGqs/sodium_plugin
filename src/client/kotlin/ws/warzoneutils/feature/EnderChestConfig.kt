package ws.warzoneutils.feature

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object EnderChestConfig {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    @Volatile
    var enabled: Boolean = true

    @JvmStatic
    @Volatile
    var showSnapshotButton: Boolean = true

    private val configPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("warzoneutils/ender_chest.json")

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
            if (root.has("showSnapshotButton")) showSnapshotButton = root.get("showSnapshotButton").asBoolean
        } catch (e: Exception) {
            save()
        }
    }

    @JvmStatic
    fun save() {
        try {
            val root = JsonObject()
            root.addProperty("enabled", enabled)
            root.addProperty("showSnapshotButton", showSnapshotButton)
            val path = configPath
            path.parent?.let { Files.createDirectories(it) }
            Files.writeString(path, gson.toJson(root))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
