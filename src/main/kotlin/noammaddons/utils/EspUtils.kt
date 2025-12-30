package noammaddons.utils

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.*
import net.minecraftforge.fml.common.gameevent.TickEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.*
import noammaddons.features.impl.esp.EspSettings
import noammaddons.utils.MathUtils.destructured
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.RenderUtils2D.draw2dEsp
import noammaddons.utils.RenderUtils2D.modelViewMatrix
import noammaddons.utils.RenderUtils2D.projectionMatrix
import noammaddons.utils.RenderUtils2D.viewportDims
import noammaddons.utils.shaders.OutlineShader
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

object EspUtils {
    fun espMob(sourceEntity: Entity, color: Color, typeId: Int = EspSettings.highlightType) {
        val espType = ESPType.fromId(typeId).takeIf { it != ESPType.Disable } ?: return
        val entity = sourceEntity as? EntityLivingBase ?: return
        if (espType == ESPType.FILLED_OUTLINE) {
            ESPType.OUTLINE.addEntity(entity, color)
            ESPType.CHAM.addEntity(entity, color)
        }
        else espType.addEntity(entity, color)
    }

    enum class ESPType(val displayName: String) {
        Disable("Disable"),
        BOX("3D Box"),
        BOX2D("2D Box"),
        OUTLINE("Outline"),
        FILLED_OUTLINE("Filled Outline"),
        CHAM("Cham");

        private val entitiesMap = ConcurrentHashMap<EntityLivingBase, Color>()

        fun getEntities(): MutableMap<EntityLivingBase, Color> = entitiesMap

        fun addEntity(entity: EntityLivingBase, color: Color) {
            entitiesMap[entity] = color
        }

        fun removeEntity(entity: EntityLivingBase) {
            entitiesMap.remove(entity)
        }

        fun getColor(entity: Entity): Color? = entitiesMap[entity as? EntityLivingBase]

        fun clearEntities() = entitiesMap.clear()

        companion object {
            fun fromId(id: Int): ESPType? = entries.find { it.ordinal == id }
            fun resetAll() = entries.forEach(ESPType::clearEntities)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onEvent(event: Event) {
        when (event) {
            is WorldUnloadEvent -> ESPType.resetAll()

            is RenderOverlayNoCaching -> processAndRemoveEntities(ESPType.BOX2D) { entity, color ->
                draw2dEsp(entity, color)
            }

            is RenderEntityModelEvent -> ESPType.OUTLINE.getColor(event.entity)?.let { color ->
                OutlineShader.renderSilhouette(event, color)
                ESPType.OUTLINE.removeEntity(event.entity)
            }

            is RenderWorld -> {
                OutlineShader.drawOutline(radius = 2.0f)

                processAndRemoveEntities(ESPType.BOX) { entity, color ->
                    drawEntityBox(entity, color)
                }
            }

            is RenderWorldLastEvent -> {
                val (x, y, z) = mc.thePlayer?.renderVec?.destructured() ?: return
                GlStateManager.pushMatrix()
                GlStateManager.translate(- x, - y, - z)

                glGetFloat(GL_MODELVIEW_MATRIX, modelViewMatrix)
                glGetFloat(GL_PROJECTION_MATRIX, projectionMatrix)

                GlStateManager.popMatrix()
                glGetInteger(GL_VIEWPORT, viewportDims)
            }

            is TickEvent.RenderTickEvent -> {
                if (event.phase == TickEvent.Phase.START) {
                    OutlineShader.reset()
                }
            }
        }
    }

    private fun processAndRemoveEntities(espType: ESPType, renderAction: (entity: EntityLivingBase, color: Color) -> Unit) {
        val map = espType.getEntities()
        if (map.isEmpty()) return

        for (entity in map.keys) {
            val color = map.remove(entity) ?: continue
            renderAction(entity, color)
        }
    }
}