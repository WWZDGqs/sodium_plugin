package ws.sodiunplugin.hud

import net.minecraft.client.MinecraftClient
import net.minecraft.entity.LivingEntity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object DamageDisplayStore {
    private const val PENDING_TTL_MS = 5000L
    private const val SETTLE_WINDOW_MS = 150L
    private const val SOLID_MS = 1000L
    private const val LIFETIME_MS = 3000L
    private const val MAX_ENTRIES = 20

    data class Entry(@JvmField val text: String, @JvmField val timestamp: Long)

    private val queue: ConcurrentLinkedQueue<Entry> = ConcurrentLinkedQueue()
    private val pending: ConcurrentHashMap<Int, Pending> = ConcurrentHashMap()

    data class Pending(
        @JvmField val weaponName: String?,
        @JvmField val damageTypeName: String,
        @JvmField val timestamp: Long,
        @JvmField val oldHealth: Float,
        @JvmField val oldAbsorption: Float,
    )

    @JvmStatic
    fun markPending(
        entityId: Int,
        weaponName: String?,
        damageTypeName: String,
        oldHealth: Float,
        oldAbsorption: Float,
    ) {
        val now = System.currentTimeMillis()
        pending[entityId] = Pending(weaponName, damageTypeName, now, oldHealth, oldAbsorption)
    }

    @JvmStatic
    fun tickSettle() {
        if (pending.isEmpty()) {
            return
        }
        val client = MinecraftClient.getInstance()
        val world = client?.world ?: run {
            pending.clear()
            return
        }
        val now = System.currentTimeMillis()
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            val (entityId, p) = iterator.next()
            if (now - p.timestamp > PENDING_TTL_MS) {
                iterator.remove()
                continue
            }
            if (now - p.timestamp < SETTLE_WINDOW_MS) {
                continue
            }
            val entity = world.getEntityById(entityId) as? LivingEntity
            if (entity != null) {
                val healthLoss = (p.oldHealth - entity.health).coerceAtLeast(0.0f)
                val absorptionLoss = (p.oldAbsorption - entity.absorptionAmount).coerceAtLeast(0.0f)
                val amount = healthLoss + absorptionLoss
                if (amount > 0.0f) {
                    val targetName = entity.name.string ?: "目标"
                    record(targetName, amount, p.weaponName, p.damageTypeName)
                }
            }
            iterator.remove()
        }
    }

    @JvmStatic
    fun record(targetName: String, amount: Float, weaponName: String?, damageTypeName: String) {
        val weapon = when {
            !weaponName.isNullOrBlank() -> weaponName
            else -> damageTypeName
        }
        val text = "对 $targetName 造成 %.1f 点伤害 ($weapon)".format(amount)
        queue.add(Entry(text, System.currentTimeMillis()))
        while (queue.size > MAX_ENTRIES) {
            queue.poll()
        }
    }

    @JvmStatic
    fun collectActive(): List<Entry> {
        val now = System.currentTimeMillis()
        val iterator = queue.iterator()
        val result = mutableListOf<Entry>()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.timestamp > LIFETIME_MS) {
                iterator.remove()
                continue
            }
            result.add(entry)
        }
        return result
    }

    @JvmStatic
    fun alphaFor(entry: Entry, now: Long): Float {
        val age = now - entry.timestamp
        return when {
            age <= SOLID_MS -> 1.0f
            age >= LIFETIME_MS -> 0.0f
            else -> 1.0f - ((age - SOLID_MS).toFloat() / (LIFETIME_MS - SOLID_MS).toFloat())
        }
    }

    @JvmStatic
    fun isInGame(): Boolean {
        return try {
            val client = MinecraftClient.getInstance()
            client != null && client.player != null && client.world != null
        } catch (_: Throwable) {
            false
        }
    }
}
