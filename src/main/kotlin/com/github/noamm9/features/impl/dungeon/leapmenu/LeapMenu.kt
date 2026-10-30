package com.github.noamm9.features.impl.dungeon.leapmenu

import com.github.noamm9.NoammAddons
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.KeybindSetting
import com.github.noamm9.config.types.ListSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.TextInputSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.CheckEntityRenderEvent
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.event.impl.ScreenEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.impl.dungeon.map.DungeonMap
import com.github.noamm9.features.impl.dungeon.map.MapConfig
import com.github.noamm9.init.types.ICustomMenu
import com.github.noamm9.ui.utils.Resolution
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.GuiUtils
import com.github.noamm9.utils.SoundUtils
import com.github.noamm9.utils.containsOneOf
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.remove
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.uppercaseFirst
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items

object LeapMenu: Feature("Custom Leap Menu and leap message"), ICustomMenu {
    private val customLeapMenu by ToggleSetting("Leap Menu", false).section("Custom Menu")
    private val renderMode by DropdownSetting("Render Mode", 0, listOf("Default", "Map", "Wheel")).withDescription("&nDefault&r is the standard 2x2 grid leap menu.\n&nMap&r lets you click a teammate's head on the dungeon map to leap to them.\n&nWheel&r is a proof of concept menu that on paper should be better than the usual 2x2, it shows all 5 players including yourself, so the order stays the same when you change class.").showIf { customLeapMenu.value }
    val sorting by DropdownSetting("Leap Order", 0, arrayListOf("A-Z Class", "A-Z Name", "Odin Sorting", "Custom sorting", "No Sorting")).withDescription("How to sort the leap menu. /na leaporder to configure custom sorting.")
    private val customLeapOrderSetting by ListSetting<String>("Custom Leap Order").hidden()
    private val customLeapTypeSetting by TextInputSetting("Custom Leap Type", "name").hidden()
    private val leftClickOnly by ToggleSetting("Left Click Only").withDescription("Prevents leaping unless clicked with left mouse button")

    val scale by SliderSetting("Menu Scale", 50f, 30, 100, 1).showIf { customLeapMenu.value }.section("Default")
    val showLastDoorOpener by ToggleSetting("Show Last Door Opener", false).showIf { customLeapMenu.value }
    val tintDeadPlayers by ToggleSetting("Tint Dead Players", true).showIf { customLeapMenu.value }
    val highlightInCore by ToggleSetting("Highlight Players in core", true).withDescription("Hightlights teammates in green if they are inside core in F7/M7 S3").showIf { customLeapMenu.value }

    val mapLeapScale by SliderSetting("Map Leap Scale", 1.5f, 0.5f, 3f, 0.1f).showIf { customLeapMenu.value && renderMode.value == 1 }.section("Map")
    private val mapLeapAfterBlood by ToggleSetting("After Blood Only", false).withDescription("Use the Default leap menu until the Blood opens, then switch to Map.").showIf { customLeapMenu.value && renderMode.value == 1 }

    private val leapKeybinds by ToggleSetting("Leap Keybinds").showIf { customLeapMenu.value }.section("Leap Keybinds")
    private val keybindMode by DropdownSetting("Mode", 0, listOf("Corners", "Class")).showIf { leapKeybinds.value }
    private val keybindKeys = (0 until 4).map { i -> KeybindSetting("Slot ${1 + i}", InputConstants.KEY_1 + i).showIf { leapKeybinds.value && keybindMode.value == 0 }.apply(configSettings::add) }
    private val classesKeys = DungeonClass.entries.dropLast(1).map { KeybindSetting(it.name.lowercase().uppercaseFirst(), InputConstants.UNKNOWN.value).showIf { leapKeybinds.value && keybindMode.value == 1 }.apply(configSettings::add) }

    private val announceSpiritLeaps by ToggleSetting("Announce Leap", true).section("Extras")
    private val leapMsg by TextInputSetting("Leap Message", "Leaping to {name}").withDescription("replaces {name} with the player name").showIf { announceSpiritLeaps.value }
    private val hideAfterLeap by ToggleSetting("Hide Players").withDescription("Hides players for a certain amount of time after you leap")
    private val hideTime by SliderSetting("Hide Time", 3.5, 0.5, 5.0, 0.1).showIf { hideAfterLeap.value }

    data class LeapMenuPlayer(val slotIndex: Int, val player: DungeonPlayer)

    val players = Array<LeapMenuPlayer?>(4) { null }
    private var clicked = false

