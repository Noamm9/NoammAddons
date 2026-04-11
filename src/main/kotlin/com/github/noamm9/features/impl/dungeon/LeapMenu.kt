package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.CheckEntityRenderEvent
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.ScreenEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.*
import com.github.noamm9.ui.clickgui.components.impl.*
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ButtonType
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonListener.dungeonTeammatesNoSelf
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import java.awt.Color

object LeapMenu: Feature("Custom Leap Menu and leap message") {
    val customLeapMenu by ToggleSetting("Leap Menu", false).section("Custom Menu")
    val scale by SliderSetting("Menu Scale", 50f, 30, 100, 1).showIf { customLeapMenu.value }
    val showLastDoorOpener by ToggleSetting("Show Last Door Opener", false).showIf { customLeapMenu.value }
    val tintDeadPlayers by ToggleSetting("Tint Dead Players", true).showIf { customLeapMenu.value }

    val sorting by DropdownSetting("Leap Order", 0, arrayListOf("A-Z Class", "A-Z Name", "Odin Sorting", "Custom sorting", "No Sorting"))
        .withDescription("How to sort the leap menu. /na leaporder to configure custom sorting.")

    val leapKeybinds by ToggleSetting("Leap Keybinds").showIf { customLeapMenu.value }.section("Leap Keybinds")
    val keybindMode by DropdownSetting("Mode", 0, listOf("Corners", "Class")).showIf { leapKeybinds.value }
    val key1 by KeybindSetting("Slot 1", GLFW.GLFW_KEY_1).showIf { leapKeybinds.value && keybindMode.value == 0 }
    val key2 by KeybindSetting("Slot 2", GLFW.GLFW_KEY_2).showIf { leapKeybinds.value && keybindMode.value == 0 }
    val key3 by KeybindSetting("Slot 3", GLFW.GLFW_KEY_3).showIf { leapKeybinds.value && keybindMode.value == 0 }
    val key4 by KeybindSetting("Slot 4", GLFW.GLFW_KEY_4).showIf { leapKeybinds.value && keybindMode.value == 0 }
    val keyArcher by KeybindSetting("Archer", GLFW.GLFW_KEY_UNKNOWN).showIf { leapKeybinds.value && keybindMode.value == 1 }
    val keyBerserk by KeybindSetting("Berserk", GLFW.GLFW_KEY_UNKNOWN).showIf { leapKeybinds.value && keybindMode.value == 1 }
    val keyHealer by KeybindSetting("Healer", GLFW.GLFW_KEY_UNKNOWN).showIf { leapKeybinds.value && keybindMode.value == 1 }
    val keyMage by KeybindSetting("Mage", GLFW.GLFW_KEY_UNKNOWN).showIf { leapKeybinds.value && keybindMode.value == 1 }
    val keyTank by KeybindSetting("Tank", GLFW.GLFW_KEY_UNKNOWN).showIf { leapKeybinds.value && keybindMode.value == 1 }

    private val announceSpiritLeaps by ToggleSetting("Announce Leap", true).section("Extras")
    private val leapMsg by TextInputSetting("Leap Message", "ILY ❤ {name}")
        .withDescription("replaces {name} with the player name")
        .showIf { announceSpiritLeaps.value }

    private val hideAfterLeap by ToggleSetting("Hide Players")
        .withDescription("Hides players for a certain amount of time after you leap")
    private val hideTime by SliderSetting("Hide Time", 3.5, 0.5, 5.0, 0.1).showIf { hideAfterLeap.value }


    data class LeapMenuPlayer(val slotIndex: Int, val player: DungeonPlayer)

    val players = MutableList<LeapMenuPlayer?>(4) { null }
    private val leapRegex = Regex("^You have teleported to (.+)!$")
    private val playerRegex = Regex("(?:\\[.+?] )?(?<name>\\w+)")
    private var shouldHide: Long = 0

    var customLeapOrder = listOf<String>()

