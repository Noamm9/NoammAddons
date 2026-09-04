package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dungeon.map.*
import com.github.noamm9.init.types.ICustomMenu
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.*
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonListener.dungeonTeammatesNoSelf
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.dungeons.map.utils.MapUtils
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawFloatingRect
import com.github.noamm9.utils.render.Render2D.drawPlayerHead
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.RenderHelper.renderVec
import gg.essential.universal.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import java.awt.Color

object LeapMenu: Feature("Custom Leap Menu and leap message"), ICustomMenu {
    private val customLeapMenu by ToggleSetting("Leap Menu", false).section("Custom Menu")
    private val scale by SliderSetting("Menu Scale", 50f, 30, 100, 1).showIf { customLeapMenu.value }
    private val showLastDoorOpener by ToggleSetting("Show Last Door Opener", false).showIf { customLeapMenu.value }
    private val tintDeadPlayers by ToggleSetting("Tint Dead Players", true).showIf { customLeapMenu.value }
    private val leftClickOnly by ToggleSetting("Left Click Only").withDescription("Prevents leaping unless clicked with left mouse button")

    private val mapLeap by ToggleSetting("Map Leap", false).withDescription("Click a teammate on the dungeon map to leap to them.").showIf { customLeapMenu.value && DungeonMap.enabled && MapConfig.mapEnabled.value }
    private val mapLeapAfterBlood by ToggleSetting("After Blood Only", false).withDescription("Use the regular leap menu until the Blood opens, then switch to Map Leap.").showIf { customLeapMenu.value && mapLeap.value }
    private val mapLeapScale by SliderSetting("Map Leap Scale", 1.5f, 0.5f, 3f, 0.1f).showIf { customLeapMenu.value && mapLeap.value }
    val sorting by DropdownSetting("Leap Order", 0, arrayListOf("A-Z Class", "A-Z Name", "Odin Sorting", "Custom sorting", "No Sorting")).withDescription("How to sort the leap menu. /na leaporder to configure custom sorting.")

    private val leapKeybinds by ToggleSetting("Leap Keybinds").showIf { customLeapMenu.value }.section("Leap Keybinds")
    private val keybindMode by DropdownSetting("Mode", 0, listOf("Corners", "Class")).showIf { leapKeybinds.value }
    private val keybindKeys = (0 until 4).map { i -> KeybindSetting("Slot ${1 + i}", UKeyboard.KEY_1 + i).showIf { leapKeybinds.value && keybindMode.value == 0 }.apply(configSettings::add) }
    private val classesKeys = DungeonClass.entries.dropLast(1).map { KeybindSetting(it.name.lowercase().uppercaseFirst(), UKeyboard.KEY_NONE).showIf { leapKeybinds.value && keybindMode.value == 1 }.apply(configSettings::add) }

    private val announceSpiritLeaps by ToggleSetting("Announce Leap", true).section("Extras")
    private val leapMsg by TextInputSetting("Leap Message", "ILY ❤ {name}").withDescription("replaces {name} with the player name").showIf { announceSpiritLeaps.value }
    private val hideAfterLeap by ToggleSetting("Hide Players").withDescription("Hides players for a certain amount of time after you leap")
    private val hideTime by SliderSetting("Hide Time", 3.5, 0.5, 5.0, 0.1).showIf { hideAfterLeap.value }

    data class LeapMenuPlayer(val slotIndex: Int, val player: DungeonPlayer)

    val players = Array<LeapMenuPlayer?>(4) { null }
    private var mapLeapHoveredIndex: Int? = null
    private var clicked = false

    private val boxBg = Color(33, 33, 33)
    private val boxBgHover = Color(67, 67, 67)
    private val doorOpenerBg = Color(135, 135, 135)
    private val doorOpenerBgHover = Color.WHITE

    private fun leapBoxColor(player: DungeonPlayer, isHovered: Boolean) = when {
        showLastDoorOpener.value && DungeonListener.lastDoorOpenner == player -> if (isHovered) doorOpenerBgHover else doorOpenerBg
        tintDeadPlayers.value && player.isDead -> MathUtils.lerpColor(boxBg, Color.RED, 0.2f).let { if (isHovered) it.brighter() else it }
        else -> if (isHovered) boxBgHover else boxBg
    }

    private val mapLeapEnabled get() = customLeapMenu.value && mapLeap.value && DungeonMap.enabled && MapConfig.mapEnabled.value && (! mapLeapAfterBlood.value || DungeonListener.bloodOpenTime != null)

    private val playerRegex = Regex("(?:\\[.+?] )?(?<name>\\w+)")
    private var shouldHide: Long = 0

    var customLeapOrder = listOf<String>()
    var customLeapType = "name"

