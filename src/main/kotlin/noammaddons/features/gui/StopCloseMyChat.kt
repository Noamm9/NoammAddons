package noammaddons.features.gui

import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiScreen
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiCloseEvent
import noammaddons.events.PacketEvent
import noammaddons.events.WorldLoadPostEvent
import noammaddons.features.Feature
import noammaddons.mixins.AccessorGuiChat
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.GuiUtils.openScreen

@Suppress("NAME_SHADOWING")
object StopCloseMyChat: Feature() {
    private var gui: String? = null

    @SubscribeEvent
    fun onEvent(event: Event) {
        if (! config.StopCloseMyChat) return

        try {
            when (event::class) {
                WorldEvent.Unload::class -> gui = getChatInput(mc.currentScreen)

                PacketEvent.Received::class -> {
                    val event = event as PacketEvent.Received
                    val chat = mc.currentScreen
                    if (chat !is GuiChat) return

                    when (event.packet) {
                        is S2EPacketCloseWindow -> event.isCanceled = true
                        is S2DPacketOpenWindow -> gui = if (gui == null) getChatInput(chat) else gui
                    }
                }

                GuiCloseEvent::class -> {
                    val event = event as GuiCloseEvent

                    if (gui == null) return
                    if (event.newGui != null) return
                    if (event.closedGui == null) return

                    openScreen(GuiChat(gui))
                    gui = null
                }

                WorldLoadPostEvent::class -> {
                    if (gui == null) return

                    openScreen(GuiChat(gui))
                    gui = null
                }
            }
        }
        catch (e: Exception) {
            debugMessage("StopCloseMyChat Error: ${e.message}")
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun getChatInput(gui: GuiScreen?): String? {
        val gui = gui as? AccessorGuiChat ?: return null
        val input = gui.inputField.text

        debugMessage("&bInput: $input")
        if (input.isBlank() || input == "/") return null
        return input
    }
}
