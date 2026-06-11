package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.items.ItemRarity
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.items.ItemUtils.lore
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.nbt.*
import net.minecraft.world.item.ItemStack
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64

data class NBTInventory(val stacks: List<ItemStack>) {
    val rows = stacks.size / 9

    /** Rarity per slot, computed once - the stacks are immutable, so [ItemUtils.getRarity] need not run every frame. */
    val rarities: List<ItemRarity> by lazy(LazyThreadSafetyMode.NONE) { stacks.map(ItemUtils::getRarity) }

    class SearchText(val name: String, val lore: String)

    /**
     * Pre-extracted unformatted name + lore per slot (lore lines joined - a query can't span the line separator),
     * so search matching while typing is a plain contains instead of rebuilding these strings per item per frame.
     * Built on first use, i.e. only if the search box is actually used while this page is visible.
     */
    val searchTexts: List<SearchText?> by lazy(LazyThreadSafetyMode.NONE) {
        stacks.map { stack ->
            if (stack.isEmpty) null
            else SearchText(stack.hoverName.unformattedText, stack.lore.joinToString("\n") { it.removeFormatting() })
        }
    }

    /**
     * Memoized: the stacks are immutable, and [StorageOverlay] re-saves the whole file on every page open - without
     * this, all 27 pages would redo codec + gzip + Base64 each time when only the freshly opened page is new.
     */
    val encoded: String by lazy { encode() }

    private fun encode(): String {
        val registryAccess = getRegistryAccess()
        val list = ListTag()

        stacks.forEach { stack ->
            val tag = CompoundTag()

            if (! stack.isEmpty) {
                tag.putString("id", stack.itemHolder.unwrapKey().orElseThrow().identifier().toString())
                tag.putInt("count", stack.count)

                val patch = DataComponentPatch.CODEC.encodeStart(
                    registryAccess.createSerializationContext(NbtOps.INSTANCE),
                    stack.componentsPatch
                ).result().orElse(null)

                if (patch != null) tag.put("components", patch)
            }

            list.add(tag)
        }

        return ByteArrayOutputStream().use { baos ->
            val root = CompoundTag().apply { put("i", list) }
            NbtIo.writeCompressed(root, baos)
            Base64.encode(baos.toByteArray())
        }
    }

    companion object {
        fun decode(encoded: String) = runCatching {
            val registryAccess = getRegistryAccess()
            val bytes = Base64.decode(encoded)

            ByteArrayInputStream(bytes).use { bais ->
                val root = NbtIo.readCompressed(bais, NbtAccounter.unlimitedHeap())
                val list = root.getList("i").get()

                val items = buildList {
                    for (i in list.indices) {
                        val tag = list.getCompound(i).get()

                        if (tag.isEmpty) {
                            add(ItemStack.EMPTY)
                            continue
                        }

                        add(ItemStack.CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY))
                    }
                }

                NBTInventory(items)
            }
        }.onFailure { it.printStackTrace() }.getOrNull()

        private fun getRegistryAccess() = mc.level?.registryAccess()
            ?: mc.connection?.registryAccess()
            ?: error("No registry access available")
    }
}