package noammaddons.features.gui.Menus.impl

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemSkull
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiMouseClickEvent
import noammaddons.events.WorldUnloadEvent
import noammaddons.features.Feature
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.DungeonUtils.leapTeammates
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.sendWindowClickPacket
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.MouseUtils.getMouseX
import noammaddons.utils.MouseUtils.getMouseY
import noammaddons.utils.PlayerUtils.closeScreen
import noammaddons.utils.RenderHelper.getHeight
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getWidth
import noammaddons.utils.RenderUtils.drawFloatingRect
import noammaddons.utils.RenderUtils.drawPlayerHead
import noammaddons.utils.RenderUtils.drawRoundedRect
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.RenderUtils.drawTitle
import java.awt.Color

object CustomSpiritLeapMenu: Feature() {
    data class LeapMenuPlayer(val slot: Int, val player: DungeonUtils.DungeonPlayer)

    val players = mutableListOf<LeapMenuPlayer?>(null, null, null, null)

    private fun inSpiritLeap(): Boolean {
        return currentChestName.removeFormatting().lowercase() == "spirit leap" && inDungeon && config.CustomLeapMenu
    }

    @SubscribeEvent
    fun preGuiRender(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inSpiritLeap()) return
        updatePlayersArray()
        event.isCanceled = true

        if (players.filterNotNull().isEmpty()) {
            drawTitle("Spirit Leap Menu", "&4&lNo players found")
            return
        }

        val scale = (config.CustomLeapMenuScale * 4.6f) / mc.getScaleFactor()
        val screenWidth = mc.getWidth() / scale
        val screenHeight = mc.getHeight() / scale

        val boxWidth = 128f * 1.3f
        val boxHeight = 80f * 0.8f
        val boxSpacing = 40f
        val headSize = 50f

        val gridWidth = 2 * boxWidth + boxSpacing
        val gridHeight = 2 * boxHeight + boxSpacing

        val gridX = screenWidth / 2f - gridWidth / 2f
        val gridY = screenHeight / 2f - gridHeight / 2f


        val offsets = listOf(
            listOf(gridX, gridY),
            listOf(gridX + boxWidth + boxSpacing, gridY),
            listOf(gridX, gridY + boxHeight + boxSpacing),
            listOf(gridX + boxWidth + boxSpacing, gridY + boxHeight + boxSpacing)
        )

        val darkMode = Color(33, 33, 33)

        val scaledMouseX = getMouseX() / scale
        val scaledMouseY = getMouseY() / scale

        val hoveredIndex = when {
            scaledMouseX < screenWidth / 2 && scaledMouseY < screenHeight / 2 -> 0
            scaledMouseX > screenWidth / 2 && scaledMouseY < screenHeight / 2 -> 1
            scaledMouseX < screenWidth / 2 && scaledMouseY > screenHeight / 2 -> 2
            scaledMouseX > screenWidth / 2 && scaledMouseY > screenHeight / 2 -> 3
            else -> null
        }

        GlStateManager.pushMatrix()
        GlStateManager.scale(scale, scale, scale)

        players.withIndex().forEach { (i, entry) ->
            if (entry == null) return@forEach
            val rectColor = when {
                config.showLastDoorOpenner && DungeonUtils.lastDoorOpenner == entry.player -> {
                    if (i == hoveredIndex) darkMode.brighter().brighter().brighter().brighter().brighter().brighter()
                    else darkMode.brighter().brighter().brighter().brighter()
                }

                config.tintDeadPlayers && entry.player.isDead -> {
                    val color = MathUtils.interpolateColor(darkMode, Color.RED, 0.2f)
                    if (i == hoveredIndex) color.brighter()
                    else color
                }

                else -> {
                    if (i == hoveredIndex) darkMode.brighter().brighter()
                    else darkMode
                }
            }

            val color = entry.player.clazz.color
            val (boxX, boxY) = offsets[i]
            val textX = boxX + boxWidth / 2.5
            val textY = boxY + boxHeight / 2 - 10

            drawFloatingRect(
                boxX, boxY,
                boxWidth, boxHeight,
                rectColor.withAlpha(190)
            )

            drawRoundedRect(
                color,
                boxX + boxWidth / 5 - headSize / 2 - 1,
                boxY + boxHeight / 2 - headSize / 2 - 1,
                headSize + 2, headSize + 2, 2f
            )

            drawPlayerHead(
                entry.player.locationSkin,
                boxX + boxWidth / 5 - headSize / 2,
                boxY + boxHeight / 2 - headSize / 2,
                headSize, headSize, 2f
            )

            drawText(
                entry.player.name,
                textX, textY + 2,
                color = color
            )

            drawText(
                if (entry.player.isDead) "&4&lDead"
                else entry.player.clazz.name,
                textX, textY + 12f,
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

        val mx = getMouseX()
        val my = getMouseY()

        val index = when {
            mx < centerX && my < centerY -> 0
            mx > centerX && my < centerY -> 1
            mx < centerX && my > centerY -> 2
            mx > centerX && my > centerY -> 3
            else -> return
        }

        players[index]?.run {
            SoundUtils.click.start()
            if (player.isDead) return@run modMessage("&3LeapMenu >> &c${player.name} is dead!")
            sendWindowClickPacket(slot, 0, 0)
            closeScreen()
        }
    }


    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) {
        players.fill(null)
    }

    fun updatePlayersArray() {
        mc.thePlayer?.openContainer?.inventorySlots?.run {
            for (i in 0 ..< size - 36) {
                val stack = get(i)?.stack ?: continue
                if (stack.item !is ItemSkull) continue
                val itemName = stack.displayName.removeFormatting()

                leapTeammates.forEachIndexed { index, teammate ->
                    if (index > players.lastIndex) return@forEachIndexed
                    if (itemName != teammate.name) return@forEachIndexed
                    players[index] = LeapMenuPlayer(get(i).slotIndex, teammate)
                }
            }
        }
    }
}