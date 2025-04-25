package noammaddons.features.impl.dungeons

import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.DropdownSetting
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.PlayerUtils
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send

object CloseChest: Feature("Allows you to instantly close chests with any key or automatically.") {
    private val mode = DropdownSetting("Close Mode", arrayListOf("Auto", "Any Key"))
    override fun init() = addSettings(mode)

    @SubscribeEvent
    fun onOpenWindow(event: PacketEvent.Received) {
        val packet = event.packet as? S2DPacketOpenWindow ?: return
        if (mode.value != 0) return
        if (! inDungeon) return
        if (! packet.windowTitle.noFormatText.equalsOneOf("Chest", "Large Chest")) return
        C0DPacketCloseWindow(packet.windowId).send()
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onInput(event: GuiKeybourdInputEvent) {
        if (! inDungeon || mode.value != 1) return
        if (GuiUtils.currentChestName.removeFormatting().equalsOneOf("Chest", "Large Chest")) {
            PlayerUtils.closeScreen()
        }
    }

    @SubscribeEvent
    fun onMouse(event: GuiMouseClickEvent) {
        if (! inDungeon || mode.value != 1) return
        if (GuiUtils.currentChestName.removeFormatting().equalsOneOf("Chest", "Large Chest")) {
            PlayerUtils.closeScreen()
        }
    }
}
