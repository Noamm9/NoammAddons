package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.CheckEntityGlowEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.enums.Classes
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderHelper.renderVec
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.Entity

object TeammateESP: Feature("Highlights your dungeon party.") {
    val highlight by ToggleSetting("Highlight Teammates", true)
    val drawName by ToggleSetting("Show Teammate Name", true)

    override fun init() {
        register<CheckEntityGlowEvent> {
            if (! highlight.value) return@register
            if (! LocationUtils.inDungeon) return@register
            if (event.entity !is AbstractClientPlayer) return@register

            for (teammate in DungeonListener.dungeonTeammates) {
                if (teammate.entity?.id != event.entity.id) continue
                event.color = teammate.clazz.color
            }
        }

        register<RenderWorldEvent> {
            if (! drawName.value) return@register
            if (! LocationUtils.inDungeon) return@register
            for (teammate in DungeonListener.dungeonTeammatesNoSelf) {
                val entity = teammate.entity ?: continue
                val color = Classes.getColorCode(teammate.clazz)
                val renderVec = entity.renderVec
                val distance = MathUtils.distance3D(renderVec, mc.player !!.renderVec)
                val scale = (distance * 0.12f).coerceAtLeast(1.0)

                Render3D.renderString(
                    "&e[${teammate.clazz.name[0]}&e] $color${teammate.name}",
                    renderVec.x,
                    renderVec.y + entity.bbHeight + 0.7 + distance * 0.015f,
                    renderVec.z,
                    scale = scale,
                    bgBox = false,
                    phase = true
                )
            }
        }
    }

    @JvmStatic
    fun shouldHideNametag(entity: Entity): Boolean {
        if (! drawName.value) return false
        if (! LocationUtils.inDungeon) return false
        return DungeonListener.dungeonTeammatesNoSelf.any { it.entity?.id == entity.id }
    }
}