    override fun init() {
        register<ChatMessageEvent> {
            leapRegex.find(event.unformattedText)?.destructured?.component1()?.let { name ->
                if (announceSpiritLeaps.value) leapMsg.value.replace("{name}", name).takeUnless { it.isBlank() }?.let {
                    ChatUtils.sendPartyMessage(it)
                }

                if (hideAfterLeap.value) {
                    shouldHide = System.currentTimeMillis() + ((hideTime.value * 1000L).toLong())
                }
            }
        }

        register<CheckEntityRenderEvent> {
            if (System.currentTimeMillis() > shouldHide) return@register
            if (event.entity !is Player) return@register
            if (event.entity == mc.player) return@register
            if (event.entity.distanceToSqr(mc.player) > 4) return@register
            if (dungeonTeammatesNoSelf.none { it.name == event.entity.name.unformattedText }) return@register
            event.isCanceled = true
        }

        register<ScreenEvent.PreRender> {
            if (! customLeapMenu.value) return@register
            if (! inSpiritLeap(event.screen)) return@register
            event.isCanceled = true

            updateLeapMenu()

            if (players.filterNotNull().isEmpty()) {
                Render2D.drawCenteredString(event.context, "§4§lNo players found", Resolution.width / 2, Resolution.height / 2)
                return@register
            }

            Resolution.refresh()
            Resolution.push(event.context)
            val userScale = (scale.value.toFloat() / 100f) * 2.0f
            val screenWidth = Resolution.width / userScale
            val screenHeight = Resolution.height / userScale

            val pose = event.context.pose()
            pose.pushMatrix()
            pose.scale(userScale)

            val boxWidth = 128f * 1.3f
            val boxHeight = 80f * 0.8f
            val padding = 40f
            val headSize = 50

            val gridW = (boxWidth * 2) + padding
            val gridH = (boxHeight * 2) + padding
            val startX = (screenWidth - gridW) / 2f
            val startY = (screenHeight - gridH) / 2f

            val offsets = listOf(
                startX to startY,
                (startX + boxWidth + padding) to startY,
                startX to (startY + boxHeight + padding),
                (startX + boxWidth + padding) to (startY + boxHeight + padding)
            )

            val cx = mc.window.width / 2
            val cy = mc.window.height / 2
            val mx = mc.mouseHandler.xpos()
            val my = mc.mouseHandler.ypos()

            val hoveredIndex = when {
                mx < cx && my < cy -> 0
                mx > cx && my < cy -> 1
                mx < cx && my > cy -> 2
                mx > cx && my > cy -> 3
                else -> - 1
            }

            players.forEachIndexed { i, entry ->
                if (entry == null) return@forEachIndexed

                val (x, y) = offsets[i]
                val isHovered = i == hoveredIndex

                val bgColor = when {
                    showLastDoorOpener.value && DungeonListener.lastDoorOpenner == entry.player -> {
                        if (isHovered) Color(33, 33, 33).brighter().brighter().brighter().brighter().brighter().brighter()
                        else Color(33, 33, 33).brighter().brighter().brighter().brighter()
                    }

                    tintDeadPlayers.value && entry.player.isDead -> {
                        val color = MathUtils.lerpColor(Color(33, 33, 33), Color.RED, 0.2f)
                        if (isHovered) color.brighter()
                        else color
                    }

                    else -> {
                        if (isHovered) Color(33, 33, 33).brighter().brighter()
                        else Color(33, 33, 33)
                    }
                }

                Render2D.drawFloatingRect(event.context, x, y, boxWidth, boxHeight, bgColor.withAlpha(190))

                val headX = (x + 10).toInt()
                val headY = (y + (boxHeight / 2) - headSize / 2).toInt()

                Render2D.drawPlayerHead(event.context, headX, headY, headSize, entry.player.skin)
                Render2D.drawBorder(event.context, headX, headY, headSize, headSize, entry.player.clazz.color)

                val textX = (x + 10 + headSize + 5).toInt()
                val textY = (y + boxHeight / 2 - mc.font.lineHeight).toInt()

                Render2D.drawString(event.context, entry.player.name, textX, textY + 2, entry.player.clazz.color)

                val status = if (entry.player.isDead) "§cDEAD" else entry.player.clazz.name
                Render2D.drawString(event.context, status, textX, textY + 12, entry.player.clazz.color)
            }

            pose.popMatrix()
            Resolution.pop(event.context)
        }

        register<ContainerEvent.MouseClick> {
            if (! customLeapMenu.value) return@register
            if (! inSpiritLeap(event.screen)) return@register

            val cx = mc.window.width / 2
            val cy = mc.window.height / 2

            val quadrant = when {
                mc.mouseHandler.xpos() < cx && mc.mouseHandler.ypos() < cy -> 0
                mc.mouseHandler.xpos() > cx && mc.mouseHandler.ypos() < cy -> 1
                mc.mouseHandler.xpos() < cx && mc.mouseHandler.ypos() > cy -> 2
                mc.mouseHandler.xpos() > cx && mc.mouseHandler.ypos() > cy -> 3
                else -> return@register
            }

            event.isCanceled = true
            triggerLeap(quadrant)
        }

        register<ContainerEvent.Keyboard> {
            if (! customLeapMenu.value || ! leapKeybinds.value) return@register
            if (! inSpiritLeap(event.screen)) return@register

            val index = if (keybindMode.value == 0) when (event.key) {
                key1.value -> 0
                key2.value -> 1
                key3.value -> 2
                key4.value -> 3
                else -> return@register
            }
            else {
                val clazz = when (event.key) {
                    keyArcher.value -> DungeonClass.Archer
                    keyBerserk.value -> DungeonClass.Berserk
                    keyHealer.value -> DungeonClass.Healer
                    keyMage.value -> DungeonClass.Mage
                    keyTank.value -> DungeonClass.Tank
                    else -> return@register
                }
                players.indexOfFirst { it?.player?.clazz == clazz }.takeIf { it != - 1 } ?: return@register
            }

            event.isCanceled = true
            triggerLeap(index)
        }
    }


