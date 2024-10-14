package noammaddons.utils

import gg.essential.elementa.components.UIRoundedRectangle.Companion.drawRoundedRectangle
import gg.essential.universal.UMatrixStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.*
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.Config.espBoxOutlineWidth
import noammaddons.mixins.AccessorMinecraft
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.PlayerUtils.Player
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.glu.Cylinder
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


object RenderUtils {
	private val renderManager: RenderManager get() = mc.renderManager
	private val tessellator = Tessellator.getInstance()
	private val worldRenderer: WorldRenderer = tessellator.worldRenderer
	private val regCylinder = Cylinder()
	private val lineCylinder = Cylinder().apply { drawStyle = GL_LINE }
	
	fun bindColor(color: Color, alpha: Number = color.alpha) {
		GlStateManager.color(
			color.red/255f,
			color.green/255f,
			color.blue/255f,
			alpha.toFloat()/255f
		)
	}
	
	fun Minecraft.getPartialTicks() = (this as AccessorMinecraft).timer.renderPartialTicks
	
	fun Entity.getRenderX(): Float = (lastTickPosX + (posX - lastTickPosX) * mc.getPartialTicks()).toFloat()
	fun Entity.getRenderY(): Float = (lastTickPosY + (posY - lastTickPosY) * mc.getPartialTicks()).toFloat()
	fun Entity.getRenderZ(): Float = (lastTickPosZ + (posZ - lastTickPosZ) * mc.getPartialTicks()).toFloat()
	fun Entity.getRenderVec(): Vec3 = Vec3(getRenderX().toDouble(), getRenderY().toDouble(), getRenderZ().toDouble())
	
	fun Minecraft.getWidth(): Int = ScaledResolution(this).scaledWidth
	fun Minecraft.getHeight(): Int = ScaledResolution(this).scaledHeight
	
	
	private fun drawFilledAABB(aabb: AxisAlignedBB, c: Color) {
		bindColor(c)
		
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
		bindColor(c)
		
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
		
		bindColor(c)
		
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
		bindColor(color, 255)
		
		worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX,aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex()
		tessellator.draw()
		
		bindColor(color, 255)
		
		worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex()
		tessellator.draw()
		
		bindColor(color, 255)
		
		worldRenderer.begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
		worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex()
		worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex()
		tessellator.draw()
		
		bindColor(color, 255)
		
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
		blockPos: BlockPos,
		color: Color,
		outline: Boolean,
		fill: Boolean,
		phase: Boolean = true,
		LineThickness: Float = 3f
	) {
		if (!outline && !fill) throw IllegalArgumentException("outline and fill cannot both be false")
		
		GlStateManager.pushMatrix()
		GlStateManager.enableAlpha()
		GlStateManager.enableBlend()
		GlStateManager.disableTexture2D()
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
		
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
		
		GlStateManager.disableBlend()
		GlStateManager.enableTexture2D()
		GlStateManager.popMatrix()
	}
	
	fun drawEntityBox(entity: Entity, color: Color, outline: Boolean, fill: Boolean, lineThickness: Float = espBoxOutlineWidth) {
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
			glLineWidth(lineThickness)
			drawOutlinedAABB(axisAlignedBB, color)
		}
		
		if (fill) drawFilledAABB(axisAlignedBB, color)
		
