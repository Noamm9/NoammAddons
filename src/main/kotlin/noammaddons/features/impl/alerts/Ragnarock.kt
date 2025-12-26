package noammaddons.features.impl.alerts

import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.ItemUtils.skyblockID
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.ServerPlayer
import noammaddons.utils.SoundUtils
import noammaddons.utils.ThreadUtils.setTimeout
import kotlin.math.roundToInt

object Ragnarock: Feature("Provides alerts about Ragnarock") {
    private val alertCancelled by ToggleSetting("Alert Cancelled", true)
    private val strengthGainedMessage by ToggleSetting("Strength Gained", true)
    private val m7Alert by ToggleSetting("M7 Dragon Alert")
    private var ticks = 0

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Received) {
        if (! strengthGainedMessage) return
        val packet = event.packet as? S29PacketSoundEffect ?: return
        if (packet.soundName != "mob.wolf.howl" || packet.pitch != 1.4920635f) return
        val item = ServerPlayer.player.getHeldItem().takeIf { it.skyblockID == "RAGNAROCK_AXE" } ?: return
        val baseStrength = item.lore.find { it.removeFormatting().startsWith("Strength:") }?.let {
            Regex("Strength: \\+(\\d+)").find(it.removeFormatting())?.destructured?.component1()?.toIntOrNull()
        } ?: return
        modMessage("&fGained strength: &c${(baseStrength * 1.5).roundToInt()}")
    }


    @SubscribeEvent
    fun onChat(event: Chat) {
        if (m7Alert && F7Phase == 5 && event.component.noFormatText == "[BOSS] Wither King: You... again?")
            ticks = 36
        else if (alertCancelled && event.component.noFormatText.matches(Regex("Ragnarock was cancelled due to (?:being hit|taking damage)!"))) {
            showTitle(subtitle = "&cRagnarock Cancelled")
            SoundUtils.Pling()
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) {
        if (ticks == 0) return
        ticks --
        if (ticks == 0) {
            showTitle("rag", rainbow = true)
            repeat(5) { mc.thePlayer.playSound("note.harp", 50f, 1.22f) }
            setTimeout(120) { repeat(5) { mc.thePlayer.playSound("note.harp", 50f, 1.13f) } }
            setTimeout(240) { repeat(5) { mc.thePlayer.playSound("note.harp", 50f, 1.29f) } }
            setTimeout(400) { repeat(5) { mc.thePlayer.playSound("note.harp", 50f, 1.60f) } }
            setTimeout(520) { repeat(5) { mc.thePlayer.playSound("note.harp", 50f, 1.60f) } }
            setTimeout(640) { repeat(5) { mc.thePlayer.playSound("note.harp", 50f, 1.72f) } }
            setTimeout(780) { repeat(5) { mc.thePlayer.playSound("note.harp", 50f, 1.89f) } }
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        ticks = 0
    }
}
