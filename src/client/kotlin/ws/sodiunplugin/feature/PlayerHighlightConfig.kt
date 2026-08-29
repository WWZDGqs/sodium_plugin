package ws.sodiunplugin.feature

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * 附近玩家高亮（发光轮廓）的设置项，持久化到本模组配置文件（player_highlight.json）。
 *
 * 颜色以枚举名保存（如 "GREEN"），避免依赖枚举序数——日后新增颜色不会打乱已保存配置。
 */
object PlayerHighlightConfig {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** 是否高亮附近的其他玩家。 */
    @JvmStatic
    @Volatile
    var enabled: Boolean = false

    /** 高亮生效的最大距离（格），超出范围的玩家不高亮。 */
    @JvmStatic
    @Volatile
    var range: Int = 64
        set(value) {
            field = value.coerceIn(8, 128)
        }

    /** 轮廓颜色。 */
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
                // 解析失败时保持默认色，不因单个字段损坏而重置整个配置。
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
