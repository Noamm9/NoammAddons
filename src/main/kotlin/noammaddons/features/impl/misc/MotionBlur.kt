package noammaddons.features.impl.misc

import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.Framebuffer
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.Dev
import noammaddons.ui.config.core.impl.SliderSetting
import org.lwjgl.opengl.GL11.*

@Dev
object MotionBlur: Feature("Blurs your screen a little to simulate smoother and cleaner gameplay") {
    private val blurStrength by SliderSetting("Blur Strength", 1, 10, 1, 4)

    private var blurBufferMain: Framebuffer? = null
    private var blurBufferInto: Framebuffer? = null

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: RenderOverlay) {
        if (mc.currentScreen != null) return
        if (! OpenGlHelper.isFramebufferEnabled()) return

        val width = mc.framebuffer.framebufferWidth
        val height = mc.framebuffer.framebufferHeight

        setupProjectionMatrix(width, height)

        blurBufferMain = checkFramebufferSizes(blurBufferMain, width, height)
        blurBufferInto = checkFramebufferSizes(blurBufferInto, width, height)

        blurBufferInto?.apply {
            framebufferClear()
            bindFramebuffer(true)
        }

        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 0, 1)
        GlStateManager.disableLighting()
        GlStateManager.disableFog()
        GlStateManager.disableBlend()

        mc.framebuffer.bindFramebufferTexture()
        GlStateManager.color(1f, 1f, 1f, 1f)
        drawTexturedRectNoBlend(width, height)

        GlStateManager.enableBlend()
        blurBufferMain?.bindFramebufferTexture()
        GlStateManager.color(1f, 1f, 1f, blurStrength.toFloat() / 10f - 0.1f)
        drawTexturedRectNoBlend(width, height)

        mc.framebuffer.bindFramebuffer(true)
        blurBufferInto?.bindFramebufferTexture()
        GlStateManager.color(1f, 1f, 1f, 1f)
        OpenGlHelper.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 771)
        drawTexturedRectNoBlend(width, height)

        val tempBuff = blurBufferMain
        blurBufferMain = blurBufferInto
        blurBufferInto = tempBuff
    }

    private fun setupProjectionMatrix(width: Int, height: Int) {
        GlStateManager.matrixMode(GL_PROJECTION)
        GlStateManager.loadIdentity()
        GlStateManager.ortho(0.0, width.toDouble(), height.toDouble(), 0.0, 2000.0, 4000.0)
        GlStateManager.matrixMode(GL_MODELVIEW)
        GlStateManager.loadIdentity()
        GlStateManager.translate(0f, 0f, - 2000f)
    }

    private fun checkFramebufferSizes(framebuffer: Framebuffer?, width: Int, height: Int): Framebuffer {
        return framebuffer?.takeIf { it.framebufferWidth == width && it.framebufferHeight == height }
            ?: Framebuffer(width, height, true).apply {
                createBindFramebuffer(width, height)
                setFramebufferFilter(GL_NEAREST)
            }
    }

    private fun drawTexturedRectNoBlend(width: Int, height: Int) {
        GlStateManager.enableTexture2D()
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer

        worldrenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldrenderer.pos(width.toDouble(), .0, 0.0).tex(1.0, 1.0).endVertex()
        worldrenderer.pos(.0, .0, 0.0).tex(.0, 1.0).endVertex()
        worldrenderer.pos(.0, height.toDouble(), 0.0).tex(.0, .0).endVertex()
        worldrenderer.pos(width.toDouble(), height.toDouble(), 0.0).tex(1.0, .0).endVertex()
        tessellator.draw()

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    }
}