    override fun init() {
        register<ChatMessageEvent> {
            val message = event.unformattedText
            if (! message.endsWith("!")) return@register
            if (! message.startsWith("You have teleported to ")) return@register
            val name = message.remove("You have teleported to ", "!")

            if (announceSpiritLeaps.value) leapMsg.value.replace("{name}", name).takeUnless { it.isBlank() }?.let {
                ChatUtils.sendPartyMessage(it)
            }

            if (hideAfterLeap.value) shouldHide = System.currentTimeMillis() + ((hideTime.value * 1000L).toLong())
        }

        register<CheckEntityRenderEvent> {
            if (System.currentTimeMillis() > shouldHide) return@register
            if (event.entity !is Player) return@register
            if (event.entity == player) return@register
            if (event.entity.distanceToSqr(player) > 4) return@register
            if (dungeonTeammatesNoSelf.none { it.name == event.entity.name.unformattedText }) return@register
            event.isCanceled = true
        }

        register<ScreenEvent.PreRender> {
            if (! customLeapMenu.value) return@register
            if (! event.screen.isLeapMenu()) return@register
            event.isCanceled = true

            updateLeapMenu()

            if (players.filterNotNull().isEmpty()) {
                event.context.drawCenteredString("§4§lNo players found", Resolution.width / 2, Resolution.height / 2)
                return@register
            }

            if (mapLeapEnabled && ! LocationUtils.inBoss) {
                renderMapLeap(event.context, event.mouseX, event.mouseY)
                return@register
            }

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

            val hoveredIndex = getHoveredIndex()

            players.forEachIndexed { i, entry ->
                if (entry == null) return@forEachIndexed

                val (x, y) = offsets[i]
                val isHovered = i == hoveredIndex

                val bgColor = leapBoxColor(entry.player, isHovered)

                event.context.drawFloatingRect(x, y, boxWidth, boxHeight, bgColor.withAlpha(190))

                val headX = (x + 10).toInt()
                val headY = (y + (boxHeight / 2) - headSize / 2).toInt()

                event.context.drawPlayerHead(headX, headY, headSize, entry.player.skin)
                event.context.drawBorder(headX, headY, headSize, headSize, entry.player.clazz.color)

                val textX = (x + 10 + headSize + 5).toInt()
                val textY = (y + boxHeight / 2 - UGraphics.getFontHeight()).toInt()

                event.context.drawString(entry.player.name, textX, textY + 2, entry.player.clazz.color)

                val status = if (entry.player.isDead) "§cDEAD" else entry.player.clazz.name
                event.context.drawString(status, textX, textY + 12, entry.player.clazz.color)
            }

            pose.popMatrix()
            Resolution.pop(event.context)
        }

        register<ContainerEvent.MouseClick> {
            if (! event.screen.isLeapMenu()) return@register

            if (event.button != 0 && leftClickOnly.value) {
                event.isCanceled = true
                return@register
            }

            val i = (if (mapLeapEnabled && ! LocationUtils.inBoss) mapLeapHoveredIndex else getHoveredIndex()) ?: return@register
            event.isCanceled = true
            triggerLeap(i)
        }

        register<ContainerEvent.Keyboard> {
            if (! event.screen.isLeapMenu()) return@register
            if (clicked) {
                clicked = false
                player.closeContainer()
            }

            if (! leapKeybinds.value) return@register
            val index = when (keybindMode.value) {
                0 -> keybindKeys.indexOfFirst { it.value == event.key }
                1 -> classesKeys.find { it.value == event.key }?.let { key ->
                    players.indexOfFirst { it?.player?.clazz == DungeonClass.fromName(key.name) }
                }

                else -> - 1
            }?.takeIf { it in players.indices } ?: return@register

            event.isCanceled = true
            triggerLeap(index)
        }

        register<MainThreadPacketReceivedEvent.Post> { if (event.packet is ClientboundContainerClosePacket) clicked = false }
        register<PacketEvent.Sent> { if (event.packet is ServerboundContainerClosePacket) clicked = false }
    }

    private fun getHoveredIndex(): Int? {
        val cx = UResolution.windowWidth / 2
        val cy = UResolution.windowHeight / 2
        val mx = UMouse.Raw.x
        val my = UMouse.Raw.y

        return when {
            mx < cx && my < cy -> 0
            mx > cx && my < cy -> 1
            mx < cx && my > cy -> 2
            mx > cx && my > cy -> 3
            else -> null
        }
    }

