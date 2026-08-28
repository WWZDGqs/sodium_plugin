package ws.sodiunplugin.hud

import net.minecraft.client.MinecraftClient
import net.minecraft.entity.LivingEntity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 伤害显示条的数据源与渲染状态。
 *
 * 捕获流程：
 *  1. [ws.sodiunplugin.mixin.client.ClientPlayNetworkHandlerDamageMixin] 在收到
 *     [net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket] 时调用 [markPending]，
 *     记录"我造成的伤害"目标实体**受伤前**的血量/吸收血快照及来源信息。
 *  2. 随后的血量/吸收血同步包会把客户端实体数值降到受伤后，[tickSettle] 在结算窗口结束后
 *     用 快照值 − 实体当前值 得到实际伤害量并生成显示条目。
 *
 * 渲染逻辑由 [ws.sodiunplugin.mixin.client.InGameHudDamageMixin] 在每帧 HUD 绘制时调用
 * [collectActive] 取出尚未淡出的条目并绘制。
 */
object DamageDisplayStore {

    /** 单条伤害记录的存活时间（毫秒）。超过后不再显示。 */
    private const val LIFETIME_MS = 2500L

    /** 完全不透明的时间窗口（毫秒），之后开始线性淡出直到 LIFETIME_MS。 */
    private const val SOLID_MS = 1200L

    /** 待结算实体的最大存活时间（毫秒）。超过未收到血量同步则丢弃，避免内存泄漏。 */
    private const val PENDING_TTL_MS = 1000L

    /** 结算窗口（毫秒）：markPending 后等待血量同步包到达再结算。 */
    private const val SETTLE_WINDOW_MS = 220L

    /**
     * 单条伤害记录。文本在捕获时即组装好，避免渲染线程重复访问实体。
     */
    data class Entry(
        @JvmField val text: String,
        @JvmField val timestamp: Long,
    )

    /** 伤害记录队列。并发安全，捕获与渲染分属不同线程/调用时机。 */
    private val queue: ConcurrentLinkedQueue<Entry> = ConcurrentLinkedQueue()

    /** 队列最大长度，防止极端情况下无限堆积。 */
    private const val MAX_ENTRIES = 8

    /**
     * 待结算项。登记"我造成的伤害"时记录受伤前的血量/吸收血快照，
     * 待结算窗口结束后与实体当前值对比得出实际伤害。
     */
    data class Pending(
        @JvmField val weaponName: String?,
        @JvmField val damageTypeName: String,
        @JvmField val timestamp: Long,
        @JvmField val oldHealth: Float,
        @JvmField val oldAbsorption: Float,
    )
    private val pending: ConcurrentHashMap<Int, Pending> = ConcurrentHashMap()

    /**
     * 登记一次"我造成的伤害"的待结算项。
     *
     * @param entityId       实体 id
     * @param weaponName     武器名（可能为 null）
     * @param damageTypeName 伤害类型名
     * @param oldHealth      受伤前的主血量快照
     * @param oldAbsorption  受伤前的吸收血（金血条）快照
     */
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

    /**
     * 每客户端 tick 调用：检查待结算实体，待结算窗口（[SETTLE_WINDOW_MS]）结束后，
     * 用登记时的受伤前快照减去实体当前血量/吸收血，得到实际伤害并生成显示条目。
     */
    @JvmStatic
    fun tickSettle() {
        if (pending.isEmpty()) {
            return
        }
        val client = MinecraftClient.getInstance()
        val world = client?.world ?: run {
            // 世界不可用（如已断开），清空待结算避免泄漏
            pending.clear()
            return
        }
        val now = System.currentTimeMillis()
        val iterator = pending.entries.iterator()
        while (iterator.hasNext()) {
            val (entityId, p) = iterator.next()
            // 过期保护：登记超过 TTL 仍未结算则丢弃
            if (now - p.timestamp > PENDING_TTL_MS) {
                iterator.remove()
                continue
            }
            // 结算窗口未结束：继续等待血量/吸收血同步包
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
            // 无论是否有伤害都移除，避免重复结算
            iterator.remove()
        }
    }

    /**
     * 记录一次伤害事件。
     *
     * @param targetName 受伤实体的显示名
     * @param amount     实际伤害量（点）
     * @param weaponName 造成伤害的武器/来源名（可能为 null 或空）
     * @param damageTypeName 伤害类型名（如 "generic"、"mob"、"player" 等）
     */
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

    /**
     * 取出当前仍然需要显示的记录（已自动剔除过期条目）。
     * 返回列表按入队顺序排列，最新的在末尾。
     */
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

    /**
     * 根据记录年龄计算 alpha（0.0–1.0）。前 [SOLID_MS] 毫秒完全不透明，
     * 之后线性淡出到 [LIFETIME_MS] 时为 0。
     */
    @JvmStatic
    fun alphaFor(entry: Entry, now: Long): Float {
        val age = now - entry.timestamp
        return when {
            age <= SOLID_MS -> 1.0f
            age >= LIFETIME_MS -> 0.0f
            else -> 1.0f - ((age - SOLID_MS).toFloat() / (LIFETIME_MS - SOLID_MS).toFloat())
        }
    }

    /** 当前是否处于游戏内（有本地玩家与窗口），供 Mixin 快速判断。 */
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
