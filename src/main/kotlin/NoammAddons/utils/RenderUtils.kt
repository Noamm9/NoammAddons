package NoammAddons.utils

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.mixins.AccessorMinecraft
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting
import gg.essential.elementa.components.UIRoundedRectangle.Companion.drawRoundedRectangle
import gg.essential.universal.UMatrixStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.glu.Cylinder
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


object RenderUtils {
	private val renderManager: RenderManager
		get() = mc.renderManager ?: throw IllegalStateException("RenderManager is not initialized!")
	
	private val tessellator = Tessellator.getInstance()
	private val worldRenderer: WorldRenderer = tessellator.worldRenderer
	private val regCylinder = Cylinder()
	private val lineCylinder = Cylinder().apply { drawStyle = GL_LINE }
	
	
	private fun preDraw() {
		GlStateManager.enableAlpha()
		GlStateManager.enableBlend()
		GlStateManager.disableTexture2D()
		GlStateManager.tryBlendFuncSeparate(
			770,
			771,
			1,
			0
		)
	}
	
	private fun postDraw() {
		GlStateManager.disableBlend()
		GlStateManager.enableTexture2D()
	}
	
	fun Minecraft.getPartialTicks() =
		(this as AccessorMinecraft).timer.renderPartialTicks
	
	fun EntityPlayer.getRenderX(): Double =
		lastTickPosX + (posX - lastTickPosX) * mc.getPartialTicks()
	
	fun EntityPlayer.getRenderY(): Double =
		lastTickPosY + (posY - lastTickPosY) * mc.getPartialTicks()
	
	fun EntityPlayer.getRenderZ(): Double =
		lastTickPosZ + (posZ - lastTickPosZ) * mc.getPartialTicks()
	
	fun Entity.getRenderX(): Double =
		lastTickPosX + (posX - lastTickPosX) * mc.getPartialTicks()
	
	fun Entity.getRenderY(): Double =
		lastTickPosY + (posY - lastTickPosY) * mc.getPartialTicks()
	
	fun Entity.getRenderZ(): Double =
		lastTickPosZ + (posZ - lastTickPosZ) * mc.getPartialTicks()
	
	fun Minecraft.getWidth(): Int =
		ScaledResolution(this).scaledWidth
	
	fun Minecraft.getHeight(): Int =
		ScaledResolution(this).scaledHeight
	
	
	private fun drawFilledAABB(aabb: AxisAlignedBB, c: Color, alphaMultiplier: Float = 1f) {
		GlStateManager.color(
			c.red / 255f,
			c.green / 255f,
			c.blue / 255f,
			c.alpha / 255f * alphaMultiplier
		)
		
		// vertical
		worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
		tessellator.draw()
		
		worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
		tessellator.draw()
		GlStateManager.color(
			c.red / 255f * 0.8f,
			c.green / 255f * 0.8f,
			c.blue / 255f * 0.8f,
			c.alpha / 255f * alphaMultiplier,
		)
		
		// x
		worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
		tessellator.draw()
		
		worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
		tessellator.draw()
		
		GlStateManager.color(
			c.red / 255f * 0.9f,
			c.green / 255f * 0.9f,
			c.blue / 255f * 0.9f,
			c.alpha / 255f * alphaMultiplier,
		)
		
		// z
		worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
		tessellator.draw()
		
		worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
		tessellator.draw()
	}
	
	private fun drawOutlinedAABB(aabb: AxisAlignedBB, color: Color) {
		
		GlStateManager.color(
			color.red / 255f,
			color.green / 255f,
			color.blue / 255f,
			1f
		)
		
		worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX,aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
		tessellator.draw()
		
		GlStateManager.color(
			color.red / 255f,
			color.green / 255f,
			color.blue / 255f,
			1f
		)
		
		worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
		tessellator.draw()
		
		GlStateManager.color(
			color.red / 255f,
			color.green / 255f,
			color.blue / 255f,
			1f
		)
		
		worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer
			.pos(
				aabb.maxX,
				aabb.maxY,
				aabb.minZ
			)
			.endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
		tessellator.draw()
		
		GlStateManager.color(
			color.red / 255f,
			color.green / 255f,
			color.blue / 255f,
			1f
		)
		
		worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
		tessellator.draw()
		
	}
	
