package noammaddons.utils

import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.*
import noammaddons.events.*
import noammaddons.features.impl.esp.EspSettings
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.MathUtils.distance3D
import noammaddons.utils.RenderHelper.glBindColor
import noammaddons.utils.RenderHelper.renderVec
import noammaddons.utils.RenderUtils.drawEntityBox
import noammaddons.utils.RenderUtils2D.draw2dEsp
import org.lwjgl.opengl.EXTFramebufferObject
import org.lwjgl.opengl.EXTPackedDepthStencil
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

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

        private val entitiesList = CopyOnWriteArrayList<Pair<EntityLivingBase, Color>>()
        fun getEntities(): List<Pair<EntityLivingBase, Color>> = entitiesList

        fun addEntity(entity: EntityLivingBase, color: Color) = entitiesList.takeIf { ! containsEntity(entity) }?.add(entity to color)
        fun removeEntity(entity: EntityLivingBase) = entitiesList.removeIf { it.first == entity }
        fun containsEntity(entity: Any) = entitiesList.any { it.first == entity as? EntityLivingBase }
        fun clearEntities() = entitiesList.clear()

        companion object {
            fun fromId(id: Int): ESPType? = entries.find { it.ordinal == id }
            fun getAllEntities(): List<Pair<EntityLivingBase, Color>> = entries.flatMap { it.getEntities() }
            fun resetAll() = entries.forEach(ESPType::clearEntities)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onEvent(event: Event) {
        when (event) {
            is WorldUnloadEvent -> ESPType.resetAll()

            is PostRenderEntityModelEvent -> {
                ESPType.OUTLINE.getEntities().find { it.first == event.entity }?.second?.let { renderOutline(event, it) }
            }

            is RenderOverlay -> processAndRemoveEntities(ESPType.BOX2D) { entity, color ->
                draw2dEsp(entity, color)
            }

            is RenderWorld -> processAndRemoveEntities(ESPType.BOX) { entity, color ->
                drawEntityBox(entity, color)
            }
        }
    }

    private fun renderOutline(event: PostRenderEntityModelEvent, color: Color) {
        val distance = distance3D(event.entity.renderVec, mc.thePlayer.renderVec)
        val adjustedLineWidth = (EspSettings.lineWidth.toDouble() / (distance / 8f))
            .coerceIn(0.5, EspSettings.lineWidth.toDouble()).toFloat()

        val originalFancyGraphics = mc.gameSettings.fancyGraphics
        val originalGamma = mc.gameSettings.gammaSetting
        mc.gameSettings.fancyGraphics = false
        mc.gameSettings.gammaSetting = 100000f

        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)

        checkSetupFBO()
        glBindColor(color)
        glDisable(GL_ALPHA_TEST)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_LIGHTING)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glLineWidth(adjustedLineWidth)
        glEnable(GL_LINE_SMOOTH)
        glEnable(GL_STENCIL_TEST)
        glClear(GL_STENCIL_BUFFER_BIT)
        glClearStencil(0xF)
        glStencilFunc(GL_NEVER, 1, 0xF)
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        render(event)
        glStencilFunc(GL_NEVER, 0, 0xF)
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
        render(event)
        glStencilFunc(GL_EQUAL, 1, 0xF)
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
        render(event)
        glDepthMask(false)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_POLYGON_OFFSET_LINE)
        glPolygonOffset(1.0f, - 2000000f)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f)
        render(event)

        glPopAttrib()
        glPopMatrix()

        mc.gameSettings.fancyGraphics = originalFancyGraphics
        mc.gameSettings.gammaSetting = originalGamma

        ESPType.OUTLINE.removeEntity(event.entity)
    }

    private fun processAndRemoveEntities(espType: ESPType, renderAction: (entity: EntityLivingBase, color: Color) -> Unit) {
        val entitiesToProcess = espType.getEntities()
        if (entitiesToProcess.isEmpty()) return

        ArrayList(entitiesToProcess).forEach { (entity, color) ->
            renderAction(entity, color)
            espType.removeEntity(entity)
        }
    }

    private fun render(event: PostRenderEntityModelEvent) = event.modelBase.render(
        event.entity,
        event.p_77036_2_,
        event.p_77036_3_,
        event.p_77036_4_,
        event.p_77036_5_,
        event.p_77036_6_,
        event.scaleFactor
    )

    private fun checkSetupFBO() {
        val fbo = mc.framebuffer?.takeIf { it.depthBuffer > - 1 } ?: return
        EXTFramebufferObject.glDeleteRenderbuffersEXT(fbo.depthBuffer)
        val stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT()
        EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
        EXTFramebufferObject.glRenderbufferStorageEXT(
            EXTFramebufferObject.GL_RENDERBUFFER_EXT,
            EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT,
            mc.displayWidth,
            mc.displayHeight
        )
        EXTFramebufferObject.glFramebufferRenderbufferEXT(
            EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
            EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT,
            EXTFramebufferObject.GL_RENDERBUFFER_EXT,
            stencilDepthBufferID
        )
        EXTFramebufferObject.glFramebufferRenderbufferEXT(
            EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
            EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
            EXTFramebufferObject.GL_RENDERBUFFER_EXT,
            stencilDepthBufferID
        )
        fbo.depthBuffer = - 1
    }
}
