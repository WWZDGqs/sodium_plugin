package ws.warzoneutils.feature

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtEnd
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.registry.RegistryOps
import java.nio.file.Files
import java.nio.file.Path

object EnderChestSnapshot {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val SIZE_LIMIT: NbtSizeTracker = NbtSizeTracker.of(16L * 1024L * 1024L)

    const val SIZE = 27

    @Volatile
    private var stacks: MutableList<ItemStack> = MutableList(SIZE) { ItemStack.EMPTY }

    @Volatile
    var capturedAt: Long = 0L
        private set

    @JvmStatic
    fun hasSnapshot(): Boolean = capturedAt > 0L

    @JvmStatic
    fun getStacks(): List<ItemStack> = stacks

    @JvmStatic
    fun getStack(index: Int): ItemStack = stacks.getOrNull(index) ?: ItemStack.EMPTY

    private val snapshotPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("warzoneutils/ender_chest_snapshot.nbt")

    private val metaPath: Path
        get() = FabricLoader.getInstance().configDir.resolve("warzoneutils/ender_chest_snapshot.json")

    @JvmStatic
    fun capture(contents: List<ItemStack>, ops: RegistryOps<NbtElement>) {
        val normalized = MutableList(SIZE) { ItemStack.EMPTY }
        for (i in 0 until SIZE) {
            normalized[i] = contents.getOrNull(i)?.copy() ?: ItemStack.EMPTY
        }
        stacks = normalized
        capturedAt = System.currentTimeMillis()
        save(ops)
    }

    private fun save(ops: RegistryOps<NbtElement>) {
        try {
            val list = NbtList()
            for (i in 0 until SIZE) {
                val encoded = ItemStack.CODEC.encodeStart(ops, stacks[i]).result().orElse(null)
                list.add(i, encoded ?: NbtEnd.INSTANCE)
            }
            val root = NbtCompound()
            root.put("items", list)
            val path = snapshotPath
            path.parent?.let { Files.createDirectories(it) }
            NbtIo.writeCompressed(root, path)

            val meta = JsonObject()
            meta.addProperty("capturedAt", capturedAt)
            meta.addProperty("nonEmptySlots", stacks.count { !it.isEmpty })
            Files.writeString(metaPath, gson.toJson(meta))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun load(ops: RegistryOps<NbtElement>) {
        val path = snapshotPath
        if (!Files.exists(path)) return
        try {
            val root = NbtIo.readCompressed(path, SIZE_LIMIT)
            val list = root.getList("items").orElse(null) ?: return
            val loaded = MutableList(SIZE) { ItemStack.EMPTY }
            for (i in 0 until SIZE) {
                if (i >= list.size) break
                val element = list.get(i) as? NbtElement ?: continue
                if (element is NbtEnd) continue
                loaded[i] = ItemStack.CODEC.parse(ops, element).result().orElse(ItemStack.EMPTY)
            }
            stacks = loaded
            if (Files.exists(metaPath)) {
                val meta = gson.fromJson(Files.readString(metaPath), JsonObject::class.java)
                if (meta != null && meta.has("capturedAt")) {
                    capturedAt = meta.get("capturedAt").asLong
                }
            } else {
                capturedAt = Files.getLastModifiedTime(path).toMillis()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