	fun drawBlockBox(
		blockPos: BlockPos, color: Color, outline: Boolean, fill: Boolean, phase: Boolean = true, LineThickness: Float = 3f
	) {
		if (! outline && ! fill) throw IllegalArgumentException("outline and fill cannot both be false")
		
		GlStateManager.pushMatrix()
		preDraw()
		
		if (phase) {
			glDisable(GL_DEPTH_TEST)
			glDepthMask(false)
		}
		
		val x = blockPos.x.toDouble()
		val y = blockPos.y.toDouble()
		val z = blockPos.z.toDouble()
		
		var axisAlignedBB = AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1)
		val block = mc.theWorld.getBlockState(blockPos).block
		
		if (block != null) {
			block.setBlockBoundsBasedOnState(mc.theWorld, blockPos)
			axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
				.expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)
				.offset(- renderManager.viewerPosX, - renderManager.viewerPosY, - renderManager.viewerPosZ)
		}
		
		if (fill) drawFilledAABB(axisAlignedBB, color)
		
		if (outline) {
			glLineWidth(LineThickness)
			drawOutlinedAABB(axisAlignedBB, color)
		}
		
		if (phase) {
			glEnable(GL_DEPTH_TEST)
			glDepthMask(true)
		}
		
		postDraw()
		GlStateManager.popMatrix()
	}
	
	fun drawEntityBox(entity: Entity, color: Color, outline: Boolean, fill: Boolean) {
		if (! outline && ! fill) return
		val x = entity.getRenderX() - renderManager.viewerPosX
		val y = entity.getRenderY() - renderManager.viewerPosY
		val z = entity.getRenderZ() - renderManager.viewerPosZ
		
		var axisAlignedBB: AxisAlignedBB
		entity.entityBoundingBox.run {
			axisAlignedBB = AxisAlignedBB(
				minX - entity.posX,
				minY - entity.posY,
				minZ - entity.posZ,
				maxX - entity.posX,
				maxY - entity.posY,
				maxZ - entity.posZ
			).offset(x, y, z)
		}
		
		glPushMatrix()
		glPushAttrib(GL_ALL_ATTRIB_BITS)
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
		glEnable(GL_BLEND)
		glDisable(GL_TEXTURE_2D)
		glDisable(GL_DEPTH_TEST)
		glDisable(GL_LIGHTING)
		glDepthMask(false)
		
		if (outline) {
			glLineWidth(config.espBoxOutlineWidth)
			drawOutlinedAABB(axisAlignedBB, color)
		}
		if (fill) {
			drawFilledAABB(axisAlignedBB, color)
		}
		
		glDepthMask(true)
		glPopAttrib()
		glPopMatrix()
	}
	
	fun drawString(
		text: String, pos: Vec3, color: Color, scale: Float = 1f, shadow: Boolean = true, phase: Boolean = true
	) {
		val f1 = 0.0266666688
		val width = mc.fontRendererObj.getStringWidth(text) / 2
		GlStateManager.pushMatrix()
		GlStateManager.translate(
			pos.xCoord - renderManager.viewerPosX,
			pos.yCoord - renderManager.viewerPosY,
			pos.zCoord - renderManager.viewerPosZ
		)
		
		glNormal3f(0f, 1f, 0f)
		
		if (mc.gameSettings.thirdPersonView != 2) {
			GlStateManager.rotate(- renderManager.playerViewY, 0f, 1f, 0f)
			GlStateManager.rotate(renderManager.playerViewX, 1f, 0f, 0f)
		}
		else {
			GlStateManager.rotate(- renderManager.playerViewY, 0f, 1f, 0f)
			GlStateManager.rotate(renderManager.playerViewX, - 1f, 0f, 0f)
		}
		
		GlStateManager.scale(- f1, - f1, - f1)
		GlStateManager.scale(scale, scale, scale)
		GlStateManager.enableBlend()
		GlStateManager.disableLighting()
		if (phase) {
			glDisable(GL_DEPTH_TEST)
			glDepthMask(false)
		}
		
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
		GlStateManager.enableTexture2D()
		mc.fontRendererObj.drawString(text, (- width).toFloat(), 0f, color.rgb, shadow)
		GlStateManager.disableBlend()
		if (phase) {
			glEnable(GL_DEPTH_TEST)
			glDepthMask(true)
		}
		GlStateManager.popMatrix()
	}
	
	fun draw3DLine(from: Vec3, to: Vec3, color: Color, LineWidth: Int = 6) {
		GlStateManager.pushMatrix()
		GlStateManager.enableBlend()
		GlStateManager.disableDepth()
		GlStateManager.disableLighting()
		GlStateManager.disableTexture2D()
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
		
		val renderPosX = to.xCoord - renderManager.viewerPosX
		val renderPosY = to.yCoord - renderManager.viewerPosY
		val renderPosZ = to.zCoord - renderManager.viewerPosZ
		
		glLineWidth(LineWidth.toFloat())
		glColor4f(
			color.red / 255f,
			color.green / 255f,
			color.blue / 255f,
			color.alpha / 255f
		)
		glBegin(GL_LINES)
		glVertex3d(from.xCoord, from.yCoord, from.zCoord)
		glVertex3d(renderPosX, renderPosY, renderPosZ)
		glEnd()
		glLineWidth(1.0f)
		
		GlStateManager.enableTexture2D()
		GlStateManager.enableDepth()
		GlStateManager.disableBlend()
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
		GlStateManager.resetColor()
		GlStateManager.popMatrix()
	}
	
	fun drawTracer(pos: Vec3, color: Color, lineWidth: Int = 3) {
		val player = mc.thePlayer
		val playerVec3 = Vec3(
			player.renderOffsetX + renderManager.viewerPosX,
			player.renderOffsetY + player.getEyeHeight() + renderManager.viewerPosY,
			player.renderOffsetZ + renderManager.viewerPosZ
		)
		
		val startX = (playerVec3.xCoord - renderManager.viewerPosX).toFloat()
		val startY = (playerVec3.yCoord - renderManager.viewerPosY).toFloat()
		val startZ = (playerVec3.zCoord - renderManager.viewerPosZ).toFloat()
		
		val endX = (pos.xCoord - renderManager.viewerPosX).toFloat()
		val endY = (pos.yCoord - renderManager.viewerPosY).toFloat()
		val endZ = (pos.zCoord - renderManager.viewerPosZ).toFloat()
		
		glPushMatrix()
		glDisable(GL_TEXTURE_2D)
		glEnable(GL_LINE_SMOOTH)
		glLineWidth(lineWidth.toFloat())
		
		glColor3f(
			color.red / 255.0f,
			color.green / 255.0f,
			color.blue / 255.0f
		)
		
		glBegin(GL_LINES)
		glVertex3f(startX, startY, startZ)
		glVertex3f(endX, endY, endZ)
		glEnd()
		
		glEnable(GL_TEXTURE_2D)
		glDisable(GL_LINE_SMOOTH)
		glPopMatrix()
	}
	
	fun drawText(text: String, x: Double, y: Double, scale: Double = 1.0, color: Color = Color.WHITE) {
		GlStateManager.pushMatrix()
		GlStateManager.scale(scale, scale, 1.0)
		
		var yOffset = y - (mc.fontRendererObj.FONT_HEIGHT) / 2
		val formattedText = text.addColor()
		if (formattedText.contains("\n")) {
			formattedText.split("\n").forEach {
				yOffset += (mc.fontRendererObj.FONT_HEIGHT * scale).toInt()
				mc.fontRendererObj.drawStringWithShadow(
					it,
					(x / scale).toFloat(),
					(yOffset / scale).toFloat(),
					color.rgb
				)
			}
		}
		else {
			mc.fontRendererObj.drawStringWithShadow(
				formattedText,
				(x / scale).toFloat(),
				(y / scale).toFloat(),
				color.rgb
			)
		}
		GlStateManager.popMatrix()
	}
	
	fun drawCenteredText(text: String, x: Double, y: Double, scale: Double = 1.0, color: Color = Color.WHITE) {
		drawText(text, x - (mc.fontRendererObj.getStringWidth(text.addColor().removeFormatting())*scale/2), y, scale, color)
	}
	
	fun renderItem(itemStack: ItemStack?, x: Int, y: Int) {
		RenderHelper.enableGUIStandardItemLighting()
		GlStateManager.enableDepth()
		mc.renderItem.renderItemAndEffectIntoGUI(itemStack, x, y)
	}
	
	fun renderTexture(
		texture: ResourceLocation?, x: Int, y: Int, width: Int = 16, height: Int = 16, enableLighting: Boolean = true
	) {
		if (enableLighting) RenderHelper.enableGUIStandardItemLighting()
		GlStateManager.enableRescaleNormal()
		GlStateManager.enableBlend()
		GlStateManager.enableDepth()
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
		GlStateManager.pushMatrix()
		mc.textureManager.bindTexture(texture)
		GlStateManager.enableRescaleNormal()
		GlStateManager.enableAlpha()
		GlStateManager.alphaFunc(516, 0.1f)
		GlStateManager.enableBlend()
		GlStateManager.blendFunc(770, 771)
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
		
		Gui.drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, width, height, width.toFloat(), height.toFloat())
		
		GlStateManager.disableAlpha()
		GlStateManager.disableRescaleNormal()
		GlStateManager.disableLighting()
		GlStateManager.popMatrix()
	}
	
	/**
	 * Draws a 3D cylinder in Minecraft using OpenGL.
	 *
	 * @param x X Coordinates
	 * @param y Y Coordinates
	 * @param z Z Coordinates
	 * @param baseRadius Radius of the bottom of the cylinder.
	 * @param topRadius Radius of the top of the cylinder.
	 * @param height Height of the cylinder.
	 * @param slices Slices in the cylinder.
	 * @param stacks Stacks in the cylinder.
	 * @param rot1 Rotation on the X axis.
	 * @param rot2 Rotation on the Y axis.
	 * @param rot3 Rotation on the Z axis.
	 * @param r Color Red (0-1)
	 * @param g Color Green (0-1)
	 * @param b Color Blue (0-1)
	 * @param a Alpha (0-1)
	 * @param phase Depth test disabled (true = see through walls)
	 * @param linemode True: the frame of the cylinder is visible, False: the filled cylinder is visible.
	 */
	fun drawCylinder(
		BlockPos: BlockPos,
		baseRadius: Float, topRadius: Float, height: Float,
		slices: Int, stacks: Int,
		rot1: Float, rot2: Float, rot3: Float,
		color: Color,
		phase: Boolean, linemode: Boolean
	) {
		val player = mc.thePlayer
		
		val renderX = BlockPos.x - player.getRenderX()
		val renderY = BlockPos.y - player.getRenderY()
		val renderZ = BlockPos.z - player.getRenderZ()
		
		GlStateManager.pushMatrix()
		glLineWidth(2.0f)
		GlStateManager.disableCull()
		GlStateManager.enableBlend()
		GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
		GlStateManager.depthMask(false)
		GlStateManager.disableTexture2D()
		GlStateManager.color(
			color.red / 255f,
			color.green / 255f,
			color.blue / 255f,
			color.alpha / 255f
		)
		GlStateManager.translate(renderX, renderY, renderZ)
		GlStateManager.rotate(rot1, 1.0f, 0.0f, 0.0f)
		GlStateManager.rotate(rot2, 0.0f, 0.0f, 1.0f)
		GlStateManager.rotate(rot3, 0.0f, 1.0f, 0.0f)
		
		if (phase) GlStateManager.disableDepth()
		
		if (linemode) lineCylinder.draw(baseRadius, topRadius, height, slices, stacks)
		else regCylinder.draw(baseRadius, topRadius, height, slices, stacks)
		
		GlStateManager.enableCull()
		GlStateManager.disableBlend()
		GlStateManager.depthMask(true)
		GlStateManager.enableTexture2D()
		GlStateManager.enableDepth()
		GlStateManager.popMatrix()
	}
	
	fun drawRoundedRect(color: Color, x: Double, y: Double, width: Double, height: Double, radius: Double = 5.0) {
		drawRoundedRectangle(
			UMatrixStack(),
			x.toFloat(),
			y.toFloat(),
			(x + width).toFloat(),
			(y + height).toFloat(),
			radius.toFloat(),
			color
		)
	}
	
	fun drawPlayerHead(
		resourceLocation: ResourceLocation, x: Double, y: Double, width: Double, height: Double, radius: Double = 10.0
	) {
		GlStateManager.pushMatrix()
		GlStateManager.disableLighting()
		GlStateManager.enableTexture2D()
		GlStateManager.enableBlend()
		GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
		GlStateManager.translate(x + width / 2, y + height / 2, 0.0)
		
		glEnable(GL_STENCIL_TEST)
		glClear(GL_STENCIL_BUFFER_BIT)
		
		glStencilFunc(GL_ALWAYS, 1, 0xFF)
		glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
		glStencilMask(0xFF)
		
		drawRoundedRect(Color.WHITE, - width / 2, - height / 2, width, height, radius)
		
		glStencilFunc(GL_EQUAL, 1, 0xFF)
		glStencilMask(0x00)
		
		mc.textureManager.bindTexture(resourceLocation)
		
		worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
		worldRenderer.pos(- width / 2, height / 2, 0.0).tex(8.0 / 64.0, 16.0 / 64.0).endVertex()
		worldRenderer.pos(width / 2, height / 2, 0.0).tex(16.0 / 64.0, 16.0 / 64.0).endVertex()
		worldRenderer.pos(width / 2, - height / 2, 0.0).tex(16.0 / 64.0, 8.0 / 64.0).endVertex()
		worldRenderer.pos(- width / 2, - height / 2, 0.0).tex(8.0 / 64.0, 8.0 / 64.0).endVertex()
		tessellator.draw()
		
		worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
		worldRenderer.pos(- width / 2, height / 2, 0.0).tex(40.0 / 64.0, 16.0 / 64.0).endVertex()
		worldRenderer.pos(width / 2, height / 2, 0.0).tex(48.0 / 64.0, 16.0 / 64.0).endVertex()
		worldRenderer.pos(width / 2, - height / 2, 0.0).tex(48.0 / 64.0, 8.0 / 64.0).endVertex()
		worldRenderer.pos(- width / 2, - height / 2, 0.0).tex(40.0 / 64.0, 8.0 / 64.0).endVertex()
		tessellator.draw()
		
		glStencilMask(0xFF)
		glDisable(GL_STENCIL_TEST)
		GlStateManager.disableBlend()
		GlStateManager.enableLighting()
		GlStateManager.popMatrix()
	}
	
	
	fun drawLine(color: Color, x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float) {
		val theta = - atan2(y2 - y1, x2 - x1)
		val i = sin(theta) * (thickness / 2)
		val j = cos(theta) * (thickness / 2)
		
		GlStateManager.pushMatrix()
		GlStateManager.enableBlend()
		GlStateManager.disableTexture2D()
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
		GlStateManager.color(
			color.red / 255f,
			color.green / 255f,
			color.blue / 255f,
			color.alpha / 255f
		)
		
		worldRenderer.begin(7, DefaultVertexFormats.POSITION)
		worldRenderer.pos((x1 + i).toDouble(), (y1 + j).toDouble(), 0.0).endVertex()
		worldRenderer.pos((x2 + i).toDouble(), (y2 + j).toDouble(), 0.0).endVertex()
		worldRenderer.pos((x2 - i).toDouble(), (y2 - j).toDouble(), 0.0).endVertex()
		worldRenderer.pos((x1 - i).toDouble(), (y1 - j).toDouble(), 0.0).endVertex()
		tessellator.draw()
		
		GlStateManager.color(1f, 1f, 1f, 1f)
		GlStateManager.enableTexture2D()
		GlStateManager.disableBlend()
		GlStateManager.popMatrix()
	}
	
	fun drawRoundedBorder(
		color: Color, x: Double, y: Double, width: Double, height: Double, radius: Double = 5.0, thickness: Double = 2.0
	) {
		GlStateManager.pushMatrix()
		GlStateManager.disableLighting()
		GlStateManager.disableTexture2D()
		glColor4f(
			color.red / 255f,
			color.green / 255f,
			color.blue / 255f,
			color.alpha / 255f
		)
		
		glLineWidth(thickness.toFloat())
		glBegin(GL_LINE_LOOP)
		
		// Top-left corner
		drawRoundedCorner(x + radius, y + radius, radius, 180.0, 270.0)
		
		// Top-right corner
		drawRoundedCorner(x + width - radius, y + radius, radius, 270.0, 360.0)
		
		// Bottom-right corner
		drawRoundedCorner(x + width - radius, y + height - radius, radius, 0.0, 90.0)
		
		// Bottom-left corner
		drawRoundedCorner(x + radius, y + height - radius, radius, 90.0, 180.0)
		
		glEnd()
		glColor4f(1f, 1f, 1f, 1f)
		GlStateManager.enableLighting()
		GlStateManager.enableTexture2D()
		GlStateManager.popMatrix()
	}
	
	private fun drawRoundedCorner(
		cx: Double, cy: Double, radius: Double, startAngle: Double, endAngle: Double, segments: Int = 16
	) {
		val angleStep = Math.toRadians(endAngle - startAngle) / segments
		
		for (i in 0 .. segments) {
			val angle = Math.toRadians(startAngle) + i * angleStep
			val x2 = cx + Math.cos(angle) * radius
			val y2 = cy + Math.sin(angle) * radius
			glVertex2d(x2, y2)
		}
	}
}
