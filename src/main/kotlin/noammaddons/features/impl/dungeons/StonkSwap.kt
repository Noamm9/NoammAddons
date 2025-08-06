package noammaddons.features.impl.dungeons

import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.PacketEvent
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.ServerPlayer

object StonkSwap: Feature("A bunch of qol features for stonk swapping") {
    // If the item index you are holding is different from the item the index the server thinks you are holding, then you stonk swapped.
    // Also, don't make a sound if you swapped from a bow because it does not work with bows for some reason work.

    private val stonkSwapSoundName = TextInputSetting("Sound Name", "random.orb")
    private val stonkSwapVolume = SliderSetting("Volume", 0f, 1f, 0.1f, 0.5f)
    private val stonkSwapPitch = SliderSetting("Pitch", 0f, 2f, 0.1f, 1f)
    private val playSound = ButtonSetting("Play Sound") {
        repeat(5) {
            mc.thePlayer?.playSound(
                stonkSwapSoundName.value,
                stonkSwapVolume.value,
                stonkSwapPitch.value
            )
        }
    }

    override fun init() = addSettings(stonkSwapSoundName, stonkSwapVolume, stonkSwapPitch, playSound)

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Sent) {
        val packet = event.packet as? C07PacketPlayerDigging ?: return
        if (packet.status != C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) return
        if (ServerPlayer.player.heldHotbarSlot == mc.thePlayer.inventory.currentItem) return
        if (mc.thePlayer.heldItem?.item !is ItemTool) return
        playSound.invoke()
    }
}
