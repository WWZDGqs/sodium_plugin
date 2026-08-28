package ws.sodiunplugin.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * 视角抖动控制的配置。所有字段都可被 Sodium 设置界面中的滑块/开关修改，
 * 并持久化到本模组的配置文件（sodium-view-shake.json）。
 */
object ShakeConfig {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** 视角抖动强度，0 表示完全关闭，100 表示原版 100% 抖动。赋值会自动夹取到 0–100。 */
    @JvmStatic
    @Volatile
    var shakeStrength: Int = 100
        set(value) {
            field = value.coerceIn(0, 100)
        }

    /** 疾跑时是否额外增强视角抖动。关闭后疾跑与走路使用相同幅度。 */
    @JvmStatic
    @Volatile
    var sprintShakeEnabled: Boolean = true

    /** 受到攻击时是否产生视角倾斜抖动。关闭后受击不再抖屏。 */
    @JvmStatic
    @Volatile
    var damageShakeEnabled: Boolean = true

    /** 反胃（河豚）视角扭曲效果开关。关闭后吃河豚不再产生扭曲画面。 */
    @JvmStatic
    @Volatile
    var nauseaEnabled: Boolean = true

    /** 药水视场角缩放开关（速度/神龟等）。关闭后这些药水不再广角/缩窄视野。 */
    @JvmStatic
    @Volatile
    var potionFovEnabled: Boolean = true

    /** 在 HUD 药水效果图标处显示剩余时间（秒）。 */
    @JvmStatic
    @Volatile
    var potionTimeEnabled: Boolean = true

    /** 当药水效果剩余时间 ≤10 秒（≤200 tick）时，在效果贴图位置绘制闪烁彩色边框。 */
    @JvmStatic
    @Volatile
    var potionBorderEnabled: Boolean = true

    /** 在 HUD 血量与饱食度上方显示伤害显示条（我造成的伤害：目标、伤害量、来源）。 */
    @JvmStatic
    @Volatile
    var damageDisplayEnabled: Boolean = true

    /**
     * 爆炸伤害归因到玩家。开启后，由爆炸类伤害（末地水晶、重生锚、TNT 等，伤害类型
     * 为 explosion / player_explosion / badRespawnPoint）造成的伤害也会显示为"我造成的伤害"。
     *
     * 客户端无法直接判断某次爆炸是否由"我放置"的实体触发（水晶爆炸的 source 指向水晶实体、
     * 重生锚爆炸 source 通常为 0），故采用类型判定：玩家放置的水晶/重生锚/TNT 爆炸都能正确显示，
     * 代价是服务器端其他爆炸也可能被计入。关闭则只显示直接攻击类伤害。
     */
    @JvmStatic
    @Volatile
    var explosionCreditEnabled: Boolean = true

    /** 粒子数量百分比，0 表示完全不生成粒子，100 表示原版全部生成。 */
    @JvmStatic
    @Volatile
    var particlePercentage: Int = 100
        set(value) {
            field = value.coerceIn(0, 100)
        }

    /** 视场角效果系数百分比，100 为原版，50 为缩小一半视野，300 为三倍广角。 */
    @JvmStatic
    @Volatile
    var fovEffect: Int = 100
        set(value) {
            field = value.coerceIn(50, 300)
        }

    /** 伽马值刻度（1–3000）。实际 gamma = value / 200，100 等于原版默认亮度 0.5。 */
    @JvmStatic
    @Volatile
    var gammaValue: Int = 100
        set(value) {
            field = value.coerceIn(1, 3000)
        }

    /** 供 LightmapGammaMixin 读取的实际伽马值（无 0.0–1.0 范围限制）。 */
    @JvmStatic
    fun getGammaDouble(): Double = gammaValue / 200.0

    /** 显示隐身玩家。开启后，喝隐身药水等处于隐形状态的玩家会像正常玩家一样完全可见。 */
    @JvmStatic
    @Volatile
    var showInvisiblePlayers: Boolean = false

    private val configPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("sodium-view-shake.json")

    /** 0–1 之间的强度系数，供 Mixin 直接相乘使用。 */
    @JvmStatic
    fun strengthFactor(): Float = shakeStrength / 100.0f

    /** 视场角效果的乘法系数（0.5–3.0），供 Mixin 使用。 */
    @JvmStatic
    fun fovFactor(): Float = fovEffect / 100.0f

    // ----- 持久化 -----

    @JvmStatic
    fun load() {
        val path = configPath
        if (!Files.exists(path)) {
            save()
            return
        }
        try {
            val root = gson.fromJson(Files.readString(path), JsonObject::class.java) ?: return
            if (root.has("shakeStrength")) {
                shakeStrength = root.get("shakeStrength").asInt
            }
            if (root.has("sprintShakeEnabled")) {
                sprintShakeEnabled = root.get("sprintShakeEnabled").asBoolean
            }
            if (root.has("damageShakeEnabled")) {
                damageShakeEnabled = root.get("damageShakeEnabled").asBoolean
            }
            if (root.has("nauseaEnabled")) {
                nauseaEnabled = root.get("nauseaEnabled").asBoolean
            }
            if (root.has("potionFovEnabled")) {
                potionFovEnabled = root.get("potionFovEnabled").asBoolean
            }
            if (root.has("potionTimeEnabled")) {
                potionTimeEnabled = root.get("potionTimeEnabled").asBoolean
            }
            if (root.has("potionBorderEnabled")) {
                potionBorderEnabled = root.get("potionBorderEnabled").asBoolean
            }
            if (root.has("damageDisplayEnabled")) {
                damageDisplayEnabled = root.get("damageDisplayEnabled").asBoolean
            }
            if (root.has("explosionCreditEnabled")) {
                explosionCreditEnabled = root.get("explosionCreditEnabled").asBoolean
            }
            if (root.has("particlePercentage")) {
                particlePercentage = root.get("particlePercentage").asInt
            }
            if (root.has("fovEffect")) {
                fovEffect = root.get("fovEffect").asInt
            }
            if (root.has("gammaValue")) {
                gammaValue = root.get("gammaValue").asInt
            }
            if (root.has("showInvisiblePlayers")) {
                showInvisiblePlayers = root.get("showInvisiblePlayers").asBoolean
            }
        } catch (e: Exception) {
            // 配置文件损坏，回退到默认值并覆盖保存
            save()
        }
    }

    @JvmStatic
    fun save() {
        try {
            val root = JsonObject()
            root.addProperty("shakeStrength", shakeStrength)
            root.addProperty("sprintShakeEnabled", sprintShakeEnabled)
            root.addProperty("damageShakeEnabled", damageShakeEnabled)
            root.addProperty("nauseaEnabled", nauseaEnabled)
            root.addProperty("potionFovEnabled", potionFovEnabled)
            root.addProperty("potionTimeEnabled", potionTimeEnabled)
            root.addProperty("potionBorderEnabled", potionBorderEnabled)
            root.addProperty("damageDisplayEnabled", damageDisplayEnabled)
            root.addProperty("explosionCreditEnabled", explosionCreditEnabled)
            root.addProperty("particlePercentage", particlePercentage)
            root.addProperty("fovEffect", fovEffect)
            root.addProperty("gammaValue", gammaValue)
            root.addProperty("showInvisiblePlayers", showInvisiblePlayers)
            val path = configPath
            if (path.parent != null) {
                Files.createDirectories(path.parent)
            }
            Files.writeString(path, gson.toJson(root))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
