package com.github.noamm9.features.impl.dungeon.solvers

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.showIf
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.ThreadUtils
import com.github.noamm9.utils.Utils.favoriteColor
import com.github.noamm9.utils.dungeons.DungeonListener.dungeonTeammates
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderHelper.renderVec
import com.github.noamm9.utils.world.WorldUtils
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks

object LividSolver: Feature() {
    private val showHp by ToggleSetting("Show HP", true)
    private val tracer by ToggleSetting("Tracer", true)
    private val hideWrong by ToggleSetting("Hide Wrong")
    private val iceSprayTimer by ToggleSetting("Ice Spray Timer")
    private val highlightColor by ColorSetting("Highlight Color", favoriteColor, false).section("Colors")
    private val tracerColor by ColorSetting("Tracer Color", favoriteColor, false).showIf { tracer.value }

    private val lividMap = mapOf(
        Blocks.GREEN_WOOL to "Frog Livid",
        Blocks.PURPLE_WOOL to "Purple Livid",
        Blocks.GRAY_WOOL to "Doctor Livid",
        Blocks.BLUE_WOOL to "Scream Livid",
        Blocks.LIME_WOOL to "Smile Livid",
        Blocks.RED_WOOL to "Hockey Livid",
        Blocks.MAGENTA_WOOL to "Crossed Livid",
        Blocks.YELLOW_WOOL to "Arcade Livid",
        Blocks.WHITE_WOOL to "Vendetta Livid"
    )

    private var currentLivid: AbstractClientPlayer? = null
    private val ceilingWoolBlock = BlockPos(5, 108, 40)

    override fun init() {
        register<CheckEntityGlowEvent> {
            if (currentLivid == event.entity) {
                event.color = highlightColor.value
            }
        }

        register<CheckEntityRenderEvent> {
            if (! hideWrong.value) return@register
            if (LocationUtils.dungeonFloorNumber != 5) return@register
            if (! LocationUtils.inBoss) return@register
            if (currentLivid == event.entity) return@register
            if (currentLivid?.isSleeping == true) return@register
            if (dungeonTeammates.any { it.entity?.uuid == event.entity.uuid }) return@register
            if (event.entity is ArmorStand && ! event.entity.name.unformattedText.contains("Livid")) return@register
            event.isCanceled = true
        }

        register<RenderWorldEvent> {
            val livid = currentLivid ?: return@register
            if (! livid.isAlive) return@register
            if (livid.isSleeping) return@register

            if (tracer.value) Render3D.renderTracer(event.ctx, livid.renderVec.add(y = 0.9), tracerColor.value)
            if (showHp.value) Render3D.renderString(
                ColorUtils.colorCodeByPercent(livid.health, livid.maxHealth) + NumbersUtils.format(livid.health),
                livid.renderVec.add(y = 3.0), scale = 1.5f
            )
        }

        register<TickEvent.Start> {
            if (! LocationUtils.inBoss || LocationUtils.dungeonFloorNumber != 5) {
                currentLivid = null
                return@register
            }

            val targetLivid = lividMap[WorldUtils.getBlockAt(ceilingWoolBlock)] ?: return@register
            if (currentLivid?.name?.unformattedText == targetLivid && currentLivid?.isAlive == true) return@register
            currentLivid = mc.level?.entitiesForRendering()?.asSequence()?.filterIsInstance<AbstractClientPlayer>()?.find {
                it.name.unformattedText == targetLivid
            }
        }

        register<WorldChangeEvent> { currentLivid = null }

        register<ChatMessageEvent> {
            if (! iceSprayTimer.value) return@register
            if (LocationUtils.dungeonFloorNumber != 5) return@register
            if (event.unformattedText != "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows.") return@register
            ThreadUtils.scheduledTaskServer(390) {
                ChatUtils.showTitle("&bIce Spray Livid!")
                mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1f))
            }
        }
    }
}