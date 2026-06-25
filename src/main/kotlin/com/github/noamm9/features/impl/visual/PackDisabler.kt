package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.init.NetworkLoop
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.GsonUtils
import com.github.noamm9.utils.Utils.send
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.google.gson.JsonElement
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.mojang.serialization.JsonOps
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items


object PackDisabler: Feature("Removes the shitty hypixel skyblock texturepack") {
    private val ignoreSbPack by ToggleSetting("Ignore Pack Download")
    private val revertAxes by ToggleSetting("Revert Axes").withDescription("Turns certain swords back into an axe")

    private val replaceableItems = hashMapOf(
        Pair("RAGNAROCK_AXE", Items.GOLDEN_AXE),
        Pair("DAEDALUS_AXE", Items.GOLDEN_AXE),
        Pair("STARRED_DAEDALUS_AXE", Items.GOLDEN_AXE),
        Pair("AXE_OF_THE_SHREDDED", Items.DIAMOND_AXE)
    )

    override fun init() {
        EventBus.register<MainThreadPacketReceivedEvent.Pre> {
            if (! ignoreSbPack.value) return@register
            val p = event.packet as? ClientboundResourcePackPushPacket ?: return@register
            ServerboundResourcePackPacket(p.id(), ServerboundResourcePackPacket.Action.ACCEPTED).send()
            ServerboundResourcePackPacket(p.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED).send()
            event.isCanceled = true
        }
    }

    fun toJson(itemStack: ItemStack?, registryAccess: HolderLookup.Provider): String {
        if (itemStack == null || itemStack.isEmpty) return "{}"
        val ops = RegistryOps.create<JsonElement>(JsonOps.INSTANCE, registryAccess)
        val jsonElement = DataComponentPatch.CODEC.encodeStart(ops, itemStack.componentsPatch).result().get()
        return GsonUtils.gson.toJson(jsonElement)
    }

    @JvmStatic
    fun appendItemLayersHook(stack: ItemStack, key: DataComponentType<*>, original: Operation<Identifier>): Identifier {
        val currentModel = original.call(stack, key)
        if (! enabled) return currentModel
        if (stack.isEmpty) return currentModel
        val skyblockID = stack.skyblockId.takeUnless(String::isEmpty) ?: return currentModel

        if (revertAxes.value && skyblockID in replaceableItems.keys) {
            val replace = replaceableItems[skyblockID] ?: return currentModel
            return replace.components().get(DataComponents.ITEM_MODEL) !!
        }

        if (! currentModel.toString().contains("hypixel_skyblock")) return currentModel
        val oldModel = NetworkLoop.idToLocation[stack.skyblockId]
        return oldModel ?: currentModel
    }
}