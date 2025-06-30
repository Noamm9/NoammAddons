package noammaddons.features.impl.misc

import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.model.ModelBase
import net.minecraft.client.model.ModelRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RenderPlayer
import net.minecraft.client.renderer.entity.layers.LayerRenderer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import noammaddons.features.impl.misc.PlayerModel.getPlayerScaleFactor
import noammaddons.NoammAddons.Companion.MOD_ID
import noammaddons.NoammAddons.Companion.mc
import noammaddons.utils.GuiUtils.isInGui
import noammaddons.utils.RenderHelper.getPartialTicks
import noammaddons.utils.WebUtils
import kotlin.math.cos
import kotlin.math.sin

object Cosmetics {
    private var devPlayers: List<String>? = null

    init {
        WebUtils.fetchJsonWithRetry<List<String>>(
            "https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/devPlayers.json"
        ) { devPlayers = it }
    }

    class CosmeticRendering(private val playerRenderer: RenderPlayer): LayerRenderer<EntityLivingBase> {
        override fun doRenderLayer(
            entityLivingBaseIn: EntityLivingBase, _1: Float,
            _2: Float, _3: Float, _4: Float,
            _5: Float, _6: Float, _7: Float
        ) {
            val player = entityLivingBaseIn as EntityPlayer
            val dev = devPlayers?.find { it == player.uniqueID.toString() } ?: return

            GlStateManager.pushMatrix()
            GlStateManager.disableLighting()

            DragonWings.renderWings(player, dev)
            AngelHalo.drawHalo(player, dev)

            GlStateManager.enableLighting()
            GlStateManager.popMatrix()
        }

        private fun getScale(size: Int) = when (size) {
            0 -> GlStateManager.scale(1f, 1f, 1f)
            1 -> GlStateManager.scale(1.25f, 1.25f, 1.25f)
            2 -> GlStateManager.scale(1.5f, 1.5f, 1.5f)
            3 -> GlStateManager.scale(1.75f, 1.75f, 1.75f)
            4 -> GlStateManager.scale(2f, 2f, 2f)
            5 -> GlStateManager.scale(3f, 3f, 3f)
            6 -> GlStateManager.scale(6f, 6f, 6f)
            else -> {}
        }

        private fun getSneakTranslation(size: Int) = when (size) {
            0 -> GlStateManager.translate(0f, 0.01f, 0.17f)
            1 -> GlStateManager.translate(0f, - 0.1f, 0.1f)
            2, 3, 4 -> GlStateManager.translate(0f, - 0.2f, 0.05f)
            5 -> GlStateManager.translate(0f, - 0.2f, 0.1f)
            6 -> GlStateManager.translate(0f, - 0.2f, - 0.3f)
            else -> {}
        }

        private fun drawBoobs(entityLivingBaseIn: Entity, size: Int) {
            playerRenderer.bindTexture((entityLivingBaseIn as AbstractClientPlayer).locationSkin)
            GlStateManager.pushMatrix()

            val bipedBoobs = ModelRenderer(playerRenderer.mainModel, 17, 20)
            bipedBoobs.addBox(- 4f, - 6f, - 9f, 8, 4, 3, 0f)

            when (size) {
                0 -> GlStateManager.translate(0f, 0.5f, 0.3f)
                1 -> GlStateManager.translate(0f, 0.6f, 0.36f)
                2 -> GlStateManager.translate(0f, 0.7f, 0.45f)
                3 -> GlStateManager.translate(0f, 0.8f, 0.55f)
                4 -> GlStateManager.translate(0f, 0.9f, 0.65f)
                5 -> GlStateManager.translate(0f, 1.28f, 1f)
                6 -> GlStateManager.translate(0f, 2.4f, 2.14f)
                else -> {}
            }

            if (entityLivingBaseIn.isSneaking()) {
                getSneakTranslation(size)
            }

            getScale(size)

            bipedBoobs.rotationPointX = 0f
            bipedBoobs.rotationPointY = 0f
            bipedBoobs.render(0.0625f)
            GlStateManager.popMatrix()
        }

