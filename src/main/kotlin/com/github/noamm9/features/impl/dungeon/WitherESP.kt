package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.CheckEntityGlowEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ColorSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.boss.wither.WitherBoss
import java.awt.Color

/*
     The wither from the armor set: isPowered: false, invulnerableTicks: 800
     Maxor: isPowered: true, invulnerableTicks: 200
     Storm: isPowered: true, invulnerableTicks: 1
     Goldor: isPowered: true, invulnerableTicks: 0
     Necron: isPowered: true, invulnerableTicks: 1
     Vanquisher: isPowered: false/true, invulnerableTicks: 250
*/
object WitherESP: Feature("Highlights all Withers in F7") {
    private val color by ColorSetting("Hightlight Color", Color.WHITE, false)

    var currentWither: WitherBoss? = null

    private fun isValidLoc(): Boolean {
        return LocationUtils.dungeonFloorNumber == 7 && LocationUtils.inBoss && LocationUtils.F7Phase != 5
    }

    override fun init() {
        register<WorldChangeEvent> { currentWither = null }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! isValidLoc()) return@register
            when (val packet = event.packet) {
                is ClientboundSetEntityDataPacket -> {
                    val entity = mc.level?.getEntity(packet.id) as? WitherBoss ?: return@register
                    if (entity.isInvisible || entity.invulnerableTicks == 800) return@register
                    currentWither = entity
                }

                is ClientboundRemoveEntitiesPacket -> {
                    packet.entityIds.find { currentWither?.id == it }?.let {
                        currentWither = null
                    }
                }
            }
        }

        register<CheckEntityGlowEvent> {
            if (! isValidLoc()) return@register
            if (event.entity != currentWither) return@register
            event.color = color.value
        }
    }
}