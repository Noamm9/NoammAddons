package com.github.noamm9.features.impl.dungeon.solvers

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.Utils.favoriteColor
import com.github.noamm9.utils.dungeons.DungeonListener.dungeonTeammates
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.RenderHelper.renderVec
import com.github.noamm9.utils.render.RenderHelper.width
import com.github.noamm9.utils.render.world.Render3D.renderString
import com.github.noamm9.utils.render.world.Render3D.renderTracer
import gg.essential.universal.USound
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks

object LividSolver: Feature() {
    private val showHp by ToggleSetting("Show HP", true)
    private val tracer by ToggleSetting("Tracer", true)
    private val hideWrong by ToggleSetting("Hide Wrong")
    private val invulnerabilityTimer by ToggleSetting("Invulnerability Timer")
    private val iceSprayTitle by ToggleSetting("Ice Spray Title", true).showIf { invulnerabilityTimer.value }
    private val iceSpraySound by ToggleSetting("Ice Spray Sound", true).showIf { invulnerabilityTimer.value }
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

    private val ceilingWoolBlock = BlockPos(5, 108, 40)
    private var lividId: Int? = null

    private const val ticks = 390// todo: change to 350
    private var timer = - 1

    override fun init() {
        hudElement("Livid Invulnerability Timer", enabled = { invulnerabilityTimer.value }, shouldDraw = { timer > 0 }, centered = true) { ctx, example ->
            val displayTicks = if (example) ticks / 2 else timer
            val color = ColorUtils.colorCodeByPercent(ticks - displayTicks, ticks, true)
            val text = "&5Livid Invulnerability: $color${(displayTicks / 20.0).toFixed(1)}"
            ctx.drawCenteredString(text, 0, 0)
            text.width() to 9f
        }

        register<CheckEntityGlowEvent> {
            if (lividId == event.entity.id) {
                event.color = highlightColor.value
            }
        }

        register<CheckEntityRenderEvent> {
            if (! hideWrong.value) return@register
            if (LocationUtils.dungeonFloorNumber != 5) return@register
            if (! LocationUtils.inBoss) return@register
            if (lividId == event.entity.id) return@register
            if (lividId.livid?.isSleeping == true) return@register
            if (dungeonTeammates.any { it.entity?.uuid == event.entity.uuid }) return@register
            if (event.entity is ArmorStand && ! event.entity.name.unformattedText.contains("Livid")) return@register
            event.isCanceled = true
        }

        register<RenderWorldEvent> {
            val livid = lividId.livid ?: return@register
            if (livid.isDeadOrDying) return@register
            if (livid.isSleeping) return@register

            if (tracer.value) event.ctx.renderTracer(livid.renderVec.add(y = 0.9), tracerColor.value)
            if (showHp.value) event.ctx.renderString(
                ColorUtils.colorCodeByPercent(livid.health, livid.maxHealth) + NumbersUtils.format(livid.health),
                livid.renderVec.add(y = 3.0), scale = 1.5f
            )
        }

        register<TickEvent.Start> {
            if (! LocationUtils.inBoss || LocationUtils.dungeonFloorNumber != 5) return@register
            val targetLivid = lividMap[WorldUtils.getBlockAt(ceilingWoolBlock)] ?: return@register
            val currentLivid = lividId.livid
            if (currentLivid?.gameProfile?.name == targetLivid && currentLivid.isRemoved) return@register
            lividId = level.entitiesForRendering().asSequence().filterIsInstance<AbstractClientPlayer>().find {
                it.gameProfile.name == targetLivid
            }?.id
        }

        register<ChatMessageEvent> {
            if (! invulnerabilityTimer.value) return@register
            if (LocationUtils.dungeonFloorNumber != 5) return@register
            if (event.unformattedText != "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows.") return@register
            timer = ticks
        }

        register<TickEvent.Server> {
            if (timer > 0) timer --
            if (timer == 0) {
                if (iceSprayTitle.value) ChatUtils.showTitle("&bIce Spray Livid!")
                if (iceSpraySound.value) USound.playSoundStatic(SoundEvents.NOTE_BLOCK_PLING, 0.25f, 1f)
                timer = - 1
            }
        }

        register<WorldChangeEvent> {
            lividId = null
            timer = - 1
        }
    }

    private val Int?.livid get() = this?.let { level.getEntity(it) as? AbstractClientPlayer }
}
