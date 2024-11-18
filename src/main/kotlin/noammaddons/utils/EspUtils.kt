package noammaddons.utils

import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.shader.Framebuffer
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderEntityModelEvent
import noammaddons.noammaddons.Companion.config
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.MathUtils.distanceIn3DWorld
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderHelper.getRenderVec
import noammaddons.utils.RenderHelper.glBindColor
import noammaddons.utils.Utils.equalsOneOf
import org.lwjgl.opengl.EXTFramebufferObject
import org.lwjgl.opengl.EXTPackedDepthStencil
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object EspUtils {

    fun EspMob(
        event: RenderEntityModelEvent,
        color: Color,
        lineWidth: Float = config.espOutlineWidth,
        outline: Boolean = config.espType.equalsOneOf(2, 0)/* && config.espOutlineOpacity != 0f*/,
        fill: Boolean = config.espType == 2/* && config.espFilledOpacity != 0f*/
    ) {
        val distance = distanceIn3DWorld(event.entity.getRenderVec(), Player?.getRenderVec() ?: return)
        val adjustedLineWidth = (lineWidth / (distance / 8f)).coerceIn(0.5, lineWidth.toDouble()).toFloat()

        if (fill) {
            val index = chamEntities.map { it.first }.indexOf(event.entity)

            if (index == - 1) addChamESP(event.entity, color)
            else {
                val newList = chamEntities.toMutableList()
                newList[index] = event.entity to color

                chamEntities.clear()
                chamEntities.addAll(newList)
            }
        }
        else removeChamESP(event.entity)

        if (! outline) return
        val fancyGraphics = mc.gameSettings.fancyGraphics
        val gamma = mc.gameSettings.gammaSetting
        mc.gameSettings.fancyGraphics = false
        mc.gameSettings.gammaSetting = 100000f
        glPushMatrix()
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        checkSetupFBO()
        glBindColor(color)
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
    }

    private val chamEntities = mutableSetOf<Pair<EntityLivingBase, Color>>()

    @JvmStatic
    @Synchronized
    fun addChamESP(entity: EntityLivingBase, color: Color) {
        chamEntities.add(entity to color)
    }

    @JvmStatic
    @Synchronized
    fun removeChamESP(entity: EntityLivingBase) {
        chamEntities.removeIf { it.first == entity }
    }

    @JvmStatic
    @Synchronized
    fun getChamColor(entity: EntityLivingBase): Color? {
        return chamEntities.firstOrNull { it.first == entity }?.second
    }

    @JvmStatic
    @Synchronized
    fun hasCham(entity: EntityLivingBase): Boolean {
        return chamEntities.any { it.first == entity }
    }

    @Synchronized
    @SubscribeEvent
    fun reset(event: WorldEvent.Unload) {
        chamEntities.clear()
    }


    private fun render(event: RenderEntityModelEvent) {
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
