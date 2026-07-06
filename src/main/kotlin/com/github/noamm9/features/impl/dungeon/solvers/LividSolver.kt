package com.github.noamm9.features.impl.dungeon.solvers

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.NumbersUtils.toFixed
import com.github.noamm9.utils.Utils.favoriteColor
import com.github.noamm9.utils.WorldUtils
import com.github.noamm9.utils.dungeons.DungeonListener.dungeonTeammates
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderHelper.renderVec
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
    private val invulnerabilityTimer by ToggleSetting("Invulnerability Timer")
    private val iceSprayTitle by ToggleSetting("Ice Spray Title", true).showIf { invulnerabilityTimer.value }
    private val iceSpraySound by ToggleSetting("Ice Spray Sound", true).showIf { invulnerabilityTimer.value }
    //#if CHEAT
    private val autoRagnarock by ToggleSetting("Auto Ragnarock", false).showIf { invulnerabilityTimer.value }
    //#endif
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

    private const val ticks = 390
    private var timer = -1
    //#if CHEAT
    private const val ragnarockSwapTick = 180
    private const val ragnarockUseTick = 150
    private var ragnarockUsed = false
    //#endif

    override fun init() {
        hudElement("Livid Invulnerability Timer", enabled = { invulnerabilityTimer.value }, shouldDraw = { timer > 0 }, centered = true) { ctx, example ->
            val displayTicks = if (example) ticks / 2 else timer
            val color = ColorUtils.colorCodeByPercent(ticks - displayTicks, ticks, true)
            val text = "&5Livid Invulnerability: $color${(displayTicks / 20.0).toFixed(1)}"
            Render2D.drawCenteredString(ctx, text, 0, 0)
            text.width().toFloat() to 9f
        }

        register<CheckEntityGlowEvent> {
            if (currentLivid == event.entity) {
                event.color = highlightColor.value
            }
        }

        register<CheckEntityRenderEvent> {
            if (!hideWrong.value) return@register
            if (LocationUtils.dungeonFloorNumber != 5) return@register
            if (!LocationUtils.inBoss) return@register
            if (currentLivid == event.entity) return@register
            if (currentLivid?.isSleeping == true) return@register
            if (dungeonTeammates.any { it.entity?.uuid == event.entity.uuid }) return@register
            if (event.entity is ArmorStand && !event.entity.name.unformattedText.contains("Livid")) return@register
            event.isCanceled = true
        }

        register<RenderWorldEvent> {
            val livid = currentLivid ?: return@register
            if (!livid.isAlive) return@register
            if (livid.isSleeping) return@register
            if (tracer.value) Render3D.renderTracer(event.ctx, livid.renderVec.add(y = 0.9), tracerColor.value)
            if (showHp.value) Render3D.renderString(
                ColorUtils.colorCodeByPercent(livid.health, livid.maxHealth) + NumbersUtils.format(livid.health),
                livid.renderVec.add(y = 3.0), scale = 1.5f
            )
        }

        register<TickEvent.Start> {
            if (!LocationUtils.inBoss || LocationUtils.dungeonFloorNumber != 5) return@register
            val targetLivid = lividMap[WorldUtils.getBlockAt(ceilingWoolBlock)] ?: return@register
            if (currentLivid?.gameProfile?.name == targetLivid && currentLivid?.isRemoved != false) return@register
            currentLivid = mc.level?.entitiesForRendering()?.asSequence()?.filterIsInstance<AbstractClientPlayer>()?.find {
                it.gameProfile.name == targetLivid
            }
        }

        register<ChatMessageEvent> {
            if (!invulnerabilityTimer.value) return@register
            if (LocationUtils.dungeonFloorNumber != 5) return@register
            if (event.unformattedText != "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows.") return@register
            timer = ticks
            //#if CHEAT
            ragnarockUsed = false
            //#endif
        }

        register<TickEvent.Server> {
            if (timer > 0) timer--

            //#if CHEAT
            if (autoRagnarock.value) {
                if (timer == ragnarockSwapTick) {
                    val slot = PlayerUtils.findHotbarSlot { it.skyblockId.contains("RAGNAROCK") }
                    if (slot != null) PlayerUtils.swapToSlot(slot)
                    else ChatUtils.modMessage("&cNo Ragnarock found in hotbar!")
                }

                if (timer == ragnarockUseTick && !ragnarockUsed) {
                    val slot = PlayerUtils.findHotbarSlot { it.skyblockId.contains("RAGNAROCK") }
                    if (slot != null) {
                        if (mc.player?.inventory?.selectedSlot != slot) PlayerUtils.swapToSlot(slot)
                        PlayerUtils.rightClick()
                        ragnarockUsed = true
                    }
                }
            }
            //#endif

            if (timer == 0) {
                if (iceSprayTitle.value) ChatUtils.showTitle("&bIce Spray Livid!")
                if (iceSpraySound.value) mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1f))
                timer = -1
                //#if CHEAT
                ragnarockUsed = false
                //#endif
            }
        }

        register<WorldChangeEvent> {
            currentLivid = null
            timer = -1
            //#if CHEAT
            ragnarockUsed = false
            //#endif
        }
    }
}
