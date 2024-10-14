// this is where I checked stuff before making them into a feature
// apart from that useless file

package noammaddons

import gg.essential.api.EssentialAPI
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.model.ModelBase
import net.minecraft.client.model.ModelRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RenderPlayer
import net.minecraft.client.renderer.entity.layers.LayerRenderer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.MessageSentEvent
import noammaddons.events.RenderOverlay
import noammaddons.noammaddons.Companion.config
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.LocationUtils.P3Section
import noammaddons.utils.LocationUtils.WorldName
import noammaddons.utils.LocationUtils.dungeonFloor
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.LocationUtils.inSkyblock
import noammaddons.utils.LocationUtils.onHypixel
import noammaddons.utils.PartyUtils.partyLeader
import noammaddons.utils.PartyUtils.partyMembers
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ScanUtils.ScanRoom.getRoom
import noammaddons.utils.ScanUtils.ScanRoom.getRoomCenter
import noammaddons.utils.ScanUtils.ScanRoom.getRoomComponent
import noammaddons.utils.ScanUtils.ScanRoom.getRoomCorner
import noammaddons.utils.ScanUtils.Utils.getCore
import noammaddons.utils.ThreadUtils.runEvery
import java.awt.Color


object TestGround {
	@SubscribeEvent
	@Suppress("UNUSED_PARAMETER")
	fun t(e: RenderOverlay) {
		if (!(config.DevMode || EssentialAPI.getMinecraftUtil().isDevelopment())) return
		drawText("""
			getCore: ${getCore(getRoomCenter().x, getRoomCenter().z)}
			currentRoom: ${getRoom()?.name}
			getRoomComponent: ${getRoomComponent()}
			getRoomCorner: ${getRoomCorner()}
			getRoomCenter: ${getRoomCenter()}
		""".trimIndent(),
		         100f, 100f, 1.5f,
		         Color.CYAN
		)
		
		drawText(
			"""Party Leader: $partyLeader
				| Party Size: ${partyMembers.size}
				| Party Members: ${partyMembers.joinToString(", ")}
			""".trimMargin(),
            20f, 200f, 1.5f,
			Color.PINK
		)
		
		drawText(
			"indungeons: $inDungeons \n dungeonfloor: $dungeonFloor \n inboss: $inBoss \n inSkyblock: $inSkyblock \n onHypixel: $onHypixel \n F7Phase: $F7Phase \n P3Section: $P3Section \n WorldName: $WorldName",
			200f, 10f
		)
	}
	
	@SubscribeEvent
	fun onMessage(event: MessageSentEvent) {
		if (!(config.DevMode || EssentialAPI.getMinecraftUtil().isDevelopment())) return

	}
	
	
	class CosmeticRendering(private val playerRenderer: RenderPlayer) : LayerRenderer<EntityLivingBase> {
		private val haloTexture = ResourceLocation(noammaddons.MOD_ID, "textures/HaloTexture.png")
		private var i = 0
		private var rot = 0f
		
		init {
			runEvery(10) {
				i ++
				rot = (180f - (i % 360))
			}
		}
		
		
		override fun doRenderLayer(
			entityLivingBaseIn: EntityLivingBase, _1: Float,
		    _2: Float, _3: Float, _4: Float,
		    _5: Float, _6: Float, _7: Float
		) {
			drawBoobs(entityLivingBaseIn, 0)
			
			drawDick(entityLivingBaseIn, 0)
			
			drawAss(entityLivingBaseIn, 0)
			
			drawHalo(entityLivingBaseIn, 1)
		}
		
		private fun getScale(size: Int) = when (size) {
			0 -> GlStateManager.scale(1.0f, 1.0f, 1.0f)
			1 -> GlStateManager.scale(1.25f, 1.25f, 1.25f)
			2 -> GlStateManager.scale(1.5f, 1.5f, 1.5f)
			3 -> GlStateManager.scale(1.75f, 1.75f, 1.75f)
			4 -> GlStateManager.scale(2.0f, 2.0f, 2.0f)
			5 -> GlStateManager.scale(3.0f, 3.0f, 3.0f)
			6 -> GlStateManager.scale(6.0f, 6.0f, 6.0f)
			else -> {}
		}
		
