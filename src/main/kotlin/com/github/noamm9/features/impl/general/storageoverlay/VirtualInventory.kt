package com.github.noamm9.features.impl.general.storageoverlay

import net.minecraft.nbt.*
import net.minecraft.world.item.ItemStack
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.jvm.optionals.getOrElse

internal data class VirtualInventory(val stacks: List<ItemStack>) {
    val rows = stacks.size / 9

    fun encode(): String {
        val list = ListTag()
        val ops = NbtOps.INSTANCE

        stacks.forEach {
            list.add(
                if (it.isEmpty) CompoundTag()
                else ItemStack.CODEC.encode(it, ops, CompoundTag()).result().getOrElse { CompoundTag() }
            )
        }

        return ByteArrayOutputStream().use {
            val tag = CompoundTag().apply { put("i", list) }
            NbtIo.writeCompressed(tag, it)
            Base64.encode(it.toByteArray())
        }
    }

    companion object {
        fun decode(encoded: String): VirtualInventory? = runCatching {
            val bytes = Base64.decode(encoded)

            ByteArrayInputStream(bytes).use { bais ->
                val tag = NbtIo.readCompressed(bais, NbtAccounter.unlimitedHeap())
                val list = tag.getList("i").orElseThrow()
                val ops = NbtOps.INSTANCE

                val items = list.map { element ->
                    val compound = element as CompoundTag
                    if (compound.isEmpty) ItemStack.EMPTY
                    else ItemStack.CODEC.parse(ops, compound).result().getOrElse { ItemStack.EMPTY }
                }

                if (items.isEmpty()) null else VirtualInventory(items)
            }
        }.getOrNull()
    }
}