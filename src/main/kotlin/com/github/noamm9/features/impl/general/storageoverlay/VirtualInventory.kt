package com.github.noamm9.features.impl.general.storageoverlay

import com.github.noamm9.NoammAddons
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.world.item.ItemStack
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64

data class VirtualInventory(val stacks: List<ItemStack>) {
    val rows = stacks.size / 9


    fun serialize(): String {
        val list = ListTag()
        val ops = NoammAddons.mc.level?.registryAccess()?.createSerializationContext(NbtOps.INSTANCE) ?: NbtOps.INSTANCE
        stacks.forEach { stack ->
            if (stack.isEmpty) {
                list.add(CompoundTag())
            } else {
                try {
                    list.add(ItemStack.CODEC.encode(stack, ops, CompoundTag()).orThrow)
                } catch (_: Exception) {
                    list.add(CompoundTag())
                }
            }
        }
        val tag = CompoundTag()
        tag.put(INVENTORY_KEY, list)
        val baos = ByteArrayOutputStream()
        NbtIo.writeCompressed(tag, baos)
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    companion object {
        private const val INVENTORY_KEY = "INVENTORY"

        fun deserialize(encoded: String): VirtualInventory? {
            return try {
                val bytes = Base64.getDecoder().decode(encoded)
                val tag = NbtIo.readCompressed(ByteArrayInputStream(bytes), NbtAccounter.unlimitedHeap())
                val list = tag.getList(INVENTORY_KEY).orElse(null) ?: return null
                val ops = NoammAddons.mc.level?.registryAccess()?.createSerializationContext(NbtOps.INSTANCE) ?: NbtOps.INSTANCE
                val items = list.map { element ->
                    val compound = element as CompoundTag
                    if (compound.isEmpty) {
                        ItemStack.EMPTY
                    } else {
                        try {
                            ItemStack.CODEC.parse(ops, compound).orThrow
                        } catch (_: Exception) {
                            ItemStack.EMPTY
                        }
                    }
                }
                if (items.isEmpty()) null else VirtualInventory(items)
            } catch (_: Exception) {
                null
            }
        }
    }
}
