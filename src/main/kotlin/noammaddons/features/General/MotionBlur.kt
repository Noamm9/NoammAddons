package noammaddons.features.General

import noammaddons.noammaddons.Companion.mc
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.Framebuffer
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.noammaddons.Companion.config
import org.lwjgl.opengl.GL11


object Motionblur {
	private var blurBufferMain: Framebuffer? = null
	private var blurBufferInto: Framebuffer? = null
	
	
	@SubscribeEvent
	fun renderOverlay(event: RenderGameOverlayEvent.Post) {
		if (mc.currentScreen != null) return
		if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
		if (!OpenGlHelper.isFramebufferEnabled()) return
		if (!config.MotionBlur) return

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
		drawTexturedRectNoBlend(0f, 0f, width.toFloat(), height.toFloat(), 0f, 1f, 0.0f, 1.0f, 9728)
		GlStateManager.enableBlend()
		blurBufferMain !!.bindFramebufferTexture()
		GlStateManager.color(1f, 1f, 1f, config.MotionBlurAmount.toFloat() / 10 - 0.1f)
		drawTexturedRectNoBlend(0f, 0f, width.toFloat(), height.toFloat(), 0f, 1f, 1f, 0f, 9728)
		mc.framebuffer.bindFramebuffer(true)
		blurBufferInto !!.bindFramebufferTexture()
		GlStateManager.color(1f, 1f, 1f, 1f)
		GlStateManager.enableBlend()
		OpenGlHelper.glBlendFunc(770, 771, 1, 771)
		drawTexturedRectNoBlend(0.0f, 0.0f, width.toFloat(), height.toFloat(), 0.0f, 1.0f, 0.0f, 1.0f, 9728)
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
			}
			else framebuffer.createBindFramebuffer(width, height)
			
			framebuffer.setFramebufferFilter(9728)
		}
		
		return framebuffer
	}
		
	private fun drawTexturedRectNoBlend(x: Float, y: Float, width: Float, height: Float, uMin: Float, uMax: Float, vMin: Float, vMax: Float, filter: Int) {
		GlStateManager.enableTexture2D()
		GL11.glTexParameteri(3553, 10241, filter)
		GL11.glTexParameteri(3553, 10240, filter)
		val tessellator = Tessellator.getInstance()
		val worldrenderer = tessellator.worldRenderer
		
		worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX)
		worldrenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0).tex(uMin.toDouble(), vMax.toDouble()).endVertex()
		worldrenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0).tex(uMax.toDouble(), vMax.toDouble()).endVertex()
		worldrenderer.pos((x + width).toDouble(), y.toDouble(), 0.0).tex(uMax.toDouble(), vMin.toDouble()).endVertex()
		worldrenderer.pos(x.toDouble(), y.toDouble(), 0.0).tex(uMin.toDouble(), vMin.toDouble()).endVertex()
		tessellator.draw()
		GL11.glTexParameteri(3553, 10241, 9728)
		GL11.glTexParameteri(3553, 10240, 9728)
	}
}