		private fun getSneakTranslation(size: Int) = when (size) {
			0 -> GlStateManager.translate(0.0f, 0.01f, 0.17f)
			1 -> GlStateManager.translate(0.0f, - 0.1f, 0.1f)
			2, 3, 4 -> GlStateManager.translate(0.0f, - 0.2f, 0.05f)
			5 -> GlStateManager.translate(0.0f, - 0.2f, 0.1f)
			6 -> GlStateManager.translate(0.0f, - 0.2f, - 0.3f)
			else -> {}
		}
		
		private fun drawBoobs(entityLivingBaseIn: Entity, size: Int) {
			playerRenderer.bindTexture((entityLivingBaseIn as AbstractClientPlayer).locationSkin)
			GlStateManager.pushMatrix()
			
			val bipedBoobs = ModelRenderer(playerRenderer.mainModel, 17, 20)
			bipedBoobs.addBox(- 4.0f, - 6.0f, - 9.0f, 8, 4, 3, 0.0f)
			
			when (size) {
				0 -> GlStateManager.translate(0.0f, 0.5f, 0.3f)
				1 -> GlStateManager.translate(0.0f, 0.6f, 0.36f)
				2 -> GlStateManager.translate(0.0f, 0.7f, 0.45f)
				3 -> GlStateManager.translate(0.0f, 0.8f, 0.55f)
				4 -> GlStateManager.translate(0.0f, 0.9f, 0.65f)
				5 -> GlStateManager.translate(0.0f, 1.28f, 1.0f)
				6 -> GlStateManager.translate(0.0f, 2.4f, 2.14f)
				else -> {}
			}
			
			if (entityLivingBaseIn.isSneaking()) {
				getSneakTranslation(size)
			}
			
			getScale(size)
			
			bipedBoobs.rotationPointX = 0.0f
			bipedBoobs.rotationPointY = 0.0f
			bipedBoobs.render(0.0625f)
			GlStateManager.popMatrix()
		}
		
		private fun drawDick(entityLivingBaseIn: Entity, size: Int) {
			playerRenderer.bindTexture((entityLivingBaseIn as AbstractClientPlayer).locationSkin)
			GlStateManager.pushMatrix()
			
			when (size) {
				0 -> GlStateManager.translate(0.0f, 0.563f, 0.3f)
				1 -> GlStateManager.translate(0.0f, 0.53f, 0.36f)
				2 -> GlStateManager.translate(0.0f, 0.5f, 0.5f)
				3 -> GlStateManager.translate(0.0f, 0.47f, 0.55f)
				4 -> GlStateManager.translate(0.0f, 0.435f, 0.65f)
				5 -> GlStateManager.translate(0.0f, 0.313f, 1.0f)
				6 -> GlStateManager.translate(0.0f, - 0.06f, 2.15f)
				else -> {}
			}
			
			if (entityLivingBaseIn.isSneaking()) {
				getSneakTranslation(size)
			}
			
			getScale(size)
			val bipedTesticles = ModelRenderer(playerRenderer.mainModel, 0, 21)
			bipedTesticles.addBox(- 2.0f, 2.0f, - 9.0f, 4, 4, 3, 0.0f)
			bipedTesticles.addBox(- 1.0f, 3.0f, - 15.0f, 2, 2, 6, 0.0f)
			
			ModelBase.copyModelAngles(playerRenderer.mainModel.bipedBody, bipedTesticles)
			bipedTesticles.rotationPointX = 0.0f
			bipedTesticles.rotationPointY = 0.0f
			bipedTesticles.render(0.0625f)
			GlStateManager.popMatrix()
		}
		
