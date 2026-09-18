package com.github.noamm9.features.impl.floor7

import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.TextInputSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.location.LocationUtils
import gg.essential.universal.UMinecraft
import net.minecraft.network.protocol.game.*
import net.minecraft.world.item.Items

object MelodyAlert: Feature() {
    private val msg by TextInputSetting("Start Message", "I ❤ Melody").jsonName("Melody Message").withDescription("Sent when the Melody terminal opens")
    private val msg1 by TextInputSetting("1/4 Message", "I ❤ Melody {progress}").withDescription("Sent at 1/4 progress. Supports {progress} and color codes")
    private val msg2 by TextInputSetting("2/4 Message", "I ❤ Melody {progress}").withDescription("Sent at 2/4 progress. Supports {progress} and color codes")
    private val msg3 by TextInputSetting("3/4 Message", "I ❤ Melody {progress}").withDescription("Sent at 3/4 progress. Supports {progress} and color codes")
    private val mode by DropdownSetting("Progress Mode", 0, listOf("1/4", "25%"))

    private val progressSlots = intArrayOf(25, 34, 43) // todo remove 43
    private var isMelodyOpen = false
    private var currentStage = - 1

    override fun init() {
        register<MainThreadPacketReceivedEvent.Post> {
            if (LocationUtils.F7Phase != 3) return@register
            val packet = event.packet as? ClientboundOpenScreenPacket ?: return@register
            if (packet.title.unformattedText != "Click the button on time!") return@register

            if (msg.value.isNotBlank()) ChatUtils.sendPartyMessage(msg.value)
            isMelodyOpen = true
            currentStage = - 1
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! isMelodyOpen) return@register
            if (event.packet !is ClientboundContainerClosePacket) return@register
            isMelodyOpen = false
            currentStage = - 1
        }

        register<PacketEvent.Sent> {
            if (! isMelodyOpen) return@register
            if (event.packet !is ServerboundContainerClosePacket) return@register
            isMelodyOpen = false
            currentStage = - 1
        }

        register<TickEvent.Start> {
            if (! isMelodyOpen) return@register
            if (UMinecraft.currentScreenObj == null) {
                isMelodyOpen = false
                return@register
            }

            if (currentStage == 3) return@register
            for (i in progressSlots.indices) {
                if (i <= currentStage) continue

                if (player.containerMenu.getSlot(progressSlots[i]).item.`is`(Items.LIME_TERRACOTTA)) {
                    //val progress = if (mode.value == 0) "${i + 1}/3" else "${(i + 1) * 33}%" todo
                    val progress = if (mode.value == 0) "${i + 1}/4" else "${(i + 1) * 25}%"
                    val text = listOf(msg1, msg2, msg3)[i].value.ifBlank { "${msg.value} $progress" }
                    if (text.isNotBlank()) ChatUtils.sendPartyMessage(text.replace("{progress}", progress))
                    currentStage = i
                }
            }
        }
    }
}