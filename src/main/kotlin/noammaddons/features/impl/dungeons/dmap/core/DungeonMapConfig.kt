package noammaddons.features.impl.dungeons.dmap.core

import noammaddons.ui.config.core.impl.*
import java.awt.Color


object DungeonMapConfig {
    val mapEnabled = ToggleSetting("Map Enabled", true)
    val dungeonMapCheater = ToggleSetting("Cheater Map", false)
    val mapExtraInfo = ToggleSetting("Show Extra Info Under Map", true)
    val mapHideInBoss = ToggleSetting("Hide In Boss", false)
    val printPlayersClearInfo = ToggleSetting("Player Clear Info", true)
    val playerHeads = DropdownSetting("Show Player Names", listOf("Off", "Holding Leap", "Always"), 1)
    val mapVanillaMarker = ToggleSetting("Vanilla Head Marker", false)
    val textScale = SliderSetting("Map Text Scale", 0f, 2f, 0.05f, 0.75f)
    val checkmarkSize = SliderSetting("Map Checkmark Scale", 0f, 2f, 0.05f, 1f)
    val playerHeadScale = SliderSetting("Player Heads Scale", 0f, 2f, 0.05f, 0.75f)
    val playerNameScale = SliderSetting("Player Name Scale", 0f, 2f, 0.05f, 0.8f)

    val mapBackgroundStyle = DropdownSetting("Map Background Style", listOf("Default", "Vanilla", "Disabled"), 0)
    val mapBackground = ColorSetting("Map Background Color", Color(255, 255, 255, 50), true).addDependency { mapBackgroundStyle.value != 0 }
    val mapBorderColor = ColorSetting("Map Border Color", Color(255, 255, 255), true).addDependency { mapBackgroundStyle.value != 0 }
    val mapBorderWidth = SliderSetting("Border Thickness", 0.0, 10.0, 0.5, 1.0).addDependency { mapBackgroundStyle.value != 0 }

    val mapRoomNames = DropdownSetting("Room Names", listOf("None", "Puzzles", "Puzzles / Trap", "All"), 2)
    val limitRoomNameSize = ToggleSetting("Limit Room Name Size", true)
    val dungeonMapCheckmarkStyle = DropdownSetting("Checkmark Style", listOf("Off", "Default Checkmarks", "Room Secrets", "Room Name"), 1)
    val hideQuestionCheckmarks = ToggleSetting("Hide Unknown Room Checkmark", false)
    val centerStyle = ToggleSetting("Center Checkmark Style", true)

    val mapVanillaMarkerColor = ColorSetting("Vanilla Head Marker", Color(0, 255, 0), true)
    val mapPlayerHeadColor = ColorSetting("Player Head Border", Color(0, 0, 0), true)
    val mapPlayerHeadColorClassBased = ToggleSetting("Player Head Border Class Base", false)
    val mapPlayerNameClassColorBased = ToggleSetting("Player Names Class Base", false)

    val colorBlood = ColorSetting("Blood Room", Color(178, 0, 0), true)
    val colorEntrance = ColorSetting("Entrance Room", Color(0, 255, 0), true)
    val colorFairy = ColorSetting("Fairy Room", Color(227, 155, 226), true)
    val colorMiniboss = ColorSetting("Miniboss Room", Color(255, 200, 0), true)
    val colorRoom = ColorSetting("Normal Room", Color(121, 70, 0), true)
    val colorPuzzle = ColorSetting("Puzzle Room", Color(123, 0, 123), true)
    val colorRare = ColorSetting("Rare Room", Color(178, 178, 178), true)
    val colorTrap = ColorSetting("Trap Room", Color(255, 130, 0), true)
    val colorUnopened = ColorSetting("Unopened Room", Color(65, 65, 65), true)

    val colorBloodDoor = ColorSetting("Blood Door", colorBlood.value, true)
    val colorEntranceDoor = ColorSetting("Entrance Door", colorEntrance.value, true)
    val colorRoomDoor = ColorSetting("Normal Door", colorRoom.value, true)
    val colorWitherDoor = ColorSetting("Wither Door", Color(16, 16, 16), true)
    val colorOpenWitherDoor = ColorSetting("Opened Wither Door", colorRoom.value, true)
    val colorUnopenedDoor = ColorSetting("Unopened Door", colorUnopened.value, true)

    val boxWitherDoors = ToggleSetting("Box Wither Doors", true)
    val witherDoorNoKeyColor = ColorSetting("No Key Color", Color(255, 0, 0), false).addDependency(boxWitherDoors)
    val witherDoorKeyColor = ColorSetting("Has Key Color", Color(0, 255, 0), false).addDependency(boxWitherDoors)
    val witherDoorOutlineWidth = SliderSetting("Door Outline Width", 1f, 10f, 0.5f, 3f).addDependency(boxWitherDoors)
    val witherDoorFill = SliderSetting("Door Fill Opacity", 0f, 1f, 0.05f, 0.25f).addDependency(boxWitherDoors)

    fun setup(): Array<CategorySetting> {
        return arrayOf(
            CategorySetting("Map", listOf(
                SeperatorSetting("Toggle"),
                mapEnabled,
                dungeonMapCheater,
                mapExtraInfo,
                mapHideInBoss,
                printPlayersClearInfo,
                playerHeads,
                mapVanillaMarker,

                SeperatorSetting("Size"),
                textScale,
                checkmarkSize,
                playerHeadScale,
                playerNameScale,

                SeperatorSetting("Render"),
                mapBackgroundStyle,
                mapBackground,
                mapBorderColor,
                mapBorderWidth,
            )
            ),

            CategorySetting("Rooms", listOf(
                SeperatorSetting("Style"),
                mapRoomNames,
                limitRoomNameSize,
                dungeonMapCheckmarkStyle,
                hideQuestionCheckmarks,
                centerStyle,
            )
            ),

            CategorySetting("Colors", listOf(
                SeperatorSetting("Markers"),
                mapVanillaMarkerColor,
                mapPlayerHeadColor,
                mapPlayerHeadColorClassBased,
                mapPlayerNameClassColorBased,

                SeperatorSetting("Rooms"),
                colorBlood,
                colorEntrance,
                colorFairy,
                colorMiniboss,
                colorRoom,
                colorPuzzle,
                colorRare,
                colorTrap,
                colorUnopened,

                SeperatorSetting("Doors"),
                colorBloodDoor,
                colorEntranceDoor,
                colorRoomDoor,
                colorWitherDoor,
                colorOpenWitherDoor,
                colorUnopenedDoor,
            )
            ),

            CategorySetting("Wither Door ESP", listOf(
                boxWitherDoors,
                witherDoorNoKeyColor,
                witherDoorKeyColor,
                witherDoorOutlineWidth,
                witherDoorFill,
            )
            )
        )
    }
}
