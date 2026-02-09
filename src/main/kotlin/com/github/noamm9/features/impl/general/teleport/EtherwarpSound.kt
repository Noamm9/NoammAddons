package com.github.noamm9.features.impl.general.teleport

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ButtonSetting
import com.github.noamm9.ui.clickgui.componnents.impl.SliderSetting
import com.github.noamm9.ui.clickgui.componnents.impl.SoundSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.withDescription
import com.github.noamm9.utils.MathUtils.toVec
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.map.utils.ScanUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.world.WorldUtils
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.block.Blocks

object EtherwarpSound: Feature() {
    private val zeroPingSound by ToggleSetting("Zero-Ping Sound").withDescription("plays the Etherwarp Sound Client-side instead of waiting for the server to send the sound packet")
    private val sound by SoundSetting("Sound", SoundEvents.EXPERIENCE_ORB_PICKUP).withDescription("The internal Minecraft sound key to play.")
    private val volume by SliderSetting("Volume", 0.5f, 0f, 1f, 0.1f).withDescription("The loudness of the sound.")
    private val pitch by SliderSetting("Pitch", 1f, 0f, 2f, 0.1f).withDescription("The pitch/frequency of the sound.")
    private val playSound by ButtonSetting("Play Sound", false) {
        repeat(5) { mc.soundManager?.play(SimpleSoundInstance.forUI(sound.value, pitch.value, volume.value)) }
    }.withDescription("Click to play sound.")

    private val interactable = listOf(
        Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.HOPPER,
        Blocks.CAULDRON, Blocks.LEVER, Blocks.STONE_BUTTON, Blocks.OAK_BUTTON,
        Blocks.OAK_TRAPDOOR, Blocks.IRON_TRAPDOOR
    )

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            if (packet.sound.value() != SoundEvents.ENDER_DRAGON_HURT) return@register
            if (packet.pitch != 0.53968257f) return@register
            event.isCanceled = true

            if (! zeroPingSound.value) playSound.action.invoke()
        }

        register<PacketEvent.Sent> {
            if (! zeroPingSound.value) return@register
            if (event.packet !is ServerboundUseItemPacket) return@register
            val player = mc.player ?: return@register
            if (! mc.options.keyShift.isDown) return@register
            if (LocationUtils.F7Phase == 3 && LocationUtils.inBoss) return@register
            if (ScanUtils.currentRoom?.data?.name.equalsOneOf("New Trap", "Old Trap", "Teleport Maze", "Boulder")) return@register
            PlayerUtils.getSelectionBlock()?.let { if (WorldUtils.getBlockAt(it) in interactable) return@register }
            val dist = EtherwarpHelper.getEtherwarpDistance(player.mainHandItem) ?: return@register

            val (succeeded, pos) = EtherwarpHelper.getEtherPos(player.position(), dist)
            if (! succeeded || pos == null) return@register

            if (ScanUtils.getRoomFromPos(pos.toVec())?.data?.name.equalsOneOf("Teleport Maze", "Boulder")) return@register

            playSound.action.invoke()
        }
    }
}