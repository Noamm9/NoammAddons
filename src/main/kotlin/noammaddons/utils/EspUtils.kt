package noammaddons.utils

import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.shader.Framebuffer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.noammaddons.Companion.config
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

    @Suppress("NAME_SHADOWING")
    fun espMob(
        entity: Entity,
        color: Color,
        type: Int = config.espType
    ) {
        val entity = entity as? EntityLivingBase ?: return

        when (type) {
            0 -> outlineEntities.add(entity to color)
            1 -> boxEntities.add(entity to color)
            2 -> {
                chamEntities.add(entity to color)
                outlineEntities.add(entity to color)
            }

            3 -> d2Entities.add(entity to color)
            4 -> chamEntities.add(entity to color)
        }
    }


    @JvmField
    val outlineEntities = CopyOnWriteArrayList<Pair<EntityLivingBase, Color>>()

    @JvmField
    val chamEntities = CopyOnWriteArrayList<Pair<EntityLivingBase, Color>>()

    @JvmField
    val boxEntities = CopyOnWriteArrayList<Pair<EntityLivingBase, Color>>()

    @JvmField
    val d2Entities = CopyOnWriteArrayList<Pair<EntityLivingBase, Color>>()

    @JvmStatic
    val allEntities get() = outlineEntities + chamEntities + boxEntities + d2Entities

    private fun CopyOnWriteArrayList<Pair<EntityLivingBase, Color>>.remove(entity: EntityLivingBase) {
        removeIf { it.first == entity }
    }

    @SubscribeEvent
    fun onEvent(event: Event) = when (event) {
        is WorldUnloadEvent -> {
            outlineEntities.clear()
            chamEntities.clear()
            boxEntities.clear()
            d2Entities.clear()
        }

        is PostRenderEntityModelEvent -> outlineEntities.forEach {
            if (event.entity != it.first) return@forEach
            val distance = distance3D(event.entity.renderVec, mc.thePlayer.renderVec)
            val adjustedLineWidth = (config.espOutlineWidth / (distance / 8f)).coerceIn(0.5, config.espOutlineWidth.toDouble()).toFloat()
            val fancyGraphics = mc.gameSettings.fancyGraphics
            val gamma = mc.gameSettings.gammaSetting

            mc.gameSettings.fancyGraphics = false
            mc.gameSettings.gammaSetting = 100000f
            glPushMatrix()
            glPushAttrib(GL_ALL_ATTRIB_BITS)
            checkSetupFBO()
            glBindColor(it.second)
            renderOne(adjustedLineWidth)
            render(event)
            renderTwo()
            render(event)
            renderThree()
            render(event)
            renderFour()
            render(event)
            glPopAttrib()
            glPopMatrix()
            mc.gameSettings.fancyGraphics = fancyGraphics
            mc.gameSettings.gammaSetting = gamma
            outlineEntities.remove(event.entity)
        }

        is RenderOverlay -> d2Entities.forEach {
            draw2dEsp(it.first, it.second)
            d2Entities.remove(it.first)
        }

        is RenderWorld -> boxEntities.forEach {
            drawEntityBox(it.first, it.second)
            boxEntities.remove(it.first)
        }

        else -> Unit
    }


    private fun render(event: PostRenderEntityModelEvent) {
        event.modelBase.render(
            event.entity,
            event.p_77036_2_,
            event.p_77036_3_,
            event.p_77036_4_,
            event.p_77036_5_,
            event.p_77036_6_,
            event.scaleFactor
        )
    }

    private fun renderOne(LineWidth: Float) {
        glDisable(GL_ALPHA_TEST)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_LIGHTING)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glLineWidth(LineWidth)
        glEnable(GL_LINE_SMOOTH)
        glEnable(GL_STENCIL_TEST)
        glClear(GL_STENCIL_BUFFER_BIT)
        glClearStencil(0xF)
        glStencilFunc(GL_NEVER, 1, 0xF)
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
    }

    private fun renderTwo() {
        glStencilFunc(GL_NEVER, 0, 0xF)
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)
    }

    private fun renderThree() {
        glStencilFunc(GL_EQUAL, 1, 0xF)
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
    }

    private fun renderFour() {
        glDepthMask(false)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_POLYGON_OFFSET_LINE)
        glPolygonOffset(1.0f, - 2000000f)
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f)
    }

    private fun checkSetupFBO() {
        val fbo = mc.framebuffer
        if (fbo != null) {
            if (fbo.depthBuffer > - 1) {
                setupFBO(fbo)
                fbo.depthBuffer = - 1
            }
        }
    }

    private fun setupFBO(fbo: Framebuffer) {
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
    }
}
