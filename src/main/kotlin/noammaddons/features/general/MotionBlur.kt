package noammaddons.features.general

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.Framebuffer
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.features.Feature
import org.lwjgl.opengl.GL11

/*
 * Copyright (c) 2022 Moulberry
 */
object Motionblur : Feature() {
    private var blurBufferMain: Framebuffer? = null
    private var blurBufferInto: Framebuffer? = null


    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (mc.currentScreen != null) return
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (! OpenGlHelper.isFramebufferEnabled()) return
        if (! config.MotionBlur) return

        val width: Int = mc.framebuffer.framebufferWidth
        val height: Int = mc.framebuffer.framebufferHeight

        GlStateManager.pushMatrix()
        GlStateManager.matrixMode(5889)
        GlStateManager.loadIdentity()
        GlStateManager.ortho(0.0, width.toDouble(), height.toDouble(), 0.0, 2000.0, 4000.0)
        GlStateManager.matrixMode(5888)
        GlStateManager.loadIdentity()
        GlStateManager.translate(0f, 0f, - 2000f)
        blurBufferMain = checkFramebufferSizes(blurBufferMain, width, height)
        blurBufferInto = checkFramebufferSizes(blurBufferInto, width, height)
        blurBufferInto !!.framebufferClear()
        blurBufferInto !!.bindFramebuffer(true)
        OpenGlHelper.glBlendFunc(770, 771, 0, 1)
        GlStateManager.disableLighting()
        GlStateManager.disableFog()
        GlStateManager.disableBlend()
        mc.framebuffer.bindFramebufferTexture()
        GlStateManager.color(1f, 1f, 1f, 1f)
        drawBlurEffect(width.toFloat(), height.toFloat(), 0.0f, 1.0f)
        GlStateManager.enableBlend()
        blurBufferMain !!.bindFramebufferTexture()
        GlStateManager.color(1f, 1f, 1f, config.MotionBlurAmount.toFloat() / 10 - 0.1f)
        drawBlurEffect(width.toFloat(), height.toFloat(), 1f, 0f)
        mc.framebuffer.bindFramebuffer(true)
        blurBufferInto !!.bindFramebufferTexture()
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.enableBlend()
        OpenGlHelper.glBlendFunc(770, 771, 1, 771)
        drawBlurEffect(width.toFloat(), height.toFloat(), 0.0f, 1.0f)
        val tempBuff = blurBufferMain
        blurBufferMain = blurBufferInto
        blurBufferInto = tempBuff
        GlStateManager.popMatrix()
    }


    private fun checkFramebufferSizes(_framebuffer: Framebuffer?, width: Int, height: Int): Framebuffer {
        var framebuffer = _framebuffer
        if (framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            if (framebuffer == null) {
                framebuffer = Framebuffer(width, height, true)
            } else framebuffer.createBindFramebuffer(width, height)

            framebuffer.setFramebufferFilter(9728)
        }

        return framebuffer
    }

    private fun drawBlurEffect(width: Float, height: Float, vMin: Float, vMax: Float) {
        GlStateManager.enableTexture2D()
        GL11.glTexParameteri(3553, 10241, 9728)
        GL11.glTexParameteri(3553, 10240, 9728)
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer

        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX)
        worldrenderer.pos(.0, .0 + height, 0.0).tex(.0, vMax.toDouble()).endVertex()
        worldrenderer.pos(.0 + width, .0 + height, 0.0).tex(.0, .0).endVertex()
        worldrenderer.pos(.0 + width, .0, 0.0).tex(.0, vMin.toDouble()).endVertex()
        worldrenderer.pos(.0, .0, 0.0).tex(.0, vMin.toDouble()).endVertex()
        tessellator.draw()

        GL11.glTexParameteri(3553, 10241, 9728)
        GL11.glTexParameteri(3553, 10240, 9728)
    }
}