package com.github.noamm9.features.impl.floor7

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.TextInputSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.world.item.Items

object MelodyAlert: Feature() {
    private val msg by TextInputSetting("Melody Message", "I ❤ Melody")
    private val mode by DropdownSetting("Progress Mode", 0, listOf("1/4", "25%"))

    private val progressSlots = intArrayOf(25, 34, 43)
    private var isMelodyOpen = false
    private var currentStage = - 1

    override fun init() {
        register<ContainerEvent.Open> {
            if (msg.value.isBlank()) return@register
            if (LocationUtils.F7Phase != 3) return@register

            isMelodyOpen = false
            currentStage = - 1

            ThreadUtils.scheduledTask(2) {
                if (event.screen.title.unformattedText == "Click the button on time!") {
                    isMelodyOpen = true
                    ChatUtils.sendPartyMessage(msg.value)
                }
            }
        }

        register<TickEvent.Start> {
            if (! isMelodyOpen) return@register
            if (mc.screen == null) {
                isMelodyOpen = false
                return@register
            }

            if (currentStage == 3) return@register
            for (i in progressSlots.indices) {
                if (i <= currentStage) continue

                if (mc.player !!.containerMenu.getSlot(progressSlots[i]).item.`is`(Items.LIME_TERRACOTTA)) {
                    val progress = if (mode.value == 0) "${i + 1}/4" else "${(i + 1) * 25}%"
                    ChatUtils.sendPartyMessage("${msg.value} $progress")
                    currentStage = i
                }
            }
        }
    }
}