package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.config.ConfigHolder
import com.github.noamm9.config.SettingProvider
import com.github.noamm9.config.types.ColorConfig
import com.github.noamm9.config.types.ChoiceConfig
import com.github.noamm9.config.types.NumberConfig
import com.github.noamm9.config.types.BooleanConfig
import com.github.noamm9.utils.ColorUtils.withAlpha
import java.awt.Color

object MapConfig: SettingProvider {
    override val configSettings = mutableSetOf<ConfigHolder<*>>()

    val mapEnabled by BooleanConfig("Map Enabled", true).section("Map")

    //#if CHEAT
    val dungeonMapCheater by BooleanConfig("Cheater Map", false)
    //#else
    //$val dungeonMapCheater by BooleanConfig("Legit", false).hideIf { true }
    //#endif

    val mapExtraInfo by BooleanConfig("Show Extra Info Under Map", false)
    val mapHideInBoss by BooleanConfig("Hide In Boss", false)
    val printPlayersClearInfo by BooleanConfig("Player Clear Info", false)
    val playerNames by ChoiceConfig("Show Player Names", 0, listOf("Off", "Holding Leap", "Always"))
    val mapVanillaMarker by BooleanConfig("Vanilla Head Marker", false)

    val textScale by NumberConfig("Map Text Scale", 1f, 0.4f, 1.5f, 0.1).section("Size")
    val checkmarkSize by NumberConfig("Map Checkmark Scale", 1f, 0.3f, 1.5f, 0.1f)
    val playerHeadScale by NumberConfig("Player Heads Scale", 1f, 0.3f, 1.5f, 0.1f)
    val playerNameScale by NumberConfig("Player Name Scale", 0.5f, 0.3f, 1.5f, 0.1f)

    val mapBackground by ColorConfig("Map Background Color", Color(255, 255, 255, 50), true)
    val mapBorderColor by ColorConfig("Map Border Color", Color(255, 255, 255), true)
    val mapBorderWidth by NumberConfig("Border Thickness", 1, 1, 5, 1)

    val dungeonMapCheckmarkStyle by ChoiceConfig("Checkmark Style", 0, listOf("Checkmarks", "Secrets", "Room Name", "Room Name + Secrets")).section("Rooms")
    val centerStyle by BooleanConfig("Center Checkmark", true)
    val hideQuestionCheckmarks by BooleanConfig("Hide Unknown Room Checkmark", false).showIf { dungeonMapCheckmarkStyle.value == 0 }
    val limitRoomNameSize by BooleanConfig("Limit Room Name Size", true).showIf { dungeonMapCheckmarkStyle.value == 2 || dungeonMapCheckmarkStyle.value == 3 }

    //#if CHEAT
    val highlightMimicRoom by BooleanConfig("Highlight Mimic Room", true)
    val mimicEsp by BooleanConfig("Mimic ESP")

    //#else
    //$val highlightMimicRoom by BooleanConfig("Highlight Mimic Room Legit", false).hideIf { true }
    //$val mimicEsp by BooleanConfig("Mimic ESP Legit", false).hideIf { true }
    //#endif
    val mimicEspColor by ColorConfig("Mimic ESP Color", Color(255, 0, 0, 50), true).showIf { mimicEsp.value }

    val mapPlayerHeadColor by ColorConfig("Head Border", Color(0, 0, 0), true).section("Colors")
    val mapVanillaMarkerColor by ColorConfig("Vanilla Head Marker", Color(0, 255, 0), true)
    val mapPlayerHeadColorClassBased by BooleanConfig("Head Border Class Base", false)
    val mapPlayerNameClassColorBased by BooleanConfig("Player Names Class Base", false)

    val colorMimic by ColorConfig("Mimic Room", Color(255, 0, 0), true).showIf { highlightMimicRoom.value }
    val colorUnopened by ColorConfig("Unopened Room", Color(65, 65, 65), true)
    val colorBlood by ColorConfig("Blood Room", Color(178, 0, 0), true)
    val colorFairy by ColorConfig("Fairy Room", Color(227, 155, 226), true)
    val colorRare by ColorConfig("Rare Room", Color(178, 178, 178), true)
    val colorMiniboss by ColorConfig("Miniboss Room", Color(255, 200, 0), true)
    val colorPuzzle by ColorConfig("Puzzle Room", Color(123, 0, 123), true)
    val colorTrap by ColorConfig("Trap Room", Color(255, 130, 0), true)
    val colorRoom by ColorConfig("Normal Room", Color(121, 70, 0), true)
    val colorEntrance by ColorConfig("Entrance Room", Color(0, 255, 0), true)

    val colorUnopenedDoor by ColorConfig("Unopened Door", colorUnopened.value, true)
    val colorBloodDoor by ColorConfig("Blood Door", colorBlood.value, true)
    val colorWitherDoor by ColorConfig("Wither Door", Color(16, 16, 16), true)
    val colorRoomDoor by ColorConfig("Normal Door", colorRoom.value, true)
    val colorOpenWitherDoor by ColorConfig("Opened Wither Door", colorRoom.value, true)
    val colorEntranceDoor by ColorConfig("Entrance Door", colorEntrance.value, true)

    val boxDoors by BooleanConfig("Box Wither Doors").section("Door ESP")
    val highlightAllDoors by BooleanConfig("Highlight All Doors")
        .withDescription("Highlights every unopened door instead of only the next door after the run starts.")
        .showIf { boxDoors.value && dungeonMapCheater.value }
    val boxDoorsMode by ChoiceConfig("Highlight Mode", 2, listOf("Outline", "Fill", "Filled Outline"))
    val doorNoKeyColor by ColorConfig("No Key Color ", Color.RED.withAlpha(100)).showIf { boxDoors.value }
    val doorKeyColor by ColorConfig("Has Key Color ", Color.GREEN.withAlpha(100)).showIf { boxDoors.value }
}