		private fun drawAss(entityLivingBaseIn: Entity, size: Int) {
			playerRenderer.bindTexture((entityLivingBaseIn as AbstractClientPlayer).locationSkin)
			GlStateManager.pushMatrix()
			
			when (size) {
				0 -> GlStateManager.translate(0.0f, 0.5f, 0.3f)
				1 -> GlStateManager.translate(0.0f, 0.5f, 0.36f)
				2, 3 -> GlStateManager.translate(0.0f, 0.5f, 0.41f)
				4 -> GlStateManager.translate(0.0f, 0.5f, 0.50f)
				5 -> GlStateManager.translate(0.0f, 0.55f, 0.69f)
				6 -> GlStateManager.translate(0.0f, 0.65f, 1.25f)
				else -> {}
			}
			
			if (entityLivingBaseIn.isSneaking()) {
				getSneakTranslation(size)
			}
			
			getScale(size)
			
			val bipedAss = ModelRenderer(playerRenderer.mainModel, 18, 24)
			bipedAss.addBox(- 4.0f, - 0.6f, - 3.0f, 8, 4, 3, 0.0f)
			
			ModelBase.copyModelAngles(playerRenderer.mainModel.bipedBody, bipedAss)
			bipedAss.rotationPointX = 0.0f
			bipedAss.rotationPointY = 0.0f
			bipedAss.render(0.0625f)
			GlStateManager.popMatrix()
		}
		
		private fun drawHalo(entityLivingBaseIn: Entity, size: Int) {
			GlStateManager.pushMatrix()
			
			if (entityLivingBaseIn.isSneaking) {
				getSneakTranslation(size)
			}
			getScale(size)
			
			playerRenderer.bindTexture(haloTexture)
			val bipedHalo = ModelRenderer(playerRenderer.mainModel, 0, 0)
			
			
			GlStateManager.rotate(rot, 0f, 1f, 0f)
			
			bipedHalo.addBox(- 2.0f, - 10.0f, - 6.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(1.0f, - 10.0f, 5.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(0.0f, - 10.0f, 5.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 1.0f, - 10.0f, 5.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 2.0f, - 10.0f, 5.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 2.0f, - 10.0f, 4.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 3.0f, - 10.0f, 4.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 4.0f, - 10.0f, 4.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 4.0f, - 10.0f, 3.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 5.0f, - 10.0f, 3.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 5.0f, - 10.0f, 2.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 5.0f, - 10.0f, 1.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 6.0f, - 10.0f, 1.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 6.0f, - 10.0f, - 2.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 6.0f, - 10.0f, - 1.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(1.0f, - 10.0f, 4.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(2.0f, - 10.0f, 4.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(3.0f, - 10.0f, 4.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(3.0f, - 10.0f, 3.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(4.0f, - 10.0f, 3.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(4.0f, - 10.0f, 2.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(4.0f, - 10.0f, 1.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(5.0f, - 10.0f, 1.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(5.0f, - 10.0f, - 0.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(5.0f, - 10.0f, - 1.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(5.0f, - 10.0f, - 2.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 6.0f, - 10.0f, 0.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 5.0f, - 10.0f, - 2.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 5.0f, - 10.0f, - 3.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 5.0f, - 10.0f, - 4.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 4.0f, - 10.0f, - 4.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 4.0f, - 10.0f, - 5.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 3.0f, - 10.0f, - 5.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 2.0f, - 10.0f, - 5.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(4.0f, - 10.0f, - 2.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(4.0f, - 10.0f, - 3.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(4.0f, - 10.0f, - 4.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(3.0f, - 10.0f, - 4.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(3.0f, - 10.0f, - 5.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(2.0f, - 10.0f, - 5.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(1.0f, - 10.0f, - 5.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(1.0f, - 10.0f, - 6.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(0.0f, - 10.0f, - 6.0f, 1, 1, 1, 0.0f)
			bipedHalo.addBox(- 1.0f, - 10.0f, - 6.0f, 1, 1, 1, 0.0f)
			
			bipedHalo.rotationPointX = 0.0f
			bipedHalo.rotationPointY = 0.0f
			bipedHalo.render(0.0625f)
			GlStateManager.popMatrix()
		}
		
		override fun shouldCombineTextures(): Boolean {
			return false
		}
	}
}