        private fun drawDick(entityLivingBaseIn: Entity, size: Int) {
            playerRenderer.bindTexture((entityLivingBaseIn as AbstractClientPlayer).locationSkin)
            GlStateManager.pushMatrix()

            when (size) {
                0 -> GlStateManager.translate(0f, 0.563f, 0.3f)
                1 -> GlStateManager.translate(0f, 0.53f, 0.36f)
                2 -> GlStateManager.translate(0f, 0.5f, 0.5f)
                3 -> GlStateManager.translate(0f, 0.47f, 0.55f)
                4 -> GlStateManager.translate(0f, 0.435f, 0.65f)
                5 -> GlStateManager.translate(0f, 0.313f, 1f)
                6 -> GlStateManager.translate(0f, - 0.06f, 2.15f)
                else -> {}
            }

            if (entityLivingBaseIn.isSneaking()) {
                getSneakTranslation(size)
            }

            getScale(size)
            val bipedTesticles = ModelRenderer(playerRenderer.mainModel, 0, 21)
            bipedTesticles.addBox(- 2f, 2f, - 9f, 4, 4, 3, 0f)
            bipedTesticles.addBox(- 1f, 3f, - 15f, 2, 2, 6, 0f)

            ModelBase.copyModelAngles(playerRenderer.mainModel.bipedBody, bipedTesticles)
            bipedTesticles.rotationPointX = 0f
            bipedTesticles.rotationPointY = 0f
            bipedTesticles.render(0.0625f)
            GlStateManager.popMatrix()
        }

        private fun drawAss(entityLivingBaseIn: Entity, size: Int) {
            playerRenderer.bindTexture((entityLivingBaseIn as AbstractClientPlayer).locationSkin)
            GlStateManager.pushMatrix()

            when (size) {
                0 -> GlStateManager.translate(0f, 0.5f, 0.3f)
                1 -> GlStateManager.translate(0f, 0.5f, 0.36f)
                2, 3 -> GlStateManager.translate(0f, 0.5f, 0.41f)
                4 -> GlStateManager.translate(0f, 0.5f, 0.50f)
                5 -> GlStateManager.translate(0f, 0.55f, 0.69f)
                6 -> GlStateManager.translate(0f, 0.65f, 1.25f)
                else -> {}
            }

            if (entityLivingBaseIn.isSneaking()) {
                getSneakTranslation(size)
            }

            getScale(size)

            val bipedAss = ModelRenderer(playerRenderer.mainModel, 18, 24)
            bipedAss.addBox(- 4f, - 0.6f, - 3f, 8, 4, 3, 0f)

            ModelBase.copyModelAngles(playerRenderer.mainModel.bipedBody, bipedAss)
            bipedAss.rotationPointX = 0f
            bipedAss.rotationPointY = 0f
            bipedAss.render(0.0625f)
            GlStateManager.popMatrix()
        }

        override fun shouldCombineTextures(): Boolean {
            return false
        }
    }

    private fun interpolate(yaw1: Float, yaw2: Float): Float {
        var f = (yaw1 + (yaw2 - yaw1) * getPartialTicks()) % 360
        if (f < 0) {
            f += 360f
        }
        return f
    }

    /**
     * Modified
     * @author Odin Mod.
     */
    object DragonWings: ModelBase() {
        private val dragonWingTextureLocation = ResourceLocation("textures/entity/enderdragon/dragon.png")
        private val wing: ModelRenderer
        private val wingTip: ModelRenderer

