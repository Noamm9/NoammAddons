package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.SettingProvider
import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.DropdownSetting
import com.github.noamm9.config.types.SliderSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.utils.ColorUtils.withAlpha
import java.awt.Color

object MapConfig: SettingProvider {
    override val configSettings = mutableSetOf<ConfigHolder<*>>()

    val mapEnabled by ToggleSetting("Map Enabled", true).section("Map")

    //#if CHEAT
    val dungeonMapCheater by ToggleSetting("Cheater Map", false)
    //#else
    //$val dungeonMapCheater by ToggleSetting("Legit", false).hideIf { true }
    //#endif

    val mapExtraInfo by ToggleSetting("Show Extra Info Under Map", false)
    val mapHideInBoss by ToggleSetting("Hide In Boss", false)
    val printPlayersClearInfo by ToggleSetting("Player Clear Info", false)
    val playerNames by DropdownSetting("Show Player Names", 0, listOf("Off", "Holding Leap", "Always"))
    val mapVanillaMarker by ToggleSetting("Vanilla Head Marker", false)

    val textScale by SliderSetting("Map Text Scale", 1f, 0.4f, 1.5f, 0.1).section("Size")
    val checkmarkSize by SliderSetting("Map Checkmark Scale", 1f, 0.3f, 1.5f, 0.1f)
    val playerHeadScale by SliderSetting("Player Heads Scale", 1f, 0.3f, 1.5f, 0.1f)
    val playerNameScale by SliderSetting("Player Name Scale", 0.5f, 0.3f, 1.5f, 0.1f)

    val mapBackground by ColorSetting("Map Background Color", Color(255, 255, 255, 50), true)
    val mapBorderColor by ColorSetting("Map Border Color", Color(255, 255, 255), true)
    val mapBorderWidth by SliderSetting("Border Thickness", 1, 1, 5, 1)

    val dungeonMapCheckmarkStyle by DropdownSetting("Checkmark Style", 0, listOf("Checkmarks", "Secrets", "Room Name", "Room Name + Secrets")).section("Rooms")
    val centerStyle by ToggleSetting("Center Checkmark", true)
    val hideQuestionCheckmarks by ToggleSetting("Hide Unknown Room Checkmark", false).showIf { dungeonMapCheckmarkStyle.value == 0 }
    val limitRoomNameSize by ToggleSetting("Limit Room Name Size", true).showIf { dungeonMapCheckmarkStyle.value == 2 || dungeonMapCheckmarkStyle.value == 3 }

    //#if CHEAT
    val highlightMimicRoom by ToggleSetting("Highlight Mimic Room", true)
    val mimicEsp by ToggleSetting("Mimic ESP")

    //#else
    //$val highlightMimicRoom by ToggleSetting("Highlight Mimic Room Legit", false).hideIf { true }
    //$val mimicEsp by ToggleSetting("Mimic ESP Legit", false).hideIf { true }
    //#endif
    val mimicEspColor by ColorSetting("Mimic ESP Color", Color(255, 0, 0, 50), true).showIf { mimicEsp.value }

    val mapPlayerHeadColor by ColorSetting("Head Border", Color(0, 0, 0), true).section("Colors")
    val mapVanillaMarkerColor by ColorSetting("Vanilla Head Marker", Color(0, 255, 0), true).jsonName("Vanilla Head Marker Color")
    val mapPlayerHeadColorClassBased by ToggleSetting("Head Border Class Base", false)
    val mapPlayerNameClassColorBased by ToggleSetting("Player Names Class Base", false)

    val colorMimic by ColorSetting("Mimic Room", Color(255, 0, 0), true).showIf { highlightMimicRoom.value }
    val colorUnopened by ColorSetting("Unopened Room", Color(65, 65, 65), true)
    val colorBlood by ColorSetting("Blood Room", Color(178, 0, 0), true)
    val colorFairy by ColorSetting("Fairy Room", Color(227, 155, 226), true)
    val colorRare by ColorSetting("Rare Room", Color(178, 178, 178), true)
    val colorMiniboss by ColorSetting("Miniboss Room", Color(255, 200, 0), true)
    val colorPuzzle by ColorSetting("Puzzle Room", Color(123, 0, 123), true)
    val colorTrap by ColorSetting("Trap Room", Color(255, 130, 0), true)
    val colorRoom by ColorSetting("Normal Room", Color(121, 70, 0), true)
    val colorEntrance by ColorSetting("Entrance Room", Color(0, 255, 0), true)

    val colorUnopenedDoor by ColorSetting("Unopened Door", colorUnopened.value, true)
    val colorBloodDoor by ColorSetting("Blood Door", colorBlood.value, true)
    val colorWitherDoor by ColorSetting("Wither Door", Color(16, 16, 16), true)
    val colorRoomDoor by ColorSetting("Normal Door", colorRoom.value, true)
    val colorOpenWitherDoor by ColorSetting("Opened Wither Door", colorRoom.value, true)
    val colorEntranceDoor by ColorSetting("Entrance Door", colorEntrance.value, true)

    val boxDoors by ToggleSetting("Box Wither Doors").section("Door ESP")
    val highlightAllDoors by ToggleSetting("Highlight All Doors")
        .withDescription("Highlights every unopened door instead of only the next door after the run starts.")
        .showIf { boxDoors.value && dungeonMapCheater.value }
    val boxDoorsMode by DropdownSetting("Highlight Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    val doorNoKeyColor by ColorSetting("No Key Color ", Color.RED.withAlpha(100)).showIf { boxDoors.value }
    val doorKeyColor by ColorSetting("Has Key Color ", Color.GREEN.withAlpha(100)).showIf { boxDoors.value }
}