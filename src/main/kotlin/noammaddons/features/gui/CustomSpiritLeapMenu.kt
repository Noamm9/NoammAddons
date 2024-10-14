package noammaddons.features.gui

import noammaddons.noammaddons.Companion.config
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils
import net.minecraft.util.ResourceLocation
import noammaddons.noammaddons.Companion.mc
import noammaddons.events.GuiContainerEvent
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.GuiUtils
import noammaddons.utils.GuiUtils.clickSlot
import noammaddons.utils.GuiUtils.getMouseX
import noammaddons.utils.GuiUtils.getMouseY
import noammaddons.utils.GuiUtils.getPatcherScale
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.RenderUtils.drawPlayerHead
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.getHeight
import noammaddons.utils.RenderUtils.getWidth
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.closeScreen
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import java.awt.Color

object CustomSpiritLeapMenu {
    private var players = mutableListOf<LeapMenuPlayer?>(null,null,null,null)

    @SubscribeEvent
    fun preGuiRender(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (!isSpiritLeapGuiAndSettingsEnabled()) return
    //    if (!config.DevMode) event.isCanceled = true
        event.isCanceled = true
        updatePlayersArray()

        if (players.filterNotNull().isEmpty()) return

        val Scale = config.CustomLeapMenuScale * 2f /getPatcherScale()
        val screenWidth = mc.getWidth() / Scale
        val screenHeight = mc.getHeight() / Scale
        val width = 288f
        val height = 192f
        val X = screenWidth / 2f - width / 2f
        val Y = screenHeight / 2f - height / 2f
        val BoxWidth = 128f
        val BoxHeight = 80f
        val BoxSpacing = 40f
        val HeadsHeightWidth = 50f

        val offsets = listOf(
            listOf(X, Y),
            listOf(X + BoxWidth + BoxSpacing, Y),
            listOf(X, Y + BoxHeight + BoxSpacing),
            listOf(X + BoxWidth + BoxSpacing, Y + BoxHeight + BoxSpacing)
        )

        val Lightmode = Color(203, 202, 205)
        val Darkmode = Color(33, 33, 33)
        val ColorMode = if (config.CustomLeapMenuLightMode) Lightmode else Darkmode

        GlStateManager.pushMatrix()
        GlStateManager.scale(Scale, Scale, 0f)

        for (i in 0..<players.size) {
            if (players[i] == null) continue

            drawRoundedRect(
                ColorMode.darker(),
                offsets[i][0] - (BoxWidth / 15) / 2,
                offsets[i][1] - (BoxHeight / 15) / 2,
                (BoxWidth + BoxWidth / 15),
                (BoxHeight + BoxHeight / 15)
            )

            drawRoundedRect(
                ColorMode,
                offsets[i][0],
                offsets[i][1],
                BoxWidth,
                BoxHeight
            )

            drawText(
                "§n${players[i]!!.name}",
                offsets[i][0] + 4,
                offsets[i][1] + 4,
                color = players[i]!!.clazz.color
            )

            GlStateManager.color(1f, 1f, 1f, 1f)

            drawText(
                players[i]!!.clazz.name,
                offsets[i][0] + BoxWidth - mc.fontRendererObj.getStringWidth(players[i]!!.clazz.name) - BoxWidth / 25,
                offsets[i][1] + BoxHeight - BoxHeight / 8,
                color = players[i]!!.clazz.color
            )


            drawRoundedRect(
                players[i]!!.clazz.color,
                (offsets[i][0] + BoxWidth / 2 - HeadsHeightWidth / 2) - 2,
                ((offsets[i][1] + BoxHeight - HeadsHeightWidth * 1.2f) - BoxHeight / 20) - 2,
                HeadsHeightWidth + 4, HeadsHeightWidth + 4, 5f
            )

            drawPlayerHead(
                players[i]!!.skin,
                (offsets[i][0] + BoxWidth / 2 - HeadsHeightWidth / 2),
                ((offsets[i][1] + BoxHeight - HeadsHeightWidth * 1.2f) - BoxHeight / 20),
                HeadsHeightWidth, HeadsHeightWidth, 5f
            )
        }

        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.GuiMouseClickEvent) {
        if (!isSpiritLeapGuiAndSettingsEnabled()) return
        event.isCanceled = true

        val centerX = mc.getWidth()/2
        val centerY = mc.getHeight()/2

        val mx = mc.getMouseX()
        val my = mc.getMouseY()
        var index = -1

        if (mx < centerX && my < centerY) index = 0
        else if (mx > centerX && my < centerY) index = 1
        else if (mx < centerX && my > centerY) index = 2
        else if (mx > centerX && my > centerY) index = 3

        if (index == -1) return

        players[index]?.let {
            clickSlot(it.slot, false, 0)
            closeScreen()
            modMessage(it.name)
        }
    }


    private fun updatePlayersArray() {
        if (!GuiUtils.isInGui()) return
        val Chest = Player?.openContainer ?: return
	    players.fill(null)

        for (i in 0..<Chest.inventorySlots.size) {
            val itemName = (Chest.inventorySlots[i])?.stack?.displayName?.removeFormatting() ?: continue
            DungeonUtils.leapTeammates.forEachIndexed { index, it ->
                if (itemName != it.name) return@forEachIndexed
                if (it.isDead) return@forEachIndexed

                players[index] = LeapMenuPlayer(
                    it.name,
                    it.clazz,
                    Chest.inventorySlots[i].slotIndex,
                    it.locationSkin
                )
            }
        }
    }


    private fun isSpiritLeapGuiAndSettingsEnabled(): Boolean =
		GuiUtils.currentChestName.removeFormatting().toLowerCase() == "spirit leap" && config.CustomLeapMenu && inDungeons

/*
    @SubscribeEvent
    fun test(e: RenderGameOverlayEvent.Pre) {
        if (!config.DevMode) return
        if (e.type != RenderGameOverlayEvent.ElementType.TEXT) return

        players.forEachIndexed { index, it ->
            if (it != null) {
                drawText(
                    it.toString(),
                    10.0,
                    10.0 * index,
                    color = it.clazz.color
                )
            }
        }
    }*/

    private fun drawText(text: String, x: Float, y: Float, scale: Float = 1f, color: Color = Color.WHITE) {
        GlStateManager.pushMatrix()
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.scale(scale, scale, 1f)
        GlStateManager.color(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f
        )
	    
	    mc.fontRendererObj.drawStringWithShadow(
		    text.addColor(),
		    x / scale,
		    y / scale,
		    color.rgb
		)
        

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        GlStateManager.enableLighting()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }


    data class LeapMenuPlayer(var name: String, var clazz: DungeonUtils.Classes, var slot: Int, var skin: ResourceLocation)
}
