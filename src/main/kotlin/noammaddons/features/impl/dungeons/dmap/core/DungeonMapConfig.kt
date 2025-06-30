package noammaddons.features.impl.dungeons.dmap.core

import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.*
import noammaddons.NoammAddons.Companion.MOD_NAME
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color
import java.io.File
import kotlin.reflect.jvm.javaField

object DungeonMapConfig: Vigilant(
    File("./config/$MOD_NAME/dungeonMapConfig.toml"),
    "Dungeon Map",
    sortingBehavior = CategorySorting
) {
    @Property(
        name = "Map Enabled",
        type = PropertyType.SWITCH,
        description = "Render the map!",
        category = "Map",
        subcategory = "Toggle",
    )
    var mapEnabled = true

    @Property(
        name = "Cheater Map",
        type = PropertyType.CHECKBOX,
        description = "Dirty Cheater!",
        category = "Map",
        subcategory = "Toggle"
    )
    var dungeonMapCheater = false

    @Property(
        name = "Show Extra Info Under Map",
        type = PropertyType.SWITCH,
        description = "Shows extra info under the map like deaths, crypts etc.",
        category = "Map",
        subcategory = "Toggle",
    )
    var mapExtraInfo = true

    @Property(
        name = "Hide In Boss",
        type = PropertyType.CHECKBOX,
        description = "Hides the map in boss.",
        category = "Map",
        subcategory = "Toggle",
    )
    var mapHideInBoss = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Player Clear Info",
        description = "Sends at the end of the for each player his run secrets/room cleared and deaths.",
        category = "Map",
        subcategory = "Toggle",
    )
    var printPlayersClearInfo = true

    @Property(
        name = "Show Player Names",
        type = PropertyType.SELECTOR,
        description = "Show player name under player head",
        category = "Map",
        subcategory = "Toggle",
        options = ["Off", "Holding Leap", "Always"],
    )
    var playerHeads = 1

    @Property(
        name = "Vanilla Head Marker",
        type = PropertyType.CHECKBOX,
        description = "Uses the vanilla head marker for yourself.",
        category = "Map",
        subcategory = "Toggle",
    )
    var mapVanillaMarker = false

    @Property(
        name = "Map Text Scale",
        type = PropertyType.DECIMAL_SLIDER,
        description = "Scale of room names and secret counts relative to map size.",
        category = "Map",
        subcategory = "Size",
        maxF = 2f,
        decimalPlaces = 2,
    )
    var textScale = 0.75f

    @Property(
        name = "Map Checkmark Scale",
        type = PropertyType.DECIMAL_SLIDER,
        description = "Scale of checkmarks relative to map size.",
        category = "Map",
        subcategory = "Size",
        maxF = 2f,
        decimalPlaces = 2,
    )
    var checkmarkSize = 1f

    @Property(
        name = "Player Heads Scale",
        type = PropertyType.DECIMAL_SLIDER,
        description = "Scale of player heads relative to map size.",
        category = "Map",
        subcategory = "Size",
        maxF = 2f,
        decimalPlaces = 2,
    )
    var playerHeadScale = 0.75f

    @Property(
        name = "Player Name Scale",
        type = PropertyType.DECIMAL_SLIDER,
        description = "Scale of player names relative to head size.",
        category = "Map",
        subcategory = "Size",
        maxF = 2f,
        decimalPlaces = 2,
    )
    var playerNameScale = .8f

    @Property(
        name = "Map Background Style",
        type = PropertyType.SELECTOR,
        category = "Map",
        subcategory = "Render",
        options = ["Default", "Vanilla", "Disabled"]
    )
    var mapBackgroundStyle = 0

    @Property(
        name = "Map Background Color",
        type = PropertyType.COLOR,
        category = "Map",
        subcategory = "Render",
        allowAlpha = true,
    )
    var mapBackground = Color(255, 255, 255, 50)

    @Property(
        name = "Map Border Color",
        type = PropertyType.COLOR,
        category = "Map",
        subcategory = "Render",
        allowAlpha = true,
    )
    var mapBorderColor = Color(255, 255, 255)

    @Property(
        name = "Border Thickness",
        type = PropertyType.DECIMAL_SLIDER,
        category = "Map",
        subcategory = "Render",
        maxF = 10f,
    )
    var mapBorderWidth = 1f

    @Property(
        name = "Room Names",
        type = PropertyType.SELECTOR,
        description = "Shows names of rooms on map.",
        category = "Rooms",
        subcategory = "Style",
        options = ["None", "Puzzles", "Puzzles / Trap", "All"],
    )
    var mapRoomNames = 2

    @Property(
        name = "Limit Room Name Size",
        type = PropertyType.CHECKBOX,
        description = "Scales down the text size of room names if its too long to fit in the room.",
        category = "Rooms",
        subcategory = "Style",
    )
    var limitRoomNameSize = true

    @Property(
        name = "Checkmark Style",
        type = PropertyType.SELECTOR,
        description = "What to show as the room checkmarks.",
        category = "Rooms",
        subcategory = "Style",
        options = ["Off", "Default Checkmarks", "Room Secrets", "Room Name"]
    )
    var dungeonMapCheckmarkStyle = 1

    @Property(
        name = "Hide Unknown Room Checkmark",
        description = "Hides the \"?\" checkmark on unknown rooms.",
        type = PropertyType.CHECKBOX,
        category = "Rooms",
        subcategory = "Style",
    )
    var hideQuestionCheckmarks = false

    @Property(
        name = "Center Checkmark Style",
        type = PropertyType.SWITCH,
        description = "If the selected Checkmark Style will be drawn at the center of the room.",
        category = "Rooms",
        subcategory = "Style"
    )
    var centerStyle = true

    @Property(
        name = "Vanilla Head Marker",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Markers",
        allowAlpha = true
    )
    var mapVanillaMarkerColor = Color(0, 255, 0)

    @Property(
        name = "Player Head Border",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Markers",
        allowAlpha = true
    )
    var mapPlayerHeadColor = Color(0, 0, 0)

    @Property(
        name = "Player Head Border Class Base",
        description = "Colors the border by the class color of the player.",
        type = PropertyType.CHECKBOX,
        category = "Colors",
        subcategory = "Markers",
        allowAlpha = true
    )
    var mapPlayerHeadColorClassBased = false

    @Property(
        name = "Player Names Class Base",
        description = "Colors the name by the class color of the player.",
        type = PropertyType.CHECKBOX,
        category = "Colors",
        subcategory = "Markers",
        allowAlpha = true
    )
    var mapPlayerNameClassColorBased = false

    @Property(
        name = "Blood Room",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Rooms",
        allowAlpha = true,
    )
    var colorBlood = Color(178, 0, 0)

    @Property(
        name = "Entrance Room",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Rooms",
        allowAlpha = true,
    )
    var colorEntrance = Color(0, 255, 0)

    @Property(
        name = "Fairy Room",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Rooms",
        allowAlpha = true,
    )
    var colorFairy = Color(227, 155, 226)

    @Property(
        name = "Miniboss Room",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Rooms",
        allowAlpha = true,
    )
    var colorMiniboss = Color(255, 200, 0)

    @Property(
        name = "Normal Room",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Rooms",
        allowAlpha = true,
    )
    var colorRoom = Color(121, 70, 0)

    @Property(
        name = "Puzzle Room",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Rooms",
        allowAlpha = true,
    )
    var colorPuzzle = Color(123, 0, 123)

    @Property(
        name = "Rare Room",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Rooms",
        allowAlpha = true,
    )
    var colorRare = Color(178, 178, 178)

    @Property(
        name = "Trap Room",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Rooms",
        allowAlpha = true,
    )
    var colorTrap = Color(255, 130, 0)

    @Property(
        name = "Unopened Room",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Rooms",
        allowAlpha = true,
    )
    var colorUnopened = Color(65, 65, 65)

    @Property(
        name = "Blood Door",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Doors",
        allowAlpha = true,
    )
    var colorBloodDoor = colorBlood

    @Property(
        name = "Entrance Door",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Doors",
        allowAlpha = true,
    )
    var colorEntranceDoor = colorEntrance

    @Property(
        name = "Normal Door",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Doors",
        allowAlpha = true,
    )
    var colorRoomDoor = colorRoom

    @Property(
        name = "Wither Door",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Doors",
        allowAlpha = true,
    )
    var colorWitherDoor = Color(16, 16, 16)

    @Property(
        name = "Opened Wither Door",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Doors",
        allowAlpha = true,
    )
    var colorOpenWitherDoor = colorRoom

    @Property(
        name = "Unopened Door",
        type = PropertyType.COLOR,
        category = "Colors",
        subcategory = "Doors",
        allowAlpha = true,
    )
    var colorUnopenedDoor = colorUnopened


    @Property(
        name = "Box Wither Doors",
        description = "Boxes unopened wither doors.",
        type = PropertyType.SWITCH,
        category = "Wither Door ESP",
    )
    var boxWitherDoors = true

    @Property(
        name = "No Key Color",
        type = PropertyType.COLOR,
        category = "Wither Door ESP",
        allowAlpha = true,
    )
    var witherDoorNoKeyColor = Color(255, 0, 0)

    @Property(
        name = "Has Key Color",
        type = PropertyType.COLOR,
        category = "Wither Door ESP",
        allowAlpha = true,
    )
    var witherDoorKeyColor = Color(0, 255, 0)

    @Property(
        name = "Door Outline Width",
        type = PropertyType.DECIMAL_SLIDER,
        category = "Wither Door ESP",
        minF = 1f,
        maxF = 10f,
    )
    var witherDoorOutlineWidth = 3f

    @Property(
        name = "Door Fill Opacity",
        type = PropertyType.PERCENT_SLIDER,
        category = "Wither Door ESP",
    )
    var witherDoorFill = 0.25f

    init {
        initialize()
        setCategoryDescription(
            "Map", "&5Skytils On Top!\n&b/dmap to open settings menu."
        )

        registerListener<Int>(::mapBackgroundStyle.javaField !!) {
            hidePropertyIf(::mapBorderColor.javaField !!, mapBackgroundStyle.equalsOneOf(1, 2))
            hidePropertyIf(::mapBorderWidth.javaField !!, mapBackgroundStyle.equalsOneOf(1, 2))
            hidePropertyIf(::mapBackground.javaField !!, mapBackgroundStyle.equalsOneOf(1, 2))
        }
    }

    fun save() {
        markDirty()
        writeData()
    }

    private object CategorySorting: SortingBehavior() {
        private val configCategories = listOf(
            "Map", "Rooms", "Colors", "Wither Door ESP"
        )

        private val configSubcategories = listOf(
            "Toggle", "Size", "Render"
        )

        override fun getCategoryComparator(): Comparator<in Category> = compareBy { configCategories.indexOf(it.name) }

        override fun getSubcategoryComparator(): Comparator<in Map.Entry<String, List<PropertyData>>> =
            compareBy { configSubcategories.indexOf(it.key) }
    }
}
