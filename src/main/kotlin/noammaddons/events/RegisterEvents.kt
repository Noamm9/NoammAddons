package noammaddons.events

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.server.*
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.Utils.isNull


object RegisterEvents {

    /**
     * Posts an event to the event bus and catches any errors.
     * @author Skytils
     */
    private fun Event.postCatch(): Boolean {
        return runCatching {
            MinecraftForge.EVENT_BUS.post(this)
        }.onFailure {
            it.printStackTrace()
            println("An error occurred $it")
            modMessage("Caught and logged an ${it::class.simpleName ?: "error"} at ${this::class.simpleName}. Please report this!")
        }.getOrDefault(isCanceled)
    }

    @JvmStatic
    fun postAndCatch(event: Event): Boolean = event.postCatch()

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (mc.theWorld.isNull() || mc.thePlayer.isNull()) return
        postAndCatch(Tick())
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderOverlay(event: RenderGameOverlayEvent.Text) {
        if (mc.renderManager?.fontRenderer == null) return

        GlStateManager.pushMatrix()
        GlStateManager.translate(0f, 0f, - 3f)
        postAndCatch(RenderOverlay())
        GlStateManager.translate(0f, 0f, 3f)
        GlStateManager.popMatrix()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderWorld(event: RenderWorldLastEvent) {
        GlStateManager.pushMatrix()
        postAndCatch(RenderWorld(event.partialTicks))
        GlStateManager.popMatrix()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    fun onChatMessage(event: PacketEvent.Received) {
        when (val packet = event.packet) {
            is S02PacketChat -> when (packet.type.toInt()) {
                in 0 .. 1 -> event.isCanceled = postAndCatch(Chat(packet.chatComponent))
                2 -> event.isCanceled = postAndCatch(Actionbar(packet.chatComponent))
            }

            is S32PacketConfirmTransaction -> {
                if (packet.func_148888_e()) return
                if (packet.actionNumber > 0) return

                postAndCatch(ServerTick())
            }

            is S23PacketBlockChange -> postAndCatch(BlockChangeEvent(packet.blockPosition, packet.blockState))
            is S22PacketMultiBlockChange -> packet.changedBlocks.forEach {
                postAndCatch(BlockChangeEvent(it.pos, it.blockState))
            }
        }
    }
}