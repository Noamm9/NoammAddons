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

            val ctx = event.ctx
            val consumers = ctx.consumers
            val matrices = ctx.matrixStack
            val cam = ctx.camera.position
            
            val drawFill = mode.value == 1 || mode.value == 2
            val drawOutline = mode.value == 0 || mode.value == 2

            matrices.pushPose()
            matrices.translate(- cam.x, - cam.y, - cam.z)

            val fColor = fillColor.value
            val fR = fColor.red / 255f
            val fG = fColor.green / 255f
            val fB = fColor.blue / 255f
            val fA = fColor.alpha / 255f

            val oColor = outlineColor.value
            val oR = oColor.red / 255f
            val oG = oColor.green / 255f
            val oB = oColor.blue / 255f

            if (drawFill) {
                val fillBuffer = consumers.getBuffer(if (phase.value) NoammRenderLayers.FILLED_THROUGH_WALLS else NoammRenderLayers.FILLED)
                for (entity in terminalsToRender) {
                    val hw = entity.bbWidth / 2.0
                    val h = entity.bbHeight.toDouble()
                    ShapeRenderer.addChainedFilledBoxVertices(
                        matrices, fillBuffer,
                        entity.renderX - hw, entity.renderY, entity.renderZ - hw,
                        entity.renderX + hw, entity.renderY + h, entity.renderZ + hw,
                        fR, fG, fB, fA
                    )
                }
            }

            if (drawOutline) {
                val outlineBuffer = consumers.getBuffer(if (phase.value) NoammRenderLayers.getLinesThroughWalls(2.0) else NoammRenderLayers.getLines(2.0))
                for (entity in terminalsToRender) {
                    val hw = entity.bbWidth / 2.0
                    val h = entity.bbHeight.toDouble()
                    ShapeRenderer.renderLineBox(
                        matrices.last(), outlineBuffer,
                        entity.renderX - hw, entity.renderY, entity.renderZ - hw,
                        entity.renderX + hw, entity.renderY + h, entity.renderZ + hw,
                        oR, oG, oB, 1f
                    )
                }
            }

            matrices.popPose()
        }
    }
}