        init {
            textureWidth = 256
            textureHeight = 256
            setTextureOffset("wing.skin", - 56, 88)
            setTextureOffset("wingtip.skin", - 56, 144)
            setTextureOffset("wing.bone", 112, 88)
            setTextureOffset("wingtip.bone", 112, 136)

            wing = ModelRenderer(this, "wing")
            wing.setRotationPoint(- 12f, 5f, 2f)
            wing.addBox("bone", - 56.0f, - 4f, - 4f, 56, 8, 8)
            wing.addBox("skin", - 56.0f, 0f, 2f, 56, 0, 56)
            wingTip = ModelRenderer(this, "wingtip")
            wingTip.setRotationPoint(- 56.0f, 0f, 0f)
            wingTip.addBox("bone", - 56.0f, - 2f, - 2f, 56, 4, 4)
            wingTip.addBox("skin", - 56.0f, 0f, 2f, 56, 0, 56)
            wing.addChild(wingTip)
        }

        fun renderWings(player: EntityPlayer, devID: String?) {
            if (player.uniqueID.toString() != devID) return
            val scale = getPlayerScaleFactor(player)

            GlStateManager.pushMatrix()
            GlStateManager.scale(0.2 * scale, 0.2 * scale, 0.2 * scale)
            GlStateManager.translate(0.0, 0.45, 0.1 / 0.2 / scale)

            if (player.isSneaking) GlStateManager.translate(0.0, 0.125 * scale, 0.0)

            GlStateManager.color(1f, 1f, 1f, 1f)
            mc.textureManager.bindTexture(dragonWingTextureLocation)

            for (j in 0 .. 1) {
                GlStateManager.enableCull()
                val f11 = System.currentTimeMillis() % 1000 / 1000f * Math.PI.toFloat() * 2f
                wing.rotateAngleX = Math.toRadians(- 80.0).toFloat() - cos(f11) * 0.2f
                wing.rotateAngleY = Math.toRadians(20.0).toFloat() + sin(f11) * 0.4f
                wing.rotateAngleZ = Math.toRadians(20.0).toFloat()
                wingTip.rotateAngleZ = - (sin((f11 + 2f)) + 0.5).toFloat() * 0.75f
                wing.render(0.0625f)
                GlStateManager.scale(- 1f, 1f, 1f)
                if (j == 0) GlStateManager.cullFace(1028)
            }

            GlStateManager.cullFace(1029)
            GlStateManager.disableCull()
            GlStateManager.color(1f, 1f, 1f, 1f)
            GlStateManager.popMatrix()
        }
    }

    object AngelHalo: ModelBase() {
        private val haloTexture = ResourceLocation(MOD_ID, "textures/HaloTexture.png")
        private val halo: ModelRenderer

