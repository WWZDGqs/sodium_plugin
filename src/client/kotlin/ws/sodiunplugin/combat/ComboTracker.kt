package ws.sodiunplugin.combat

object ComboTracker {
    private const val WINDOW_MS = 2500L

    @Volatile
    var combo: Int = 0
        private set

    @Volatile
    var maxCombo: Int = 0
        private set

    @Volatile
    private var lastHit: Long = 0

    @JvmStatic
    fun registerHit() {
        val now = System.currentTimeMillis()
        combo++
        if (combo > maxCombo) maxCombo = combo
        lastHit = now
    }

    @JvmStatic
    fun tick() {
        if (combo > 0 && System.currentTimeMillis() - lastHit > WINDOW_MS) {
            combo = 0
        }
    }
}