    fun updateLeapMenu() {
        players.fill(null)

        val loadedHeads = mutableMapOf<String, Int>()
        val menu = player.containerMenu

        for (i in 0 until (menu.slots.size - 36)) {
            val stack = menu.slots[i].item
            if (stack.isEmpty || ! stack.`is`(Items.PLAYER_HEAD)) continue
            val headName = playerRegex.find(stack.hoverName.string)?.groups?.get("name")?.value ?: continue
            loadedHeads[headName] = i
        }

        val leapTeammates: List<DungeonPlayer> = when (sorting.value) {
            0 -> dungeonTeammatesNoSelf.sortedWith(compareBy({ it.clazz.ordinal }, { it.name }))
            1 -> dungeonTeammatesNoSelf.sortedBy { it.name }
            2 -> odinSorting(dungeonTeammatesNoSelf).filterNotNull()
            3 -> {
                val sorted = dungeonTeammatesNoSelf.toMutableList()
                sorted.sortWith(Comparator { a, b ->
                    val type = customLeapType.lowercase()
                    val priorityA = when (type) {
                        "name" -> customLeapOrder.indexOf(a.name.lowercase())
                        "class" -> customLeapOrder.indexOf(a.clazz.name.lowercase())
                        else -> - 1
                    }.takeIf { it != - 1 } ?: Int.MAX_VALUE

                    val priorityB = when (type) {
                        "name" -> customLeapOrder.indexOf(b.name.lowercase())
                        "class" -> customLeapOrder.indexOf(b.clazz.name.lowercase())
                        else -> - 1
                    }.takeIf { it != - 1 } ?: Int.MAX_VALUE

                    if (priorityA != priorityB) priorityA.compareTo(priorityB)
                    else when (type) {
                        "name" -> a.name.lowercase().compareTo(b.name.lowercase())
                        "class" -> a.clazz.name.lowercase().compareTo(b.clazz.name.lowercase())
                        else -> 0
                    }
                })
                sorted
            }

            else -> dungeonTeammatesNoSelf
        }

        leapTeammates.take(4).forEachIndexed { i, player ->
            val slotIndex = loadedHeads[player.name]
            if (slotIndex != null) players[i] = LeapMenuPlayer(slotIndex, player)
        }
    }

    private fun triggerLeap(index: Int) {
        val entry = players.getOrNull(index) ?: return
        if (entry.player.isDead) return ChatUtils.modMessage("§3LeapMenu >> §c${entry.player.name} is dead!")

        USound.playButtonPress()
        GuiUtils.clickSlot(entry.slotIndex, GuiUtils.ButtonType.LEFT)
        if (NoammAddons.isCheat) player.closeContainer() else clicked = true
    }

    private fun renderMapLeap(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        Resolution.push(ctx)

        val sw = Resolution.width
        val sh = Resolution.height
        val mx = Resolution.getMouseX(mouseX).toFloat()
        val my = Resolution.getMouseY(mouseY).toFloat()
        val mapSize = 128f

        val scale = mapLeapScale.value
        val mapPix = mapSize * scale

        val mapOx = (sw - mapPix) / 2f
        val mapOy = (sh - mapPix - (8 * scale)) / 2f

        fun playerDot(player: DungeonPlayer): Triple<Float, Float, Float> {
            val (pmx, pmz) = teammateMapPos(player)
            return Triple(mapOx + pmx * scale, mapOy + pmz * scale, 7f * scale)
        }

        mapLeapHoveredIndex = null

        players.forEachIndexed { i, entry ->
            if (entry == null || entry.player.isDead) return@forEachIndexed
            val (px, py, r) = playerDot(entry.player)
            if (mx in px - r .. px + r && my in py - r .. py + r) mapLeapHoveredIndex = i
        }

        ctx.drawRect(0, 0, sw, sh, Color.BLACK.withAlpha(170))

        val pose = ctx.pose()
        pose.pushMatrix()
        pose.translate(mapOx, mapOy)
        pose.scale(scale)
        MapRenderer.draw(ctx, example = false, extraInfo = false)
        pose.popMatrix()

        mapLeapHoveredIndex?.let { idx ->
            players[idx]?.let { entry ->
                val (px, py, r) = playerDot(entry.player)
                ctx.drawBorder(px - r, py - r, r * 2f, r * 2f, thickness = 2)
            }
        }

        Resolution.pop(ctx)
    }

    private fun teammateMapPos(player: DungeonPlayer): Pair<Float, Float> {
        val entity = player.entity
        return if (entity == null || ! entity.isAlive) player.mapX to player.mapZ
        else MapUtils.coordsToMap(entity.renderVec)
    }

    fun odinSorting(teammates: List<DungeonPlayer>): Array<out DungeonPlayer?> {
        val neededSorting = odinSorting[DungeonListener.thePlayer?.clazz] ?: return teammates.toTypedArray()

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

    private val odinSorting = mapOf(
        DungeonClass.Archer to listOf(DungeonClass.Mage, DungeonClass.Berserk, DungeonClass.Healer, DungeonClass.Tank),
        DungeonClass.Mage to listOf(DungeonClass.Archer, DungeonClass.Berserk, DungeonClass.Healer, DungeonClass.Tank),
        DungeonClass.Berserk to listOf(DungeonClass.Archer, DungeonClass.Mage, DungeonClass.Healer, DungeonClass.Tank),
        DungeonClass.Healer to listOf(DungeonClass.Archer, DungeonClass.Berserk, DungeonClass.Mage, DungeonClass.Tank),
        DungeonClass.Tank to listOf(DungeonClass.Archer, DungeonClass.Berserk, DungeonClass.Healer, DungeonClass.Mage)
    )

    override fun isActive() = (UMinecraft.currentScreenObj as? Screen)?.isLeapMenu() ?: false
    private fun Screen.isLeapMenu() = enabled && customLeapMenu.value && LocationUtils.inDungeon && title.string.lowercase().containsOneOf("spirit leap", "teleport to player")
}