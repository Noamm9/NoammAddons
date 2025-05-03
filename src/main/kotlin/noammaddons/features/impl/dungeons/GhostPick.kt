package noammaddons.features.impl.dungeons

import net.minecraft.item.ItemStack
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.events.*
import noammaddons.events.ClickEvent.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.BlockUtils.blackList
import noammaddons.utils.BlockUtils.getBlockAt
import noammaddons.utils.BlockUtils.toAir
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.PlayerUtils.isHoldingTpItem
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.Utils.equalsOneOf


object GhostPick: Feature("Allows you to break blocks client-side.") {
    private val onlyInDungeon = ToggleSetting("Only in Dungeons", false)
    private val mode = DropdownSetting("Mode", arrayListOf("Legit", "Efficiency 100", "Ghost Blocks"))
    private val keybind = KeybindSetting("Keybind")
    override fun init() = addSettings(onlyInDungeon, mode, keybind)


    private object GhostPickElement: GuiElement(hudData.getData().GhostPick) {
        private const val text = "&b&lGhostPick: &a&lEnabled"
        override val enabled get() = featureState
        override val width = getStringWidth(text)
        override val height = 9f
        override fun draw() = drawText(text, getX(), getY(), getScale())
    }

    private fun isAllowedTool(itemStack: ItemStack) = itemStack.item is ItemTool

    private var featureState = false

    override fun onDisable() {
        super.onDisable()
        featureState = false
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Sent) {
        if (! featureState) return
        val heldItem = mc.thePlayer?.heldItem ?: return
        if (event.packet !is C07PacketPlayerDigging) return
        if (event.packet.status.equalsOneOf( // @formatter:off
            C07PacketPlayerDigging.Action.DROP_ALL_ITEMS,
            C07PacketPlayerDigging.Action.DROP_ITEM,
            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
        )) return

        if (mode.value == 0) {
            // disable on bow, tnt, blaze rod
            if (heldItem.getItemId().equalsOneOf(261, 46, 369)) return
            event.isCanceled = true
        }
        else if (mode.value == 1) {
            if (! isAllowedTool(heldItem)) return
            if (isHoldingTpItem()) return
            val blockPos = mc.objectMouseOver?.blockPos
            toAir(blockPos)
        }
    }

    @SubscribeEvent
    fun onTick(event: Tick) {
        if (onlyInDungeon.value) return
        if (mc.currentScreen != null) return
        if (!inDungeon) featureState = false
        if (! keybind.isPressed()) return
        featureState = ! featureState
    }


    @SubscribeEvent
    fun ghostBlocks(event: RightClickEvent) {
        if (! featureState) return
        if (mode.value != 2) return
        if (! mc.gameSettings.keyBindUseItem.isKeyDown) return
        if (! isAllowedTool(mc.thePlayer?.heldItem ?: return)) return
        mc.thePlayer.rayTrace(100.0, .0f)?.blockPos?.run {
            if (getBlockAt(this) in blackList) return@run
            event.isCanceled = true
            toAir(this)
        }
    }

    @SubscribeEvent
    fun draw(event: RenderOverlay) {
        if (!GhostPickElement.enabled) return
        GhostPickElement.draw()
    }
}
