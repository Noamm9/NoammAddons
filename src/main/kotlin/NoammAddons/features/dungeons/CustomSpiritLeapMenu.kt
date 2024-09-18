package NoammAddons.features.dungeons

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.DungeonUtils
import net.minecraft.util.ResourceLocation
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.GuiContainerEvent
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.modMessage
import NoammAddons.utils.GuiUtils
import NoammAddons.utils.GuiUtils.clickSlot
import NoammAddons.utils.GuiUtils.getMouseX
import NoammAddons.utils.GuiUtils.getMouseY
import NoammAddons.utils.GuiUtils.getPatcherScale
import NoammAddons.utils.LocationUtils.inDungeons
import NoammAddons.utils.RenderUtils.drawPlayerHead
import NoammAddons.utils.RenderUtils.drawRoundedRect
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
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

        val Scale = (config.CustomLeapMenuScale * 2 / getPatcherScale())
        val screenWidth = mc.getWidth() / Scale
        val screenHeight = mc.getHeight() / Scale
        val width = 288.0
        val height = 192.0
        val X = screenWidth / 2 - width / 2
        val Y = screenHeight / 2 - height / 2
        val BoxWidth = 128.0
        val BoxHeight = 80.0
        val BoxSpacing = 40.0
        val HeadsHeightWidth = 50.0

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
        GlStateManager.scale(Scale, Scale, .0)

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
                ((offsets[i][1] + BoxHeight - HeadsHeightWidth * 1.2) - BoxHeight / 20) - 2,
                HeadsHeightWidth + 4, HeadsHeightWidth + 4, 5.0
            )

            drawPlayerHead(
                players[i]!!.skin,
                (offsets[i][0] + BoxWidth / 2 - HeadsHeightWidth / 2),
                ((offsets[i][1] + BoxHeight - HeadsHeightWidth * 1.2) - BoxHeight / 20),
                HeadsHeightWidth, HeadsHeightWidth, 5.0
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
            mc.thePlayer.closeScreen()
            modMessage(it.name)
        }
    }


    private fun updatePlayersArray() {
        if (!GuiUtils.isInGui()) return
        val Chest = mc.thePlayer?.openContainer ?: return

        for (i in 0..<Chest.inventorySlots.size) {
            val itemName = (Chest.inventorySlots.get(i))?.stack?.displayName?.removeFormatting() ?: continue
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


    private fun isSpiritLeapGuiAndSettingsEnabled(): Boolean = GuiUtils.currentChestName.toLowerCase() == "spirit leap" && config.CustomLeapMenu && inDungeons

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
    }
*/

    fun drawText(text: String, x: Double, y: Double, scale: Double = 1.0, color: Color = Color.WHITE) {
        GlStateManager.pushMatrix()
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.scale(scale, scale, 1.0)

        GlStateManager.color(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f
        )

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
        } else {
            mc.fontRendererObj.drawStringWithShadow(
                formattedText,
                (x / scale).toFloat(),
                (y / scale).toFloat(),
                color.rgb
            )
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        GlStateManager.enableLighting()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    data class LeapMenuPlayer(var name: String, var clazz: DungeonUtils.Classes, var slot: Int, var skin: ResourceLocation)
}
