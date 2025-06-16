package noammaddons

import net.minecraft.client.gui.GuiDownloadTerrain
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.AttackEntityEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noammaddons.events.*
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.DungeonUtils
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils2D.modelViewMatrix
import noammaddons.utils.RenderUtils2D.projectionMatrix
import noammaddons.utils.RenderUtils2D.viewportDims
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.send
import org.lwjgl.opengl.GL11.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3


// used to be a place for me to test shit.
// but now it's just a dump of silent features
object TestGround {
    private var fuckingBitch = false
    private var sent = false
    private var a = false

    @SubscribeEvent
    fun handlePartyCommands(event: MessageSentEvent) {
        if (! onHypixel) return
        val msg = event.message.removeFormatting().lowercase()

        if (msg == "/p invite accept") {
            event.isCanceled = true
            a = true
            sent = false
            setTimeout(250) { a = false }
            return
        }

        if (a && ! sent) {
            if (msg.startsWith("/p invite ") || msg.startsWith("/party accept ")) {
                event.isCanceled = true

                val modifiedMessage = msg
                    .replace("/party accept ", "/p join ")
                    .replace("/p invite ", "/p join ")

                sendChatMessage(modifiedMessage)
                sent = true
            }
        }

        if (msg.equalsOneOf("/pll", "/pl")) {
            event.isCanceled = true
            C01PacketChatMessage("/pl").send()
        }
    }

    @SubscribeEvent
    fun wtf(event: WorldLoadPostEvent) {
        setTimeout(500) {
            if (mc.currentScreen !is GuiDownloadTerrain) return@setTimeout
            mc.currentScreen = null
        }
    }

    @SubscribeEvent
    fun fucklocraw(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        if (event.isLocal) return
        fuckingBitch = true
        setTimeout(5000) { fuckingBitch = false }
    }

    @SubscribeEvent
    fun fuckLocraw2(event: PacketEvent.Sent) {
        if (! fuckingBitch) return
        val packet = event.packet as? C01PacketChatMessage ?: return
        if (packet.message != "/locraw") return
        debugMessage("Cancelling /locraw")
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onPlaterInteract(e: AttackEntityEvent) {
        if (! inDungeon) return
        if (e.entityPlayer != mc.thePlayer) return
        if (DungeonUtils.dungeonTeammates.none { it.entity == e.target }) return
        e.isCanceled = true
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRenderWorld(event: RenderWorldLastEvent) {
        val (x, y, z) = mc.thePlayer?.renderVec?.destructured() ?: return
        GlStateManager.pushMatrix()
        GlStateManager.translate(- x, - y, - z)

        glGetFloat(GL_MODELVIEW_MATRIX, modelViewMatrix)
        glGetFloat(GL_PROJECTION_MATRIX, projectionMatrix)

        GlStateManager.popMatrix()
        glGetInteger(GL_VIEWPORT, viewportDims)
    }
}