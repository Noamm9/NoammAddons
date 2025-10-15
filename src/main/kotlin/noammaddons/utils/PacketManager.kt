package noammaddons.utils

import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraftforge.fml.common.eventhandler.*
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.PacketEvent
import noammaddons.events.ServerTick
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting


object PacketManager {
    private val termRegex = Regex("^(Click in order!|Select all the (.+?) items!|What starts with: '(.+?)'\\?|Change all to same color!|Correct all the panes!|Click the button on time!)\$")
    private var inTerm = false
    private var currentWindowId: Int? = null
    private var windowOpenTime: Long? = null
    private var windowTitle: String? = null
    private var lastInteract = 0

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onWindowOpen(event: PacketEvent.Received) {
        val packet = event.packet as? S2DPacketOpenWindow ?: return
        windowTitle = packet.windowTitle.noFormatText
        inTerm = windowTitle?.matches(termRegex) ?: false
        windowOpenTime = if (inTerm && windowOpenTime != null) windowOpenTime else System.currentTimeMillis()
        currentWindowId = packet.windowId
        if (windowTitle == "Click the button on time!") lastInteract = 0
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onWindowCloseReceived(event: PacketEvent.Received) {
        if (event.packet !is S2EPacketCloseWindow) return
        currentWindowId = null
        windowOpenTime = null
        windowTitle = null
        inTerm = false
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onWindowCloseSent(event: PacketEvent.Sent) {
        if (event.packet !is C0DPacketCloseWindow) return
        currentWindowId = null
        windowOpenTime = null
        windowTitle = null
        inTerm = false
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onWindowClick(event: PacketEvent.Sent) {
        val packet = event.packet as? C0EPacketClickWindow ?: return
        if (packet.windowId == 0) return // your player inventory
        if (packet.windowId != currentWindowId) {
            if (currentWindowId != null) return cancel(
                event, "Canceled click due to window ID mismatch (Click: ${packet.windowId}, Expected: $currentWindowId)"
            )
        }

        val openTime = windowOpenTime ?: return
        val title = windowTitle ?: return
        if (System.currentTimeMillis() - openTime < 350L && inTerm && title != "Click the button on time!") {
            cancel(event, "Canceled a click that was too fast (${System.currentTimeMillis() - openTime}ms)")
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onInteract(event: PacketEvent.Sent) {
        if (event.packet !is C02PacketUseEntity) return
        val entity = event.packet.getEntityFromWorld(mc.theWorld) ?: return
        if (entity.name.removeFormatting() != "Inactive Terminal") return
        if (lastInteract > 0 || currentWindowId != null)
            cancel(event, "Cenceled UseEntity. either in gui or using entity too fast")
        else lastInteract = 15
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onServerTick(event: ServerTick) {
        if (lastInteract == 0) return
        lastInteract --
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onSlotChange(event: PacketEvent.Sent) {
        val packet = event.packet as? C09PacketHeldItemChange ?: return
        if (packet.slotId < 0 || packet.slotId >= InventoryPlayer.getHotbarSize())
            cancel(event, "Canceled a HeldItemChange that was out of hotbar. (slot: ${packet.slotId})")
    }

    private fun cancel(event: Event, msg: String) {
        debugMessage("&6PacketManager:&r &c$msg&r")
        event.isCanceled = true
    }

    fun getTermOpenTime() = windowOpenTime ?: throw IllegalStateException("Tried to get term open time while not in a term? This should not be possible")
}