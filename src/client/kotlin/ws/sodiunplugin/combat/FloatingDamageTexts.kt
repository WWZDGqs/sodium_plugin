package ws.sodiunplugin.combat

import java.util.concurrent.ConcurrentLinkedQueue

object FloatingDamageTexts {
    private const val LIFETIME_MS = 900L
    private const val RISE_PX = 26f

    private data class F(@JvmField val amount: Float, @JvmField val spawn: Long)

    private val list: ConcurrentLinkedQueue<F> = ConcurrentLinkedQueue()

    @JvmStatic
    fun add(amount: Float) {
        list.add(F(amount, System.currentTimeMillis()))
        while (list.size > 30) list.poll()
    }

    @JvmStatic
    fun tick() {
        val now = System.currentTimeMillis()
        val it = list.iterator()
        while (it.hasNext()) {
            if (now - it.next().spawn > LIFETIME_MS) it.remove()
        }
    }

    @JvmStatic
    fun collect(): List<FloatArray> {
        val now = System.currentTimeMillis()
        val result = mutableListOf<FloatArray>()
        for (f in list) {
            val age = (now - f.spawn).toFloat()
            val progress = (age / LIFETIME_MS).coerceIn(0f, 1f)
            result.add(floatArrayOf(f.amount, 1f - progress))
        }
        return result
    }

    @JvmStatic
    fun risePx(): Float = RISE_PX
}
