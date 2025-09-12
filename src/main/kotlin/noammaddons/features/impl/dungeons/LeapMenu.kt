package noammaddons.features.impl.dungeons

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemSkull
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.ui.config.core.annotations.AlwaysActive
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.DungeonUtils.dungeonTeammatesNoSelf
import noammaddons.utils.DungeonUtils.leapTeammates
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.disableNEUInventoryButtons
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
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import org.lwjgl.input.Keyboard
import java.awt.Color

@AlwaysActive
object LeapMenu: Feature("Custom Leap Menu and leap message") {
    val customLeapMenu = ToggleSetting("Leap Menu", false)
    val scale = SliderSetting("Menu Scale", 1, 100, 1, 50.0).addDependency(customLeapMenu)
    val showLastDoorOpenner = ToggleSetting("Show Last Door Openner", false).addDependency(customLeapMenu)
    val tintDeadPlayers = ToggleSetting("Tint Dead Players", true).addDependency(customLeapMenu)
    val leapKeybinds = ToggleSetting("Leap Keybinds").addDependency(customLeapMenu)
    val key1 = KeybindSetting("Slot 1", Keyboard.KEY_1)
    val key2 = KeybindSetting("Slot 2", Keyboard.KEY_2)
    val key3 = KeybindSetting("Slot 3", Keyboard.KEY_3)
    val key4 = KeybindSetting("Slot 4", Keyboard.KEY_4)

    private val announceSpiritLeaps = ToggleSetting("Announce Leap", true)
    private val leapMsg = TextInputSetting("Leap Message", "ILY ❤ {name}").addDependency(announceSpiritLeaps)

    private val hideAfterLeap = ToggleSetting("Hide Players After Leap ")
    private val hideTime = SliderSetting("Hide Time", 0.5, 5, 0.1, 3.5).addDependency(hideAfterLeap)
    private var hidePlayers = false

    override fun init() {
        key1.addDependency(leapKeybinds)
        key2.addDependency(leapKeybinds)
        key3.addDependency(leapKeybinds)
        key4.addDependency(leapKeybinds)

        addSettings(
            SeperatorSetting("Custom Leap Menu"),
            customLeapMenu, scale,
            showLastDoorOpenner, tintDeadPlayers,
            leapKeybinds, key1, key2, key3, key4,
            SeperatorSetting("Leap Announcement"),
            announceSpiritLeaps, leapMsg,
            SeperatorSetting("Hide Players After Leap"),
            hideAfterLeap, hideTime
        )
    }

    data class LeapMenuPlayer(val slot: Int, val player: DungeonUtils.DungeonPlayer)

    val players = mutableListOf<LeapMenuPlayer?>(null, null, null, null)

    private fun inSpiritLeap(): Boolean {
        return currentChestName.removeFormatting().lowercase() == "spirit leap" && inDungeon && customLeapMenu.value
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldUnloadEvent) = players.fill(null)

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! enabled) return
        Regex("^You have teleported to (.+)!\$").find(event.component.noFormatText)?.let {
            if (announceSpiritLeaps.value) {
                val name = it.destructured.component1()
                val msg = leapMsg.value.replace("{name}", name)
                sendPartyMessage(msg)
            }

            if (hideAfterLeap.value) {
                setTimeout(hideTime.value.toInt() * 1000L) { hidePlayers = false }
                hidePlayers = true
            }
        }
    }

    @SubscribeEvent
    fun onRenderPlayer(event: RenderPlayerEvent.Pre) {
        if (! hidePlayers) return
        if (event.entity == mc.thePlayer) return
        if (dungeonTeammatesNoSelf.none { it.entity == event.entity }) return
        if (event.entityPlayer.getDistanceToEntity(mc.thePlayer) > 4) return
        event.isCanceled = true
    }

    @SubscribeEvent
    fun preGuiRender(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inSpiritLeap()) return
        updateLeapMenu()
        if (! enabled) return
        disableNEUInventoryButtons()
        event.isCanceled = true

        if (players.filterNotNull().isEmpty()) {
            drawTitle("Spirit Leap Menu", "&4&lNo players found")
            return
        }

        val scale = (scale.value.toFloat() / 100f) * 4.5f / mc.getScaleFactor()
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
                showLastDoorOpenner.value && DungeonUtils.lastDoorOpenner == entry.player -> {
                    if (i == hoveredIndex) darkMode.brighter().brighter().brighter().brighter().brighter().brighter()
                    else darkMode.brighter().brighter().brighter().brighter()
                }

                tintDeadPlayers.value && entry.player.isDead -> {
                    val color = MathUtils.lerpColor(darkMode, Color.RED, 0.2f)
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
                entry.player.skin,
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
        if (! enabled) return
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
            SoundUtils.click()
            if (player.isDead) return@run modMessage("&3LeapMenu >> &c${player.name} is dead!")
            sendWindowClickPacket(slot, 0, 0)
            closeScreen()
        }
    }

    @SubscribeEvent
    fun onUserInput(event: UserInputEvent) {
        if (! enabled) return
        if (! inSpiritLeap()) return
        if (! leapKeybinds.value) return
        if (! event.isMouse && event.keyCode.equalsOneOf(Keyboard.KEY_ESCAPE, Keyboard.KEY_RETURN)) return closeScreen()
        event.isCanceled = true

        val index = when {
            key1.isPressed() -> 0
            key2.isPressed() -> 1
            key3.isPressed() -> 2
            key4.isPressed() -> 3
            else -> return
        }

        players[index]?.run {
            SoundUtils.click()
            if (player.isDead) return@run modMessage("&3LeapMenu >> &c${player.name} is dead!")
            sendWindowClickPacket(slot, 0, 0)
            closeScreen()
        }
    }

    fun updateLeapMenu() {
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