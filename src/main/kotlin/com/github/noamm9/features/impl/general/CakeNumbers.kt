package com.github.noamm9.features.impl.general

import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.remove
import com.github.noamm9.utils.render.Render2D
import net.minecraft.world.item.Items

object CakeNumbers: Feature("Displays the year of the cake in the New Year Cake Bag.") {
    override fun init() {
        register<ContainerEvent.Render.Slot.Post> {
            if (! LocationUtils.inSkyblock) return@register
            if (! event.slot.item.`is`(Items.CAKE)) return@register
            val name = event.slot.item.hoverName.unformattedText
            if ("New Year Cake (Year " !in name) return@register
            val year = name.remove("New Year Cake (Year ", ")").trim()
            Render2D.drawCenteredString(event.context, "&b$year", event.slot.x + 8, event.slot.y + 8, scale = 0.8)
        }
    }
}