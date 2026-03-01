package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.*
import com.github.noamm9.ui.hud.getValue
import com.github.noamm9.ui.hud.provideDelegate
import com.github.noamm9.utils.ActionBarParser
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.dungeons.enums.SecretType
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.network.PacketUtils.send
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.github.noamm9.utils.render.Render3D
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.sounds.SoundEvents
import java.util.concurrent.CopyOnWriteArraySet

object Secrets: Feature() {
    private val hudDisplay by ToggleSetting("Secret HUD", true)
        .withDescription("Displays the current room's secrets on screen.")
        .section("HUD")

    private val closeChest by ToggleSetting("Close Chest").section("Auto")
        .withDescription("Automatically closes the secret chest for you.")

    private val secretClicked by ToggleSetting("Highlight Clicked Secret")
        .withDescription("Highlights the block of a secret when you interact with it.")
        .section("Secret Clicked")

    private val displayTime by SliderSetting("Highlight Time", 2.0, 0.5, 5.0, 0.1)
        .withDescription("How long (in seconds) the highlight box remains visible.")
        .showIf { secretClicked.value }

    private val secretClickedColor by ColorSetting("Highlight Color", Utils.favoriteColor.withAlpha(50))
        .withDescription("The color of the secret highlight box.")
        .showIf { secretClicked.value }

    private val mode by DropdownSetting("Render Mode", 2, listOf("Fill", "Outline", "Filled Outline"))
        .withDescription("Choose how the box is rendered.")
        .showIf { secretClicked.value }

    private val phase by ToggleSetting("See Through Walls")
        .withDescription("If enabled, the highlight will be visible through other blocks.")
        .showIf { secretClicked.value }

    private val secretSound by ToggleSetting("Secret Sound")
        .withDescription("Plays a sound effect when a secret is clicked/found.")
        .section("Secret Sound")

    private val sound by SoundSetting("Sound", SoundEvents.EXPERIENCE_ORB_PICKUP)
        .withDescription("The internal Minecraft sound key to play.")
        .showIf { secretSound.value }

    private val volume by SliderSetting("Volume", 0.5f, 0f, 1f, 0.1f)
        .withDescription("The loudness of the sound.")
        .showIf { secretSound.value }

    private val pitch by SliderSetting("Pitch", 1f, 0f, 2f, 0.1f)
        .withDescription("The pitch/frequency of the sound.")
        .showIf { secretSound.value }

    private val playSound by ButtonSetting("Test Sound", false) {
        repeat(5) { mc.soundManager.play(SimpleSoundInstance.forUI(sound.value, pitch.value, volume.value)) }
    }.withDescription("Click to test the current sound configuration.").showIf { secretSound.value }

    private val secretHud by hudElement("Secret Hud", { hudDisplay.value }, { LocationUtils.inDungeon && ! LocationUtils.inBoss }) { ctx, example ->
        val line = if (example) "&7Secrets: &c3&7/&a7"
        else {
            val max = ActionBarParser.maxSecrets ?: return@hudElement 0f to 0f
            val current = ActionBarParser.secrets ?: return@hudElement 0f to 0f
            "&7Secrets: ${ColorUtils.colorCodeByPercent(current, max)}$current&7/&a$max"
        }

        Render2D.drawString(ctx, line, 0, 0)
        return@hudElement line.width().toFloat() to 9f
    }

    private data class ClickedSecret(val pos: BlockPos, val time: Long)

    private val clicked = CopyOnWriteArraySet<ClickedSecret>()
    private var lastPlayed = System.currentTimeMillis()

    override fun init() {
        register<MainThreadPacketReceivedEvent.Pre> {
            if (! closeChest.value) return@register
            if (! LocationUtils.inDungeon) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (! packet.title.unformattedText.equalsOneOf("Chest", "Large Chest")) return@register
            ServerboundContainerClosePacket(packet.containerId).send()
            event.isCanceled = true
        }

        register<RenderWorldEvent> {
            if (clicked.isEmpty()) return@register
            clicked.removeIf { it.time + (displayTime.value * 1000) < System.currentTimeMillis() }
            clicked.takeUnless { it.isEmpty() }?.forEach {
                Render3D.renderBlock(
                    event.ctx, it.pos,
                    secretClickedColor.value,
                    outline = mode.value.equalsOneOf(1, 2),
                    fill = mode.value.equalsOneOf(0, 2),
                    phase = phase.value
                )
            }
        }

        register<DungeonEvent.SecretEvent> {
            if (secretSound.value) {
                if (event.type == SecretType.ITEM && System.currentTimeMillis() - lastPlayed < 2000) return@register
                if (event.type == SecretType.CHEST) lastPlayed = System.currentTimeMillis()
                if (clicked.any { it.pos == event.pos }) return@register
                playSound.action.invoke()
            }

            if (secretClicked.value) {
                if (clicked.any { it.pos == event.pos }) return@register
                clicked.add(ClickedSecret(event.pos, System.currentTimeMillis()))
            }
        }
    }
}