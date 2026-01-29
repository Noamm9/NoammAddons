package com.github.noamm9.features.impl.dungeon.solvers.devices

import com.github.noamm9.NoammAddons
import com.github.noamm9.event.impl.BlockChangeEvent
import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ColorSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.section
import com.github.noamm9.ui.clickgui.componnents.withDescription
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.MathUtils.add
import com.github.noamm9.utils.PlayerUtils
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.NoammRenderLayers
import com.github.noamm9.utils.render.Render3D
import com.github.noamm9.utils.render.RenderContext
import com.github.noamm9.utils.world.WorldUtils
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import org.lwjgl.glfw.GLFW
import java.awt.Color

object SimonSaysSolver: Feature() {
    private val blockWrongClicks by ToggleSetting("Block Wrong Clicks").withDescription("Blocks wrong clicks. &eSneak to Override and stop blocking.").section("Options")
    private val sendReset by ToggleSetting("Send Reset Message").withDescription("Sends a reset message to the party when the solver breaks.")

    private val color1 by ColorSetting("First Color", Color.GREEN).withDescription("Color of the first button.").section("Colors")
    private val color2 by ColorSetting("Second Color", Color.YELLOW).withDescription("Color of the second button.")
    private val color3 by ColorSetting("Other Color", Color.RED).withDescription("Color of the rest of the buttons.")

    private val blocks = LinkedHashSet<BlockPos>()
    private var hadButtons = false

    private val isSimonSaysActive get() = enabled && LocationUtils.F7Phase == 3
    private val startObsidianBlock = BlockPos(111, 120, 92)
    private val startButtonPos = startObsidianBlock.add(- 1, 0, 0)
    private val devStartBtn = BlockPos(110, 121, 91)


    override fun init() {
        register<BlockChangeEvent> {
            if (! isSimonSaysActive) return@register
            val buttonsExist = WorldUtils.getBlockAt(startButtonPos) == Blocks.STONE_BUTTON
            if (buttonsExist != hadButtons) {
                hadButtons = buttonsExist
                if (! buttonsExist) {
                    if (sendReset.value && blocks.isNotEmpty() && MathUtils.distance2D(startButtonPos, mc.player !!.blockPosition()) < 5) {
                        ChatUtils.sendPartyMessage("SS BROKE!!! SS BROKE!!! SS BROKE!!! SS BROKE!!! SS BROKE!!! SS BROKE!!! SS BROKE!!!")
                    }
                    blocks.clear()
                }
            }

            for (dy in 0 .. 3) {
                for (dz in 0 .. 3) {
                    val pos = startObsidianBlock.add(0, dy, dz)
                    if (WorldUtils.getBlockAt(pos) == Blocks.SEA_LANTERN) {
                        blocks.add(pos)
                    }
                }
            }

            if (! blocks.isEmpty() && NoammAddons.debugFlags.contains("ss")) {
                ChatUtils.modMessage("SimonSaysSolver: ${blocks.size}")
            }
        }

        register<MouseClickEvent> {
            if (! isSimonSaysActive) return@register
            if (event.action != GLFW.GLFW_PRESS) return@register
            if (mc.screen != null) return@register
            val lookPos = PlayerUtils.getSelectionBlock() ?: return@register
            if (lookPos == devStartBtn) return@register blocks.clear()
            val nextBlock = blocks.firstOrNull() ?: return@register
            if (WorldUtils.getBlockAt(lookPos) != Blocks.STONE_BUTTON) return@register

            if (lookPos.add(1, 0, 0) == nextBlock) blocks.remove(nextBlock)
            else if (blockWrongClicks.value) {
                if (mc.player?.isCrouching == true) return@register
                if (! event.button.equalsOneOf(0, 1)) return@register
                event.cancel()
            }
        }

        register<RenderWorldEvent> {
            if (blocks.isEmpty()) return@register
            val countColor = ColorUtils.colorCodeByPresent(blocks.size, 5)

            Render3D.renderString(
                "$countColor${blocks.size}",
                devStartBtn.x + 0.7,
                devStartBtn.y + 1.0,
                devStartBtn.z + 0.5,
                scale = 2f,
            )

            for ((index, pos) in blocks.withIndex()) {
                renderButtonBox(event.ctx, pos.add(- 1, 0, 0), index)
            }
        }
    }

    private fun renderButtonBox(ctx: RenderContext, pos: BlockPos, index: Int) {
        val consumers = ctx.consumers ?: return
        val matrices = ctx.matrixStack ?: return
        val cam = ctx.camera.position.reverse()
        val color = when (index) {
            0 -> color1
            1 -> color2
            else -> color3
        }.value

        val expand = 0.01
        val radiusLong = 0.1875
        val radiusShort = 0.125

        val absMinX = pos.x + 1.0 - radiusShort - expand
        val absMinY = pos.y + 0.5 - radiusShort - expand
        val absMinZ = pos.z + 0.5 - radiusLong - expand
        val absMaxX = pos.x + 1.0 + expand
        val absMaxY = pos.y + 0.5 + radiusShort + expand
        val absMaxZ = pos.z + 0.5 + radiusLong + expand

        matrices.pushPose()
        matrices.translate(cam.x, cam.y, cam.z)

        ShapeRenderer.addChainedFilledBoxVertices(
            matrices,
            consumers.getBuffer(NoammRenderLayers.FILLED),
            absMinX, absMinY, absMinZ,
            absMaxX, absMaxY, absMaxZ,
            color.red / 255f, color.green / 255f, color.blue / 255f, 0.39f
        )

        ShapeRenderer.renderLineBox(
            matrices.last(),
            consumers.getBuffer(NoammRenderLayers.getLines(2.5)),
            absMinX, absMinY, absMinZ,
            absMaxX, absMaxY, absMaxZ,
            color.red / 255f, color.green / 255f, color.blue / 255f, 1f
        )

        matrices.popPose()
    }
}