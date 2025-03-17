package noammaddons.features.general

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.Framebuffer
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import org.lwjgl.opengl.GL11.*

/*
 * Copyright (c) 2022 Moulberry
 */
object Motionblur: Feature() {
    private var blurBufferMain: Framebuffer? = null
    private var blurBufferInto: Framebuffer? = null

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (! config.MotionBlur) return
        if (getAmount() == 0f) return
        if (mc.currentScreen != null) return
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
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
        GlStateManager.color(1f, 1f, 1f, getAmount() / 10f - 0.1f)
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

    private fun drawTexturedRectNoBlend(
        width: Int, height: Int
    ) {
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


    private fun getAmount(): Float {
        return config.MotionBlurAmount.toFloat()
    }
}