        init {
            textureWidth = 32
            textureHeight = 32
            halo = ModelRenderer(this, "ring")


            halo.addBox(- 2f, - 10f, - 6f, 1, 1, 1, 0f)
            halo.addBox(1f, - 10f, 5f, 1, 1, 1, 0f)
            halo.addBox(0f, - 10f, 5f, 1, 1, 1, 0f)
            halo.addBox(- 1f, - 10f, 5f, 1, 1, 1, 0f)
            halo.addBox(- 2f, - 10f, 5f, 1, 1, 1, 0f)
            halo.addBox(- 2f, - 10f, 4f, 1, 1, 1, 0f)
            halo.addBox(- 3f, - 10f, 4f, 1, 1, 1, 0f)
            halo.addBox(- 4f, - 10f, 4f, 1, 1, 1, 0f)
            halo.addBox(- 4f, - 10f, 3f, 1, 1, 1, 0f)
            halo.addBox(- 5f, - 10f, 3f, 1, 1, 1, 0f)
            halo.addBox(- 5f, - 10f, 2f, 1, 1, 1, 0f)
            halo.addBox(- 5f, - 10f, 1f, 1, 1, 1, 0f)
            halo.addBox(- 6f, - 10f, 1f, 1, 1, 1, 0f)
            halo.addBox(- 6f, - 10f, - 2f, 1, 1, 1, 0f)
            halo.addBox(- 6f, - 10f, - 1f, 1, 1, 1, 0f)
            halo.addBox(1f, - 10f, 4f, 1, 1, 1, 0f)
            halo.addBox(2f, - 10f, 4f, 1, 1, 1, 0f)
            halo.addBox(3f, - 10f, 4f, 1, 1, 1, 0f)
            halo.addBox(3f, - 10f, 3f, 1, 1, 1, 0f)
            halo.addBox(4f, - 10f, 3f, 1, 1, 1, 0f)
            halo.addBox(4f, - 10f, 2f, 1, 1, 1, 0f)
            halo.addBox(4f, - 10f, 1f, 1, 1, 1, 0f)
            halo.addBox(5f, - 10f, 1f, 1, 1, 1, 0f)
            halo.addBox(5f, - 10f, - 0f, 1, 1, 1, 0f)
            halo.addBox(5f, - 10f, - 1f, 1, 1, 1, 0f)
            halo.addBox(5f, - 10f, - 2f, 1, 1, 1, 0f)
            halo.addBox(- 6f, - 10f, 0f, 1, 1, 1, 0f)
            halo.addBox(- 5f, - 10f, - 2f, 1, 1, 1, 0f)
            halo.addBox(- 5f, - 10f, - 3f, 1, 1, 1, 0f)
            halo.addBox(- 5f, - 10f, - 4f, 1, 1, 1, 0f)
            halo.addBox(- 4f, - 10f, - 4f, 1, 1, 1, 0f)
            halo.addBox(- 4f, - 10f, - 5f, 1, 1, 1, 0f)
            halo.addBox(- 3f, - 10f, - 5f, 1, 1, 1, 0f)
            halo.addBox(- 2f, - 10f, - 5f, 1, 1, 1, 0f)
            halo.addBox(4f, - 10f, - 2f, 1, 1, 1, 0f)
            halo.addBox(4f, - 10f, - 3f, 1, 1, 1, 0f)
            halo.addBox(4f, - 10f, - 4f, 1, 1, 1, 0f)
            halo.addBox(3f, - 10f, - 4f, 1, 1, 1, 0f)
            halo.addBox(3f, - 10f, - 5f, 1, 1, 1, 0f)
            halo.addBox(2f, - 10f, - 5f, 1, 1, 1, 0f)
            halo.addBox(1f, - 10f, - 5f, 1, 1, 1, 0f)
            halo.addBox(1f, - 10f, - 6f, 1, 1, 1, 0f)
            halo.addBox(0f, - 10f, - 6f, 1, 1, 1, 0f)
            halo.addBox(- 1f, - 10f, - 6f, 1, 1, 1, 0f)


            halo.rotationPointX = 0f
            halo.rotationPointY = 0f
            halo.render(0.0625f)
        }

        fun drawHalo(player: EntityPlayer, devID: String?) {
            if (player.uniqueID.toString() != devID) return
            val rotation = if (isInGui()) player.renderYawOffset
            else interpolate(player.prevRenderYawOffset, player.renderYawOffset)
            val scale = getPlayerScaleFactor(player)
            val yPos = if (scale == 1f) 0.3f else 0.45f

            GlStateManager.pushMatrix()
            GlStateManager.translate(0f, - yPos * scale, 0f)
            GlStateManager.scale(scale, scale, scale)

            val rotationAngle = (System.currentTimeMillis() % 3600) / 10f
            GlStateManager.rotate(rotationAngle - rotation, 0f, 1f, 0f)

            if (player.isSneaking) GlStateManager.translate(0f, yPos * scale, 0f)

            GlStateManager.color(1f, 1f, 0f, 1f)
            mc.textureManager.bindTexture(haloTexture)
            GlStateManager.enableCull()

            halo.render(0.0625f)

            GlStateManager.disableCull()
            GlStateManager.popMatrix()
        }
    }
}