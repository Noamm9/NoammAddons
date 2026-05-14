package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons.mc
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.nbt.*
import net.minecraft.world.item.ItemStack
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64

internal data class VirtualInventory(val stacks: List<ItemStack>) {
    val rows = stacks.size / 9

    fun encode(): String {
        val registryAccess = getRegistryAccess()
        val list = ListTag()

        stacks.forEach { stack ->
            val tag = CompoundTag()

            if (! stack.isEmpty) {
                tag.putString("id", stack.itemHolder.unwrapKey().orElseThrow().location().toString())
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
            val root = CompoundTag().apply { put(KEY, list) }
            NbtIo.writeCompressed(root, baos)
            Base64.encode(baos.toByteArray())
        }
    }

    companion object {
        private const val KEY = "INVENTORY"

        fun decode(encoded: String) = runCatching {
            val registryAccess = getRegistryAccess()
            val bytes = Base64.decode(encoded)

            ByteArrayInputStream(bytes).use { bais ->
                val root = NbtIo.readCompressed(bais, NbtAccounter.unlimitedHeap())
                val list = root.getList(KEY).get()

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

                VirtualInventory(items)
            }
        }.onFailure { it.printStackTrace() }.getOrNull()

        private fun getRegistryAccess() = mc.level?.registryAccess()
            ?: mc.connection?.registryAccess()
            ?: error("No registry access available")
    }
}