		glDepthMask(true)
		glEnable(GL_TEXTURE_2D)
		glPopAttrib()
		glPopMatrix()
	}
	
	fun drawBox(
		x: Number, y: Number, z: Number,
		color: Color,
		outline: Boolean, fill: Boolean,
		width: Number = 1f, height: Number = 1f,
		phase: Boolean = true, LineThickness: Number = 3f
	) {
		if (!outline && !fill) throw IllegalArgumentException("outline and fill cannot both be false")
		
		GlStateManager.pushMatrix()
		GlStateManager.enableAlpha()
		GlStateManager.enableBlend()
		GlStateManager.disableTexture2D()
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
		
		if (phase) {
			glDisable(GL_DEPTH_TEST)
			glDepthMask(false)
		}
		
		val axisAlignedBB = AxisAlignedBB(
			x.toDouble(), y.toDouble(), z.toDouble(),
			x.toDouble() + width.toDouble(),
			y.toDouble() + height.toDouble(),
			z.toDouble() + width.toDouble()
		)
			.expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)
			.offset(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
		
		if (fill) drawFilledAABB(axisAlignedBB, color)
		
		if (outline) {
			glLineWidth(LineThickness.toFloat())
			drawOutlinedAABB(axisAlignedBB, color)
		}
		
		if (phase) {
			glEnable(GL_DEPTH_TEST)
			glDepthMask(true)
		}
		
		GlStateManager.disableBlend()
		GlStateManager.enableTexture2D()
		GlStateManager.popMatrix()
	}
	
	fun drawBox(
		from: Vec3, to: Vec3,
		color: Color,
		outline: Boolean, fill: Boolean,
		phase: Boolean = true, LineThickness: Number = 3f
	) {
		drawBox(
			from.xCoord.toFloat(), from.yCoord.toFloat(), from.zCoord.toFloat(),
			color, outline, fill,
			width = to.xCoord.toFloat() - from.xCoord.toFloat(), height = to.yCoord.toFloat() - from.yCoord.toFloat(),
			phase = phase, LineThickness = LineThickness
		)
	}
	
	
	fun drawString(
		text: String,
		pos: Vec3,
		color: Color,
		scale: Float = 1f,
		renderBlackBox: Boolean = true,
		shadow: Boolean = true,
		phase: Boolean = true
	) {
		drawString(
			text = text,
			x = pos.xCoord,
            y = pos.yCoord,
            z = pos.zCoord,
            color = color,
			renderBlackBox = renderBlackBox,
            scale = scale,
			shadow = shadow,
			phase = phase
		)
	}
	
	fun drawString(
		 text: String,
		 x: Number, y: Number, z: Number,
		 color: Color,
		 scale: Number = 1f,
		 renderBlackBox: Boolean = false,
		 shadow: Boolean = true,
		 phase: Boolean = false
	) {
		 val formatedText = text.addColor()
		 val isThirdPerson = mc.gameSettings.thirdPersonView == 2
		
		 GlStateManager.color(1f, 1f, 1f, 0.5f)
		 GlStateManager.pushMatrix()
		 GlStateManager.translate(
			 -renderManager.viewerPosX + x.toDouble(),
			 -renderManager.viewerPosY + y.toDouble(),
			 -renderManager.viewerPosZ + z.toDouble()
		 )
		 GlStateManager.rotate(-mc.renderManager.playerViewY, 0f, 1f, 0f)
		 GlStateManager.rotate(mc.renderManager.playerViewX * if (isThirdPerson) -1 else 1, 1f, 0f, 0f)
		 GlStateManager.scale(-scale.toDouble()/25, -scale.toDouble()/25, scale.toDouble()/25)
		 GlStateManager.disableLighting()
		 GlStateManager.depthMask(false)
		
		 if (phase) GlStateManager.disableDepth()
		
		 GlStateManager.enableBlend()
		 GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
		
		 val lines = formatedText.split("\n")
		 val maxWidth = lines.maxOf { mc.fontRendererObj.getStringWidth(it) } / 2
		
		 if (renderBlackBox) {
			 GlStateManager.disableTexture2D()
			 worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR)
			 worldRenderer.pos(-maxWidth - 1.0, -1.0, 0.0).color(0, 0, 0, 64).endVertex()
			 worldRenderer.pos(-maxWidth - 1.0, 9.0 * lines.size, 0.0).color(0, 0, 0, 64).endVertex()
			 worldRenderer.pos(maxWidth + 1.0, 9.0 * lines.size, 0.0).color(0, 0, 0, 64).endVertex()
			 worldRenderer.pos(maxWidth + 1.0, -1.0, 0.0).color(0, 0, 0, 64).endVertex()
			 tessellator.draw()
			 GlStateManager.enableTexture2D()
		 }
		
		lines.forEachIndexed { i, line ->
			mc.fontRendererObj.drawString(
				line,
				-mc.fontRendererObj.getStringWidth(line)/2f,
				i * 9f,
				color.rgb, shadow
			)
		}
		
		GlStateManager.color(1f, 1f, 1f, 1f)
		GlStateManager.depthMask(true)
		GlStateManager.enableDepth()
		GlStateManager.popMatrix()
	}
	
	fun draw3DLine(from: Vec3, to: Vec3, color: Color, LineWidth: Float = 6f) {
		GlStateManager.pushMatrix()
		GlStateManager.enableBlend()
		GlStateManager.disableDepth()
		GlStateManager.disableLighting()
		GlStateManager.disableTexture2D()
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
		
		glTranslated(-renderManager.viewerPosX, -renderManager.viewerPosY, -renderManager.viewerPosZ)
		glLineWidth(LineWidth)
		bindColor(color)
		glBegin(GL_LINES)
		glVertex3d(from.xCoord, from.yCoord, from.zCoord)
		glVertex3d(to.xCoord, to.yCoord, to.zCoord)
		glEnd()
		glLineWidth(1.0f)
		
		GlStateManager.enableTexture2D()
		GlStateManager.enableDepth()
		GlStateManager.disableBlend()
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
		GlStateManager.resetColor()
		GlStateManager.popMatrix()
	}
	
	fun drawTracer(pos: Vec3, color: Color, lineWidth: Float = 3f) {
		val player = Player!!
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
		glLineWidth(lineWidth)
		
		bindColor(color, 255)
		
		glBegin(GL_LINES)
		glVertex3f(startX, startY, startZ)
		glVertex3f(endX, endY, endZ)
		glEnd()
		
		glEnable(GL_TEXTURE_2D)
		glDisable(GL_LINE_SMOOTH)
		glPopMatrix()
	}
	
	fun drawText(text: String, x: Float, y: Float, scale: Float = 1f, color: Color = Color.WHITE) {
		GlStateManager.pushMatrix()
		GlStateManager.enableBlend()
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
		GlStateManager.scale(scale, scale, 1f)
		
		var yOffset = y
		val formattedText = text.addColor()
		if (formattedText.contains("\n")) {
			formattedText.split("\n").forEach {
				yOffset += mc.fontRendererObj.FONT_HEIGHT * scale
				mc.fontRendererObj.drawStringWithShadow(
					it,
					x / scale,
					yOffset / scale,
					color.rgb
				)
			}
		}
		else {
			mc.fontRendererObj.drawStringWithShadow(
				formattedText,
				x / scale,
				yOffset / scale,
				color.rgb
			)
		}
		
		GlStateManager.disableBlend()
		GlStateManager.popMatrix()
	}
	
	fun drawCenteredText(text: String, x: Number, y: Number, scale: Number = 1f, color: Color = Color.WHITE) {
		drawText(
			text,
			x.toFloat() - (mc.fontRendererObj.getStringWidth(text.removeFormatting()) * scale.toFloat() / 2),
			y.toFloat(),
			scale.toFloat(),
			color
		)
	}
	
	
	fun drawChromaWaveText(
		text: String,
		x: Float, y: Float,
		scale: Float = 1f,
		waveSpeed: Float = 4f,
	) {
		val string = text.removeFormatting()
		val time = System.currentTimeMillis()
		val chromaWidth = mc.fontRendererObj.getStringWidth(string)
		val speed = waveSpeed*1000.0
		var xPos = x
		
		
		for (i in string.indices) {
			val charWidth = mc.fontRendererObj.getCharWidth(text[i])
			
			val waveOffset = i.toFloat() / chromaWidth
			val hue = ((time % speed) / speed + waveOffset) % 1f
			
			val waveHue = (hue + (sin(waveOffset * Math.PI) * 0.1f)).toFloat() % 1f
			
			val startColor = Color.getHSBColor(waveHue, 1.0f, 1.0f)
			//val endColor = Color.getHSBColor((waveHue + 0.1f) % 1f, 1.0f, 1.0f)
			
			drawText(
				text[i].toString(),
				xPos, y,
				scale,
				startColor
			)
			
			xPos += charWidth * scale.toInt()
		}
	}
	
	fun drawCenteredChromaWaveText(
		text: String,
		x: Float, y: Float,
		scale: Float = 1f,
		waveSpeed: Float = 4f
	) {
		drawChromaWaveText(
			text,
			x - (mc.fontRendererObj.getStringWidth(text.removeFormatting())*scale/2),
			y, scale, waveSpeed
		)
	}
	
	fun renderItem(itemStack: ItemStack?, x: Int, y: Int) {
		RenderHelper.enableGUIStandardItemLighting();
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableBlend();
		GlStateManager.enableDepth();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		mc.renderItem.renderItemAndEffectIntoGUI(itemStack, x, y);
	}
	
	
	
	
	fun renderTexture(texture: ResourceLocation?, x: Int, y: Int, w: Int, h: Int) {
		GlStateManager.pushMatrix()
		GlStateManager.enableBlend();
		GlStateManager.disableLighting();
		
		mc.textureManager.bindTexture(texture);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, w, h, w.toFloat(), h.toFloat())
		
		GlStateManager.disableBlend();
		GlStateManager.popMatrix()
	}
	
	/**
	 * Draws a 3D cylinder in Minecraft using OpenGL.
	 *
	 * @param BlockPos BlockPos
	 * @param baseRadius Radius of the bottom of the cylinder.
	 * @param topRadius Radius of the top of the cylinder.
	 * @param height Height of the cylinder.
	 * @param slices Slices in the cylinder.
	 * @param stacks Stacks in the cylinder.
	 * @param rot1 Rotation on the X axis.
	 * @param rot2 Rotation on the Y axis.
	 * @param rot3 Rotation on the Z axis.
	 * @param color Color
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
		val player = Player!!
		
		val renderX = BlockPos.x - player.getRenderX()
		val renderY = BlockPos.y - player.getRenderY()
		val renderZ = BlockPos.z - player.getRenderZ()
		
		GlStateManager.pushMatrix()
		glLineWidth(2f)
		GlStateManager.disableCull()
		GlStateManager.enableBlend()
		GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
		GlStateManager.depthMask(false)
		GlStateManager.disableTexture2D()
		bindColor(color)
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
	
	fun drawRoundedRect(color: Color, x: Number, y: Number, width: Number, height: Number, radius: Number = 5f) {
		drawRoundedRectangle(
			UMatrixStack(),
			x.toFloat(), y.toFloat(),
			x.toFloat() + width.toFloat(),
			y.toFloat() + height.toFloat(),
			radius.toFloat(),
			color
		)
	}
	
	fun drawPlayerHead(
		resourceLocation: ResourceLocation, x: Float, y: Float, width: Float, height: Float, radius: Float = 10f
	) {
		GlStateManager.pushMatrix()
		GlStateManager.disableLighting()
		GlStateManager.enableBlend()
		GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
		GlStateManager.translate(x + width / 2, y + height / 2, 0f)
		
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
		worldRenderer.pos((- width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(8.0 / 64.0, 16.0 / 64.0).endVertex()
		worldRenderer.pos((width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(16.0 / 64.0, 16.0 / 64.0).endVertex()
		worldRenderer.pos((width / 2).toDouble(), (- height / 2).toDouble(), 0.0).tex(16.0 / 64.0, 8.0 / 64.0).endVertex()
		worldRenderer.pos((- width / 2).toDouble(), (- height / 2).toDouble(), 0.0).tex(8.0 / 64.0, 8.0 / 64.0).endVertex()
		tessellator.draw()
		
		worldRenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
		worldRenderer.pos((- width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(40.0 / 64.0, 16.0 / 64.0).endVertex()
		worldRenderer.pos((width / 2).toDouble(), (height / 2).toDouble(), 0.0).tex(48.0 / 64.0, 16.0 / 64.0).endVertex()
		worldRenderer.pos((width / 2).toDouble(), (- height / 2).toDouble(), 0.0).tex(48.0 / 64.0, 8.0 / 64.0).endVertex()
		worldRenderer.pos((- width / 2).toDouble(), (- height / 2).toDouble(), 0.0).tex(40.0 / 64.0, 8.0 / 64.0).endVertex()
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
		color: Color,
		x: Float, y: Float,
		width: Float, height: Float,
		radius: Float = 5f, thickness: Float = 2f,
		drawMode: Float = 1f
	) {
		if (drawMode == 2f) {
			return drawRoundedRect(color, x - thickness, y - thickness, width+thickness*2, height+thickness*2)
		}
		
		GlStateManager.pushMatrix()
		GlStateManager.disableLighting()
		GlStateManager.disableTexture2D()
		bindColor(color)
		
		glLineWidth(thickness)
		glBegin(GL_LINE_LOOP)
		
		// Top-left corner
		drawRoundedCorner(x + radius, y + radius, radius, 180f, 270f)
		
		// Top-right corner
		drawRoundedCorner(x + width - radius, y + radius, radius, 270f, 360f)
		
		// Bottom-right corner
		drawRoundedCorner(x + width - radius, y + height - radius, radius, 0f, 90f)
		
		// Bottom-left corner
		drawRoundedCorner(x + radius, y + height - radius, radius, 90f, 180f)
		
		glEnd()
		glColor4f(1f, 1f, 1f, 1f)
		GlStateManager.enableTexture2D()
		GlStateManager.popMatrix()
	}
	
	private fun drawRoundedCorner(
		cx: Float, cy: Float,
		radius: Float,
		startAngle: Float, endAngle: Float,
		segments: Int = 16
	) {
		val angleStep = Math.toRadians((endAngle - startAngle).toDouble()) / segments
		
		for (i in 0 .. segments) {
			val angle = Math.toRadians(startAngle.toDouble()) + i * angleStep
			val x2 = cx + cos(angle) * radius
			val y2 = cy + sin(angle) * radius
			glVertex2d(x2, y2)
		}
	}
	
	
	fun drawCircle(x: Float, y: Float, r: Float, h: Int, j: Int, color: Color) {
		glEnable(GL_BLEND)
		glDisable(GL_CULL_FACE)
		glDisable(GL_TEXTURE_2D)
		glBegin(GL_TRIANGLE_FAN)
		
		bindColor(color)
		glVertex2f(x, y)
		
		var h1 = h.toFloat()
		while (h1 <= j) {
			bindColor(color)
			glVertex2f(
				(r * cos(Math.PI * h1 / 180) + x).toFloat(),
				(r * sin(Math.PI * h1 / 180) + y).toFloat()
			)
			h1 ++
		}
		
		glEnd()
		glEnable(GL_TEXTURE_2D)
		glEnable(GL_CULL_FACE)
		glDisable(GL_BLEND)
	}
}