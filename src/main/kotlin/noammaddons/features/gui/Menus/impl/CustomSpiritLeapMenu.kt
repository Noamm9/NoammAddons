package noammaddons.features.gui.Menus.impl

import io.github.moulberry.notenoughupdates.NEUApi
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiMouseClickEvent
import noammaddons.events.InventoryFullyOpenedEvent
import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils.Classes
import noammaddons.utils.DungeonUtils.leapTeammates
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.getMouseX
import noammaddons.utils.GuiUtils.getMouseY
import noammaddons.utils.GuiUtils.sendWindowClickPacket
import noammaddons.utils.LocationUtils.inDungeons
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.PlayerUtils.closeScreen
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawFloatingRectWithAlpha
import noammaddons.utils.RenderUtils.drawPlayerHead
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.SoundUtils
import noammaddons.utils.Utils.isNull
import java.awt.Color

object CustomSpiritLeapMenu: Feature() {
    private data class LeapMenuPlayer(var name: String, var clazz: Classes, var slot: Int, var skin: ResourceLocation)

    private var players = mutableListOf<LeapMenuPlayer?>(null, null, null, null)

    private fun inSpiritLeap(): Boolean {
        return currentChestName.removeFormatting().lowercase() == "spirit leap" && config.CustomLeapMenu && inDungeons
    }

    @SubscribeEvent
    fun fuckNEU(event: InventoryFullyOpenedEvent) {
        // Fuck NEU horrible code, but thanks for api 😘
        if (Loader.instance().activeModList.none { it.modId == NotEnoughUpdates.MODID }) return
        if (! inSpiritLeap()) return

        NEUApi.setInventoryButtonsToDisabled()
    }

    @SubscribeEvent
    fun preGuiRender(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inSpiritLeap()) return
        updatePlayersArray()
        event.isCanceled = true

        if (players.filterNotNull().isEmpty()) return showTitle(
            "Spirit Leap Menu",
            "&4&lNo players found",
            0.01f
        )

        val scale = (config.CustomLeapMenuScale * 4.5f) / mc.getScaleFactor()
        val screenWidth = mc.getWidth() / scale
        val screenHeight = mc.getHeight() / scale

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
        GlStateManager.scale(scale, scale, scale)

        players.forEachIndexed { i, player ->
            if (player.isNull()) return@forEachIndexed
            val color = player !!.clazz.color

            drawFloatingRectWithAlpha(
                offsets[i][0].toInt(),
                offsets[i][1].toInt(),
                BoxWidth.toInt(),
                BoxHeight.toInt(),
                false, ColorMode
            )

            drawRoundedRect(
                color,
                (offsets[i][0] + BoxWidth / 2 - HeadsHeightWidth / 2) - 1,
                ((offsets[i][1] + BoxHeight - HeadsHeightWidth * 1.2f) - BoxHeight / 20) - 1,
                HeadsHeightWidth + 2, HeadsHeightWidth + 2, 5f
            )

            drawPlayerHead(
                player.skin,
                (offsets[i][0] + BoxWidth / 2 - HeadsHeightWidth / 2),
                ((offsets[i][1] + BoxHeight - HeadsHeightWidth * 1.2f) - BoxHeight / 20),
                HeadsHeightWidth, HeadsHeightWidth, 5f
            )

            drawText(
                "§n${player.name}",
                offsets[i][0] + 4,
                offsets[i][1] + 4,
                color = color
            )

            drawText(
                player.clazz.name,
                offsets[i][0] + BoxWidth - getStringWidth(player.clazz.name) - BoxWidth / 25,
                offsets[i][1] + BoxHeight - BoxHeight / 8,
                color = color
            )

        }
        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onClick(event: GuiMouseClickEvent) {
        if (! inSpiritLeap()) return
        event.isCanceled = true

        val centerX = mc.getWidth() / 2
        val centerY = mc.getHeight() / 2

        val mx = mc.getMouseX()
        val my = mc.getMouseY()

        val index = when {
            mx < centerX && my < centerY -> 0
            mx > centerX && my < centerY -> 1
            mx < centerX && my > centerY -> 2
            mx > centerX && my > centerY -> 3
            else -> return
        }

        players[index]?.run {
            SoundUtils.click.start()
            sendWindowClickPacket(slot, 0, 0)
            closeScreen()
        }
    }


    private fun updatePlayersArray() {
        Player?.openContainer?.inventorySlots?.run {
            players.fill(null)

            for (i in 0 ..< size - 36) {
                val itemName = get(i)?.stack?.displayName?.removeFormatting() ?: continue
                leapTeammates.forEachIndexed { index, it ->
                    if (it.isDead) return@forEachIndexed
                    if (itemName != it.name) return@forEachIndexed

                    players[index] = LeapMenuPlayer(
                        it.name, it.clazz,
                        get(i).slotIndex,
                        it.locationSkin
                    )
                }
            }
        }
    }
}