package ws.sodiunplugin.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object ShakeConfig {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    @Volatile
    var shakeStrength: Int = 100
        set(value) {
            field = value.coerceIn(0, 100)
        }

    @JvmStatic
    @Volatile
    var sprintShakeEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var damageShakeEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var nauseaEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var potionFovEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var potionTimeEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var potionBorderEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var damageDisplayEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var damageFloatEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var comboEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var projectileCooldownEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var explosionCreditEnabled: Boolean = true

    @JvmStatic
    @Volatile
    var particlePercentage: Int = 100
        set(value) {
            field = value.coerceIn(0, 100)
        }

    @JvmStatic
    @Volatile
    var fovEffect: Int = 100
        set(value) {
            field = value.coerceIn(50, 300)
        }

    @JvmStatic
    @Volatile
    var gammaValue: Int = 100
        set(value) {
            field = value.coerceIn(1, 3000)
        }

    @JvmStatic
    fun getGammaDouble(): Double = gammaValue / 200.0

    @JvmStatic
    @Volatile
    var showInvisiblePlayers: Boolean = false

    private val configPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("sodium-view-shake.json")

    @JvmStatic
    fun strengthFactor(): Float = shakeStrength / 100.0f

    @JvmStatic
    fun fovFactor(): Float = fovEffect / 100.0f


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
            if (root.has("damageFloatEnabled")) {
                damageFloatEnabled = root.get("damageFloatEnabled").asBoolean
            }
            if (root.has("comboEnabled")) {
                comboEnabled = root.get("comboEnabled").asBoolean
            }
            if (root.has("projectileCooldownEnabled")) {
                projectileCooldownEnabled = root.get("projectileCooldownEnabled").asBoolean
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
            root.addProperty("damageFloatEnabled", damageFloatEnabled)
            root.addProperty("comboEnabled", comboEnabled)
            root.addProperty("projectileCooldownEnabled", projectileCooldownEnabled)
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
