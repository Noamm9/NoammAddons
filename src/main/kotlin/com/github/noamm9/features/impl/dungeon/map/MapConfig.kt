package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.ui.clickgui.components.Setting
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.SliderSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.ui.clickgui.components.showIf
import java.awt.Color

//#if LEGIT
//$$import com.github.noamm9.ui.clickgui.components.hideIf
//#endif

object MapConfig {
    val mapEnabled = ToggleSetting("Map Enabled", true).section("Map")

    //#if CHEAT
    val dungeonMapCheater = ToggleSetting("Cheater Map", false)
    //#else
    //$$val dungeonMapCheater = ToggleSetting("Legit", false).hideIf { true }
    //#endif

    val mapExtraInfo = ToggleSetting("Show Extra Info Under Map", false)
    val mapHideInBoss = ToggleSetting("Hide In Boss", false)
    val printPlayersClearInfo = ToggleSetting("Player Clear Info", false)
    val playerNames = DropdownSetting("Show Player Names", 0, listOf("Off", "Holding Leap", "Always"))
    val mapVanillaMarker = ToggleSetting("Vanilla Head Marker", false)

    val textScale = SliderSetting("Map Text Scale", 1f, 0.4f, 1.5f, 0.1).section("Size")
    val checkmarkSize = SliderSetting("Map Checkmark Scale", 1f, 0.3f, 1.5f, 0.1f)
    val playerHeadScale = SliderSetting("Player Heads Scale", 1f, 0.3f, 1.5f, 0.1f)
    val playerNameScale = SliderSetting("Player Name Scale", 0.5f, 0.3f, 1.5f, 0.1f)

    val mapBackground = ColorSetting("Map Background Color", Color(255, 255, 255, 50), true)
    val mapBorderColor = ColorSetting("Map Border Color", Color(255, 255, 255), true)
    val mapBorderWidth = SliderSetting("Border Thickness", 1, 1, 5, 1)

    val dungeonMapCheckmarkStyle = DropdownSetting("Checkmark Style", 0, listOf("Checkmarks", "Secrets", "Room Name", "Room Name + Secrets")).section("Rooms")
    val centerStyle = ToggleSetting("Center Checkmark", true)
    val hideQuestionCheckmarks = ToggleSetting("Hide Unknown Room Checkmark", false).showIf { dungeonMapCheckmarkStyle.value == 0 }
    val limitRoomNameSize = ToggleSetting("Limit Room Name Size", true).showIf { dungeonMapCheckmarkStyle.value == 2 || dungeonMapCheckmarkStyle.value == 3 }

    //#if CHEAT
    val highlightMimicRoom = ToggleSetting("Highlight Mimic Room", true)
    //#else
    //$$val highlightMimicRoom = ToggleSetting("Highlight Mimic Room Legit", false).hideIf { true }
    //#endif

    val mapPlayerHeadColor = ColorSetting("Head Border", Color(0, 0, 0), true).section("Colors")
    val mapPlayerHeadColorClassBased = ToggleSetting("Head Border Class Base", false)
    val mapPlayerNameClassColorBased = ToggleSetting("Player Names Class Base", false)

    val colorBlood = ColorSetting("Blood Room", Color(178, 0, 0), true)
    val colorEntrance = ColorSetting("Entrance Room", Color(0, 255, 0), true)
    val colorFairy = ColorSetting("Fairy Room", Color(227, 155, 226), true)
    val colorMiniboss = ColorSetting("Miniboss Room", Color(255, 200, 0), true)
    val colorRoom = ColorSetting("Normal Room", Color(121, 70, 0), true)
    val colorPuzzle = ColorSetting("Puzzle Room", Color(123, 0, 123), true)
    val colorMimic = ColorSetting("Mimic Room", Color(255, 0, 0), true).showIf { highlightMimicRoom.value }
    val colorRare = ColorSetting("Rare Room", Color(178, 178, 178), true)
    val colorTrap = ColorSetting("Trap Room", Color(255, 130, 0), true)
    val colorUnopened = ColorSetting("Unopened Room", Color(65, 65, 65), true)

    val colorBloodDoor = ColorSetting("Blood Door", colorBlood.value, true)
    val colorEntranceDoor = ColorSetting("Entrance Door", colorEntrance.value, true)
    val colorRoomDoor = ColorSetting("Normal Door", colorRoom.value, true)
    val colorWitherDoor = ColorSetting("Wither Door", Color(16, 16, 16), true)
    val colorOpenWitherDoor = ColorSetting("Opened Wither Door", colorRoom.value, true)
    val colorUnopenedDoor = ColorSetting("Unopened Door", colorUnopened.value, true)

    val boxWitherDoors = ToggleSetting("Box Wither Doors", true).section("Wither Door ESP")
    val witherDoorNoKeyColor = ColorSetting("No Key Color", Color(255, 0, 0), false).showIf { boxWitherDoors.value }
    val witherDoorKeyColor = ColorSetting("Has Key Color", Color(0, 255, 0), false).showIf { boxWitherDoors.value }
    val witherDoorFill = SliderSetting("Door Fill Opacity", 40, 0, 100, 1).showIf { boxWitherDoors.value }

    fun setup(): Array<Setting<*>> {
        return arrayOf(
            mapEnabled, dungeonMapCheater, mapExtraInfo, mapHideInBoss,
            printPlayersClearInfo, playerNames, mapVanillaMarker, textScale,
            checkmarkSize, playerHeadScale, playerNameScale, mapBackground,
            mapBorderColor, mapBorderWidth, dungeonMapCheckmarkStyle, centerStyle,
            hideQuestionCheckmarks, limitRoomNameSize, highlightMimicRoom,
            mapPlayerHeadColor, mapPlayerHeadColorClassBased, mapPlayerNameClassColorBased,
            colorBlood, colorEntrance, colorFairy, colorMiniboss, colorRoom, colorPuzzle,
            colorMimic, colorRare, colorTrap, colorUnopened, colorBloodDoor,
            colorEntranceDoor, colorRoomDoor, colorWitherDoor, colorOpenWitherDoor,
            colorUnopenedDoor, boxWitherDoors, witherDoorNoKeyColor, witherDoorKeyColor,
            witherDoorFill
        )
    }
}