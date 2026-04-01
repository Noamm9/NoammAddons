package com.github.noamm9.features.impl.floor7

import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.RenderWorldEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.hideIf
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.MathUtils.Vec3
import com.github.noamm9.utils.Utils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.NoammRenderLayers
import com.github.noamm9.utils.render.RenderHelper.renderX
import com.github.noamm9.utils.render.RenderHelper.renderY
import com.github.noamm9.utils.render.RenderHelper.renderZ
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color

object TerminalHitboxes: Feature() {
    private val mode by DropdownSetting("Mode", 1, listOf("Outline", "Fill", "Filled Outline"))
    private val phase by ToggleSetting("Phase", false)
    private val fillColor by ColorSetting("Fill Color", Color.orange).hideIf { mode.value == 0 }
    private val outlineColor by ColorSetting("Outline Color", Utils.favoriteColor).hideIf { mode.value == 1 }

    private val terminalPositions = listOf(
        listOf(Vec3(110, 113, 73), Vec3(110, 119, 79), Vec3(90, 112, 92), Vec3(90, 122, 101)),
        listOf(Vec3(68, 109, 122), Vec3(59, 119, 123), Vec3(47, 109, 122), Vec3(39, 108, 142), Vec3(40, 124, 123)),
        listOf(Vec3(- 2, 109, 112), Vec3(- 2, 119, 93), Vec3(18, 123, 93), Vec3(- 2, 109, 77)),
        listOf(Vec3(41, 109, 30), Vec3(44, 121, 30), Vec3(67, 109, 30), Vec3(72, 114, 47))
    )

    private val cachedTerminals = mutableMapOf<Int, MutableSet<ArmorStand>>()

    override fun init() {
        register<WorldChangeEvent> { cachedTerminals.clear() }

        register<MainThreadPacketReceivedEvent.Post> {
            if (LocationUtils.dungeonFloorNumber != 7 || LocationUtils.F7Phase != 3) return@register
            val packet = event.packet as? ClientboundSetEntityDataPacket ?: return@register
            val entity = mc.level?.getEntity(packet.id) as? ArmorStand ?: return@register
            val name = entity.customName?.unformattedText

            if (name == "Inactive Terminal") {
                for ((section, posList) in terminalPositions.withIndex()) {
                    if (posList.none { entity.distanceToSqr(it) <= 1.5 }) continue
                    cachedTerminals.getOrPut(section + 1) { mutableSetOf() }.add(entity)
                }
            }
            else if (name == "Terminal Active") {
                for (i in terminalPositions.indices) {
                    cachedTerminals[i + 1]?.remove(entity)
                }
            }
        }

        register<RenderWorldEvent> {
            if (! LocationUtils.inDungeon || LocationUtils.F7Phase != 3) return@register
            val section = LocationUtils.P3Section ?: return@register
            val terminalsToRender = cachedTerminals[section]?.takeUnless { it.isEmpty() } ?: return@register

            val consumers = event.ctx.consumers
            val matrices = event.ctx.matrixStack
            val cam = event.ctx.camera.position

            val drawFill = mode.value == 1 || mode.value == 2
            val drawOutline = mode.value == 0 || mode.value == 2

            val fillBuffer = if (drawFill) consumers.getBuffer(if (phase.value) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED) else null
            val outlineBuffer = if (drawOutline) consumers.getBuffer(if (phase.value) NoammRenderLayers.getLinesThroughWalls(2.0) else NoammRenderLayers.getLines(2.0)) else null

            matrices.pushPose()
            matrices.translate(- cam.x, - cam.y, - cam.z)

            for (entity in terminalsToRender) {
                val hw = entity.bbWidth / 2.0
                val hd = entity.bbHeight.toDouble()

                val minX = entity.renderX - hw
                val minY = entity.renderY
                val minZ = entity.renderZ - hw
                val maxX = entity.renderX + hw
                val maxY = entity.renderY + hd
                val maxZ = entity.renderZ + hw

                if (drawFill) ShapeRenderer.addChainedFilledBoxVertices(
                    matrices, fillBuffer,
                    minX, minY, minZ, maxX, maxY, maxZ,
                    fillColor.value.red / 255f, fillColor.value.green / 255f, fillColor.value.blue / 255f, fillColor.value.alpha / 255f
                )

                if (drawOutline) ShapeRenderer.renderLineBox(
                    matrices.last(), outlineBuffer,
                    minX, minY, minZ, maxX, maxY, maxZ,
                    outlineColor.value.red / 255f, outlineColor.value.green / 255f, outlineColor.value.blue / 255f, 1f
                )
            }

            matrices.popPose()
        }
    }
}