    private fun inSpiritLeap(screen: Screen): Boolean {
        val title = screen.title.string.lowercase()
        return (title.contains("spirit leap") || title.contains("teleport to player"))
            && LocationUtils.inDungeon
            && customLeapMenu.value
            && enabled
    }

    fun updateLeapMenu() {
        players.clear()
        val loadedHeads = mutableMapOf<String, Int>()

        mc.player?.containerMenu?.let { menu ->
            for (i in 0 until (menu.slots.size - 36)) {
                val stack = menu.slots[i].item ?: continue
                if (stack.isEmpty || ! stack.`is`(Items.PLAYER_HEAD)) continue
                val headName = playerRegex.find(stack.hoverName.string)?.groups?.get("name")?.value ?: continue
                loadedHeads[headName] = i
            }
        }

        val leapTeammates: List<DungeonPlayer?> = when (sorting.value) {
            0 -> dungeonTeammatesNoSelf.sortedWith(compareBy({ it.clazz.ordinal }, { it.name }))
            1 -> dungeonTeammatesNoSelf.sortedBy { it.name }
            2 -> odinSorting(dungeonTeammatesNoSelf).toList()
            3 -> dungeonTeammatesNoSelf.sortedBy {
                customLeapOrder.indexOf(it.name.lowercase()).takeIf { i -> i != - 1 } ?: Int.MAX_VALUE
            }

            else -> dungeonTeammatesNoSelf
        }

        leapTeammates.forEach { player ->
            if (player == null) players.add(null)
            else {
                val slotIndex = loadedHeads[player.name]
                if (slotIndex != null) players.add(LeapMenuPlayer(slotIndex, player))
                else players.add(null)
            }
        }
    }

    private fun triggerLeap(index: Int) {
        val entry = players[index] ?: return
        if (entry.player.isDead) return ChatUtils.modMessage("§3LeapMenu >> §c${entry.player.name} is dead!")

        mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F))
        GuiUtils.clickSlot(entry.slotIndex, ButtonType.LEFT)
        mc.player?.closeContainer()
    }

    fun odinSorting(teammates: List<DungeonPlayer>): Array<out DungeonPlayer?> {
        val neededSorting = mapOf(
            DungeonClass.Archer to listOf(DungeonClass.Mage, DungeonClass.Berserk, DungeonClass.Healer, DungeonClass.Tank),
            DungeonClass.Mage to listOf(DungeonClass.Archer, DungeonClass.Berserk, DungeonClass.Healer, DungeonClass.Tank),
            DungeonClass.Berserk to listOf(DungeonClass.Archer, DungeonClass.Mage, DungeonClass.Healer, DungeonClass.Tank),
            DungeonClass.Healer to listOf(DungeonClass.Archer, DungeonClass.Berserk, DungeonClass.Mage, DungeonClass.Tank),
            DungeonClass.Tank to listOf(DungeonClass.Archer, DungeonClass.Berserk, DungeonClass.Healer, DungeonClass.Mage)
        )[DungeonListener.thePlayer?.clazz] ?: return teammates.toTypedArray()

        val quadrants = arrayOfNulls<DungeonPlayer>(4)
        val secondRound = mutableListOf<DungeonPlayer>()

        for (player in teammates) {
            val preferredIndex = neededSorting.indexOf(player.clazz)
            if (preferredIndex != - 1 && preferredIndex < 4 && quadrants[preferredIndex] == null) {
                quadrants[preferredIndex] = player
            }
            else secondRound.add(player)
        }

        for (i in quadrants.indices) {
            if (quadrants[i] == null && secondRound.isNotEmpty()) {
                quadrants[i] = secondRound.removeFirst()
            }
        }

        return quadrants
    }
}