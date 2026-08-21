package com.github.noamm9.features.impl.floor7

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.event.impl.BossBarUpdateEvent
import com.github.noamm9.event.impl.CheckEntityGlowEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.location.LocationUtils
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.boss.wither.WitherBoss
import java.awt.Color

object WitherESP: Feature("Highlights all Withers in F7.") {
    private val maxorColor by ColorSetting("Maxor", Color(88, 4, 164), false)
    private val stormColor by ColorSetting("Storm", Color(0, 208, 255), false)
    private val goldorColor by ColorSetting("Goldor", Color.WHITE, false)
    private val necronColor by ColorSetting("Necron", Color.RED, false)

    private enum class Wither(val color: ColorSetting) {
        MAXOR(maxorColor),
        STORM(stormColor),
        GOLDOR(goldorColor),
        NECRON(necronColor)
    }

    private var currentWither: WitherBoss? = null
    private var currentType = Wither.MAXOR

    override fun init() {
        register<WorldChangeEvent> {
            currentWither = null
            currentType = Wither.MAXOR
        }

        register<MainThreadPacketReceivedEvent.Post> {
            if (! isValidLoc()) return@register
            when (val packet = event.packet) {
                is ClientboundSetEntityDataPacket -> {
                    val entity = level.getEntity(packet.id) as? WitherBoss ?: return@register
                    if (entity.isInvisible || entity.invulnerableTicks == 800) return@register
                    currentWither = entity
                }

                is ClientboundRemoveEntitiesPacket -> {
                    if (packet.entityIds.any { currentWither?.id == it }) currentWither = null
                }
            }
        }

        register<BossBarUpdateEvent> {
            if (! isValidLoc()) return@register
            val bossName = event.name.unformattedText
            Wither.entries.find { bossName.contains(it.name, ignoreCase = true) }?.let {
                currentType = it
            }
        }

        register<CheckEntityGlowEvent> {
            if (! isValidLoc() || event.entity != currentWither) return@register
            event.color = currentType.color.value
        }
    }

    private fun isValidLoc() = LocationUtils.dungeonFloorNumber == 7 && LocationUtils.inBoss && LocationUtils.F7Phase != 5
}