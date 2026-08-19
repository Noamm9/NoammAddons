package com.github.noamm9.features.impl.general.teleport

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.PlayerUtils.serverPitch
import com.github.noamm9.utils.PlayerUtils.serverYaw
import com.github.noamm9.utils.items.EtherwarpHelper
import com.github.noamm9.utils.items.TeleportUtils
import com.github.noamm9.utils.render.Render3D.renderBlock
import com.github.noamm9.utils.render.Render3D.renderBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.sounds.SoundEvents
import org.lwjgl.glfw.GLFW
import java.awt.Color

object Etherwarp: Feature("Etherwarp overlay, sound, and left-click activation.") {
    private val overlay by ToggleSetting("Etherwarp Overlay").section("Overlay")
    private val mode by DropdownSetting("Mode", 0, listOf("Outline", "Fill", "Filled Outline")).showIf { overlay.value }
    private val phase by ToggleSetting("Phase").showIf { overlay.value }
    private val lineWidth by SliderSetting("Line Width", 1.0, 1.0, 10.0, 0.1).showIf { overlay.value && mode.value != 1 }
    private val showFail by ToggleSetting("Show Fail", true).withDescription("Shows the fail position of the etherwarp").showIf { overlay.value }
    private val fullBlock by ToggleSetting("Full Block").withDescription("Draws the overlay as a full block").showIf { overlay.value }

    private val fillColor by ColorSetting("Fill Color", Utils.favoriteColor.withAlpha(50)).showIf { overlay.value && mode.value != 0 }.section("Colors")
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor, false).showIf { overlay.value && mode.value != 1 }

    private val invalidFillColor by ColorSetting("Invalid Fill Color ", Color.RED.withAlpha(50)).showIf { overlay.value && mode.value != 0 && showFail.value }
    private val invalidOutlineColor by ColorSetting("Invalid Outline Color ", Color.RED, false).showIf { overlay.value && mode.value != 1 && showFail.value }

    private val etherwarpSound by ToggleSetting("Etherwarp Sound").section("Sound")
    private val zeroPingSound by ToggleSetting("Zero-Ping Sound").withDescription("Plays the Etherwarp sound client-side instead of waiting for the server to send the sound packet").showIf { etherwarpSound.value }
    private val playSound = createSoundSettings("Sound", SoundEvents.EXPERIENCE_ORB_PICKUP) { etherwarpSound.value }

    private val leftClick by ToggleSetting("Left-Click Etherwarp").section("Left Click")
    private val swingHandToggle by ToggleSetting("Swing Hand", true).showIf { leftClick.value }

    //#if CHEAT
    private val autoSneak by ToggleSetting("Auto Sneak", false).showIf { leftClick.value }
    private val autoSneakDelay by SliderSetting("Auto Sneak Delay", 50, 50, 150, 1).showIf { leftClick.value && autoSneak.value }
    //#endif

    override fun init() {
        register<RenderWorldEvent> {
            if (! overlay.value) return@register
            if (! mc.options.keyShift.isDown) return@register
            val heldItem = player.mainHandItem.takeUnless { it.isEmpty } ?: return@register
            val distance = EtherwarpHelper.getEtherwarpDistance(heldItem) ?: return@register
            val (valid, pos) = EtherwarpHelper.getEtherPos(player.position(), player.lookAngle, distance)
            if (! valid && ! showFail.value) return@register
            pos ?: return@register

            if (fullBlock.value) event.ctx.renderBox(pos.x + 0.5,
                pos.y, pos.z + 0.5, 1.0001, 1.0001, if (valid) outlineColor.value else invalidOutlineColor.value,
                if (valid) fillColor.value else invalidFillColor.value,
                mode.value.equalsOneOf(0, 2),
                mode.value.equalsOneOf(1, 2),
                phase.value,
                lineWidth.value
            )
            else event.ctx.renderBlock(pos, if (valid) outlineColor.value else invalidOutlineColor.value,
                if (valid) fillColor.value else invalidFillColor.value,
                mode.value.equalsOneOf(0, 2),
                mode.value.equalsOneOf(1, 2),
                phase = phase.value,
                lineWidth.value.toFloat()
            )
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! etherwarpSound.value) return@register
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            if (packet.sound.value() != SoundEvents.ENDER_DRAGON_HURT) return@register
            if (packet.pitch != 0.53968257f) return@register
            event.isCanceled = true

            if (! zeroPingSound.value) playSound.action.invoke()
        }

        register<PacketEvent.Sent> {
            if (! etherwarpSound.value || ! zeroPingSound.value) return@register
            val packet = event.packet as? ServerboundUseItemOnPacket ?: return@register
            if (! mc.options.keyShift.isDown) return@register
            if (WorldUtils.getBlockAt(packet.hitResult.blockPos) !in TeleportUtils.TILLABLE_BLOCKS) return@register
            val dist = EtherwarpHelper.getEtherwarpDistance(player.mainHandItem) ?: return@register
            val (succeeded, pos) = EtherwarpHelper.getEtherPos(player.position(), player.lookAngle, dist)
            if (! succeeded || pos == null) return@register
            if (TeleportUtils.canTeleport(player.serverYaw, player.serverPitch)) playSound.action.invoke()
        }

        register<PacketEvent.Sent> {
            if (! etherwarpSound.value || ! zeroPingSound.value) return@register
            val packet = event.packet as? ServerboundUseItemPacket ?: return@register
            if (! mc.options.keyShift.isDown) return@register
            val dist = EtherwarpHelper.getEtherwarpDistance(player.mainHandItem) ?: return@register
            val (succeeded, pos) = EtherwarpHelper.getEtherPos(player.position(), player.lookAngle, dist)
            if (! succeeded || pos == null) return@register
            if (TeleportUtils.canTeleport(packet.yRot, packet.xRot)) playSound.action.invoke()
        }

        register<MouseClickEvent> {
            if (! leftClick.value) return@register
            if (event.button != 0) return@register
            if (event.action != GLFW.GLFW_PRESS) return@register
            if (mc.screen != null) return@register
            //#if CHEAT
            if (! mc.options.keyShift.isDown && ! autoSneak.value) return@register
            //#else
            //$if (! mc.options.keyShift.isDown) return@register
            //#endif
            if (EtherwarpHelper.getEtherwarpDistance(player.mainHandItem) == null) return@register

            event.isCanceled = true

            //#if CHEAT
            if (! player.isCrouching && autoSneak.value) scope.launch {
                val wait = autoSneakDelay.value.toLong() / 2
                PlayerUtils.toggleSneak(true)
                delay(wait)

                PlayerUtils.rightClick()
                if (swingHandToggle.value) PlayerUtils.swingArm()

                delay(wait)
                PlayerUtils.toggleSneak(false)
            }
            else {
                PlayerUtils.rightClick()
                if (swingHandToggle.value) PlayerUtils.swingArm()
            }
            //#else
            //$PlayerUtils.rightClick()
            //$if (swingHandToggle.value) PlayerUtils.swingArm()
            //#endif
        }
    }
}