    private val mapLeapAvailable get() = DungeonMap.enabled && MapConfig.mapEnabled.value && ! LocationUtils.inBoss && (! mapLeapAfterBlood.value || DungeonListener.bloodOpenTime != null)

    private val renderer: LeapMenuRenderer
        get() = when (renderMode.value) {
            0 -> GridLeapRenderer
            1 if mapLeapAvailable -> MapLeapRenderer
            2 -> WheelLeapRenderer

            else -> GridLeapRenderer
        }

    private val playerRegex = Regex("(?:\\[.+?] )?(?<name>\\w+)")
    private var shouldHide: Long = 0

    var customLeapOrder: List<String>
        get() = customLeapOrderSetting.value
        set(value) {
            customLeapOrderSetting.value = value.toMutableList()
        }

    var customLeapType: String
        get() = customLeapTypeSetting.value
        set(value) {
            customLeapTypeSetting.value = value
        }

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
            if (DungeonListener.dungeonTeammatesNoSelf.none { it.name == event.entity.name.unformattedText }) return@register
            event.isCanceled = true
        }

        register<ScreenEvent.PreRender> {
            if (! customLeapMenu.value) return@register
            if (! event.screen.isLeapMenu()) return@register
            event.isCanceled = true

            updateLeapMenu()

            if (players.filterNotNull().isEmpty()) {
                event.context.drawCenteredString(
                    "§4§lNo players found",
                    Resolution.width / 2,
                    Resolution.height / 2,
                    scale = 4
                )
                return@register
            }

            renderer.render(event.context, event.mouseX, event.mouseY)
        }

        register<ContainerEvent.MouseClick> {
            if (! event.screen.isLeapMenu()) return@register

            if (event.button != InputConstants.MOUSE_BUTTON_LEFT && leftClickOnly.value) {
                event.isCanceled = true
                return@register
            }

            val i = renderer.getHoveredIndex() ?: return@register
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
                0 -> keybindKeys.indexOfFirst { it.value == event.key }.let { slot ->
                    if (renderer == WheelLeapRenderer) WheelLeapRenderer.getKeybindIndex(slot) else slot
                }
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

        sortTeammates(DungeonListener.dungeonTeammatesNoSelf).take(4).forEachIndexed { i, player ->
            val slotIndex = loadedHeads[player.name]
            if (slotIndex != null) players[i] = LeapMenuPlayer(slotIndex, player)
        }
    }

    fun sortTeammates(teammates: List<DungeonPlayer>): List<DungeonPlayer> {
        return when (sorting.value) {
            0 -> teammates.sortedWith(compareBy({ it.clazz.ordinal }, { it.name }))
            1 -> teammates.sortedBy { it.name }
            2 -> odinSorting(teammates).filterNotNull()
            3 -> {
                val sorted = teammates.toMutableList()
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

            else -> teammates
        }
    }

    private fun triggerLeap(index: Int) {
        val entry = players.getOrNull(index) ?: return
        if (entry.player.isDead) return ChatUtils.modMessage("§3LeapMenu >> §c${entry.player.name} is dead!")

        SoundUtils.playSound(SoundEvents.UI_BUTTON_CLICK, 0.25f, 1f)
        GuiUtils.clickSlot(entry.slotIndex, GuiUtils.ButtonType.LEFT)
        if (NoammAddons.isCheat) player.closeContainer() else clicked = true
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

        return quadrants + secondRound
    }

    private val odinSorting = mapOf(
        DungeonClass.Archer to listOf(DungeonClass.Mage, DungeonClass.Berserk, DungeonClass.Healer, DungeonClass.Tank),
        DungeonClass.Mage to listOf(DungeonClass.Archer, DungeonClass.Berserk, DungeonClass.Healer, DungeonClass.Tank),
        DungeonClass.Berserk to listOf(DungeonClass.Archer, DungeonClass.Mage, DungeonClass.Healer, DungeonClass.Tank),
        DungeonClass.Healer to listOf(DungeonClass.Archer, DungeonClass.Berserk, DungeonClass.Mage, DungeonClass.Tank),
        DungeonClass.Tank to listOf(DungeonClass.Archer, DungeonClass.Berserk, DungeonClass.Healer, DungeonClass.Mage)
    )

    override fun isActive() = mc.gui.screen()?.isLeapMenu() ?: false
    private fun Screen.isLeapMenu() = enabled && customLeapMenu.value && LocationUtils.inDungeon && title.string.lowercase().containsOneOf("spirit leap", "teleport to player")
}