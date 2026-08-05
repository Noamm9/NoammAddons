package com.github.noamm9.features.impl.floor7

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.TickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.MathUtils.aabb
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render3D.renderBoxBounds
import com.github.noamm9.utils.render.Render3D.renderString
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.phys.AABB
import java.awt.Color

object LeapCounter: Feature("Shows how many players have leaped you") {
    private var currentSpot: REGION? = null
    private var count = 0

    override fun init() {
        hudElement("LeapCounter", centered = true) { ctx, e ->
            val maxCount = if (e) 4 else currentSpot?.maxCount ?: return@hudElement 0f to 0f
            val startFormat = (if (maxCount - count <= 1) "§9" else "§4")
            val str = "$startFormat$count§9/$maxCount Players Leaped"
            ctx.drawCenteredString(str, 0, 0)
            str.width().toFloat() to 9f
        }

        register<TickEvent.End> {
            if (! LocationUtils.inBoss && LocationUtils.dungeonFloorNumber != 7) return@register

            currentSpot = REGION.entries.find { it.box.contains(player.position()) } ?: run {
                currentSpot = null
                count = 0
                return@register
            }

            currentSpot?.let { count = maxOf(count, getCount(level, player, it)) }
        }

        register<RenderWorldEvent> {
            if ("lcbox" in NoammAddons.debugFlags) REGION.entries.forEach {
                event.ctx.renderString(it.name, it.box.center.add(y = 2), scale = 2)
                event.ctx.renderBoxBounds(it.box, Color.YELLOW.withAlpha(100))
            }
        }
    }

    private fun getCount(world: ClientLevel, player: LocalPlayer, region: REGION): Int {
        val entities = world.getEntities(player, region.box).filterIsInstance<AbstractClientPlayer>().map { it.id }
        val teammates = DungeonListener.dungeonTeammatesNoSelf.mapNotNull { it.entity?.id }
        return entities.count { it in teammates }
    }

    private enum class REGION(val box: AABB, val maxCount: Int) {
        SS_BOX(aabb(106, 119, 92, 109, 121, 96), 3),
        EE2_BOX(aabb(57, 108, 130, 59, 110, 132), 4),
        HEE2_BOX(aabb(57, 132, 138, 62, 133, 140), 4),
        EE3_BOX(aabb(1, 108, 101, 3, 110, 107), 3),
        CORE_BOX(aabb(53.5, 114, 49.5, 55.5, 116, 51.5), 4),
        RELIC_BOX(aabb(51.5, 3, 73.5, 57.5, 8, 79.5), 4)
    }
}