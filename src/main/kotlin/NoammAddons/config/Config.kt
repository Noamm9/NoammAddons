package NoammAddons.config

import NoammAddons.features.Alerts.EnergyCrystal
import NoammAddons.features.Alerts.RNGSound
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.Category
import gg.essential.vigilance.data.Property
import gg.essential.vigilance.data.PropertyType
import gg.essential.vigilance.data.SortingBehavior
import java.awt.Color
import java.io.File
import java.util.function.Consumer


object Config : Vigilant(File("./config/NoammAddons/config.toml"), "NoammAddons", sortingBehavior = Sorting) {

    // General
    @Property(
        type = PropertyType.SWITCH,
        name = "Scaleable Tooltips",
        description = "Allows you to scale the size of the item tooltips and move them around your screen.\n\n" +
                "Scroll Wheel: Scrolls vertically \n" +
                "Left Shift + Scroll Wheel: Scrolls horizontally.\n" +
                "Left Control + Scroll Wheel: Adjusts the scale.\n" +
                "Space Bar: Resets the position and the scale to the default.",
        category = "General",
        subcategory = "Tooltips"
    )
    var ScalableTooltips = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Tooltips Scale",
        description = "What should be the base scaling value of the tooltips",
        category = "General",
        subcategory = "Tooltips"
    )
    var ScalableTooltipsScale = 1f


    // Dungeons
    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Show Extra Stats",
        category = "Dungeons",
        subcategory = "General"

    )
    var showExtraStats = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Close Secrets Chest",
        category = "Dungeons" ,
        subcategory = "Secrets"
    )
    var autoCloseSecretChests = false

    @Property(
        type = PropertyType.SWITCH,
        name = "I HATE DIORITE",
        category = "Dungeons",
        subcategory = "F7"
    )
    var IHATEDIORITE = false

    @Property(
        type = PropertyType.SWITCH,
        name = "F7 Ghost Block",
        description = "Automatically creates ghost blocks to go to P3 from P2 on F7.",
        category = "Dungeons",
        subcategory = "F7"
    )
    var f7p3Ghost = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Livid Solver",
        category = "Dungeons",
        subcategory = "Render"
    )
    var lividFinder = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Fels",
        category = "Dungeons",
        subcategory = "Render"
    )
    var showFels = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Shadow Assassins",
        category = "Dungeons",
        subcategory = "Render"
    )
    var showShadowAssassin = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Stealthy Mobs",
        category = "Dungeons",
        subcategory = "Render"
    )
    var showStealthy = false


    // ESP
    @Property(
        type = PropertyType.SELECTOR,
        name = "ESP Type",
        category = "ESP",
        options = ["Outline", "Box"]
    )
    var espType = 0
/*
    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "OutlineESP Width",
        category = "ESP",
        maxF = 10f
    )
    var espOutlineWidth = 1f*/

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Box Outline Opacity",
        category = "ESP",
    )
    var espBoxOutlineOpacity = 0.95f

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Box Outline Width",
        category = "ESP",
        maxF = 10f
    )
    var espBoxOutlineWidth = 1f

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Box Opacity",
        category = "ESP",
    )
    var espBoxOpacity = 0.3f

    @Property(
        type = PropertyType.SWITCH,
        name = "ESP Bats",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espBats = false

    @Property(
        type = PropertyType.SWITCH,
        name = "ESP Fels",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espFels = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Shadow Assassins",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espShadowAssassin = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Minibosses",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espMiniboss = false


    @Property(
        type = PropertyType.SWITCH,
        name = "Seperate Miniboss Colors",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espSeperateMinibossColor = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Starred Mobs",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espStarMobs = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Remove Starred Nametags",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var removeStarMobsNametag = false

    @Property(
        type = PropertyType.COLOR,
        name = "Livid Color",
        description = "Default #55FFFF.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorLivid = Color(255, 20, 20)

    @Property(
        type = PropertyType.COLOR,
        name = "Bat Color",
        description = "Default #2FEE2F.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorBats = Color(47, 238, 47)

    @Property(
        type = PropertyType.COLOR,
        name = "Fel Color",
        description = "Default #CB59FF.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorFels = Color(203, 89, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Shadow Assassin Color",
        description = "Default #AA00AA.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorShadowAssassin = Color(170, 0, 170)

    @Property(
        type = PropertyType.COLOR,
        name = "Miniboss Color",
        description = "Used for all minibosses except Shadow Assassins if seperate miniboss colors is off. Default #D70000.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorMiniboss = Color(215, 0, 0)

    @Property(
        type = PropertyType.COLOR,
        name = "Unstable Dragon Adventurer Color",
        description = "Default #B212E3.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorUnstable = Color(178, 18, 227)

    @Property(
        type = PropertyType.COLOR,
        name = "Young Dragon Adventurer Color",
        description = "Default #DDE4F0.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorYoung = Color(221, 228, 240)

    @Property(
        type = PropertyType.COLOR,
        name = "Superior Dragon Adventurer Color",
        description = "Default #F2DF11.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorSuperior = Color(242, 223, 17)

    @Property(
        type = PropertyType.COLOR,
        name = "Holy Dragon Adventurer Color",
        description = "Default #47D147.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorHoly = Color(71, 209, 71)

    @Property(
        type = PropertyType.COLOR,
        name = "Frozen Dragon Adventurer Color",
        description = "Default #A0DAEF.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorFrozen = Color(160, 218, 239)

    @Property(
        type = PropertyType.COLOR,
        name = "Angry Archaeologist Color",
        description = "Default #5555FF.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorAngryArchaeologist = Color(85, 85, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Star Mobs Color",
        description = "Default #FFFF00.",
        category = "ESP",
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorStarMobs = Color(255, 255, 0)


    // Alerts
    @Property(
        type = PropertyType.SWITCH,
        name = "Blood Ready Notify",
        description = "Notification when the watcher has finished spawning mobs.",
        category = "Alerts",
        subcategory = "Dungeons"
    )
    var bloodReadyNotify = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Place Energy Crystal Alert",
        description = "Alerts when you have an unplaced energy crystal in your inventory.",
        category = "Alerts",
        subcategory = "Dungeons"
    )
    var energyCrystalAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Full Thunder In A Bottle Alert",
        description = "Alerts when your Thunder In A Bottle finish charging.",
        category = "Alerts",
        subcategory = "Dungeons"
    )
    var FullThunderBottleAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Thunder In A Bottle Alert",
        description = "Alerts when you enter F7 or M7 without an Empty Thunder Bottle.",
        category = "Alerts",
        subcategory = "Dungeons"
    )
    var NoThunderBottleAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "M7 Ragnarock Axe Alert",
        description = "Shows on screen when to use Ragnarock Axe before P5 starts",
        category = "Alerts",
        subcategory = "Dungeons"
    )
    var M7P5RagAxe = false

    @Property(
        type = PropertyType.SWITCH,
        name = "RNG Meter Reset Alert",
        description = "Shows on screen when the RNG Meter Resets\n§b§lAlso Plays Really cool intro music",
        category = "Alerts",
        subcategory = "Dungeons"
    )
    var RNGSound = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Sold AH Notification",
        description = "Plays A sound when an item on your AH sold",
        category = "Alerts",
        subcategory = "General"
    )
    var SoldAHNotification = false

    /*

.addSwitch({
    title: "§9Bonzo Mask§f Alert",
    description: "\n§fShows on screen when the §9Bonzo Mask §6Ability§f has been used",
    category: "Alerts",
    subcategory: "",
    configName: "BonzoMaskAlert",
})
.addSwitch({
    title: "Spirit Mask Alert",
    description: "\n§fShows on screen when the §fSpirit Mask §6Ability§f has been used",
    category: "Alerts",
    subcategory: "",
    configName: "SpiritMaskAlert",
})
.addSwitch({
    title: "§5Phoenix Pet Alert",
    description: "\n§fShows on screen when the §5Phoenix Pet §6Ability§f has been used",
    category: "Alerts",
    subcategory: "",
    configName: "PhoenixPetAlert",
})
     */







    // GUI
    @Property(
        type = PropertyType.SWITCH,
        name = "Highlight Salvageable Items",
        category = "GUI",
        subcategory = "Inventory"
    )
    var overlaySalvageable = false

    @Property(
        type = PropertyType.COLOR,
        name = "Salvageable Items Color",
        description = "Default 55FFFFAA.",
        category = "GUI",
        subcategory = "Inventory"
    )
    var overlayColorSalvageable = Color(85, 255, 255, 170)

    @Property(
        type = PropertyType.COLOR,
        name = "Top Quality Salvageable Items Color",
        description = "Default 6AFF6AAA.",
        category = "GUI",
        subcategory = "Inventory"
    )
    var overlayColorTopSalvageable = Color(106, 255, 106, 170)


    @Property(
        type = PropertyType.SWITCH,
        name = "Block Overlay",
        category = "Cosmetic",
        subcategory = "Block Overlay"
    )
    var BlockOverlay = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Block Overlay Type",
        description = "How to highlight the block",
        category = "Cosmetic",
        subcategory = "Block Overlay",
        options = ["Outline", "Overlay", "Outlined Overlay"],
    )
    var BlockOverlayType = 0

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Outline Thickness",
        description = "",
        category = "Cosmetic",
        subcategory = "Block Overlay",
        minF = 1f,
        maxF = 10f
    )
    var BlockOverlayOutlineThickness = 5f

    @Property(
        type = PropertyType.COLOR,
        name = "Outline Color",
        description = "The color of the Outline",
        category = "Cosmetic",
        subcategory = "Block Overlay"
    )
    var BlockOverlayOutlineColor = Color(0, 114, 255, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Overlay Color",
        description = "The color of the Overlay",
        category = "Cosmetic",
        subcategory = "Block Overlay",
    )
    var BlockOverlayOverlayColor = Color(0, 114, 255, 75)

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Show Through Blocks?",
        description = "Whether to Enable or Disable Depth Checking.",
        category = "Cosmetic",
        subcategory = "Block Overlay"
    )
    var BlockOverlayESP = true


    @Property(
        type = PropertyType.SWITCH,
        name = "Player Scale",
        description = "Allows to dynamically adjust the size of the player character." +
                "\n\n §dNow you can match your IRL Height ❤.",
        category = "Cosmetic",
        subcategory = "Player"
    )
    var PlayerScale = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Player Scale On Everyone?",
        description = "Whether to make this feature work on everyone. or just yourself.",
        category = "Cosmetic",
        subcategory = "Player"
    )
    var PlayerScaleOnEveryone = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom Scale",
        description = "How much to scale the player character's scale from the default 100% down to 10%.",
        category = "Cosmetic",
        subcategory = "Player"
    )
    var PlayerScaleValue = 1f


    @Property(
        type = PropertyType.SWITCH,
        name = "Time Changer",
        description = "Allows to adjust the World's time.",
        category = "Cosmetic",
        subcategory = "Time Changer"
    )
    var TimeChanger = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Time Changer Mode",
        description = "How to adjust the World's time.",
        category = "Cosmetic",
        subcategory = "Time Changer",
        options = [
            "Day",
            "Noon",
            "Sunset",
            "Night",
            "Midnight",
            "Sunrise",
        ]
    )
    var TimeChangerMode = 0


    @Property(
        type = PropertyType.SWITCH,
        name = "No Blindness",
        description = "Disables blindness.",
        category = "Cosmetic",
        subcategory = "Clear Sight"
    )
    var antiBlind = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Portal Effect",
        description = "Disables nether portal overlay.",
        category = "Cosmetic",
        subcategory = "Clear Sight"
    )
    var antiPortal = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Water FOV",
        description = "Disables FOV change in water.",
        category = "Cosmetic",
        subcategory = "Clear Sight"
    )
    var antiWaterFOV = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Block Animation",
        description = "Disable block animation on all swords with right click ability.",
        category = "Cosmetic",
        subcategory = "Clear Sight"
    )
    var noBlockAnimation = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Right Click Ghost Block",
        description = "Right click with any pickaxe to create ghost block.",
        category = "Cosmetic",
        subcategory = "QOL"
    )
    var stonkGhostBlock = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Remove Selfie camera",
        description = "Removes selfie camera In F5.",
        category = "Cosmetic",
        subcategory = "QOL"
    )
    var removeSelfieCamera = false

    @Property(
        type = PropertyType.SWITCH,
        name = "CustomFOV",
        description = "Allows to set a higher or lower FOV than Minecraft default.",
        category = "Cosmetic",
        subcategory = "QOL"
    )
    var CustomFov = false

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "FOV Value",
        description = "What to set your FOV to.",
        category = "Cosmetic",
        subcategory = "QOL",
        minF = 1f,
        maxF = 179f,
        decimalPlaces = 1
    )
    var CustomFovValue = 90f

    @Property(
        type = PropertyType.SWITCH,
        name = "custom Damage Splash",
        description = "Reformats Hypixel SkyBlock's Shitty Damage Splash.",
        category = "Cosmetic",
        subcategory = "QOL"
    )
    var customDamageSplash = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Hide Falling Blocks",
        description = "Hides falling blocks, Good for fps.",
        category = "Cosmetic",
        subcategory = "QOL"
    )
    var hideFallingBlocks = false


    @Property(
        type = PropertyType.SWITCH,
        name = "Force Skyblock",
        description = "Forces all features to enable, even if you are not on skyblock.",
        category = "Dev"
    )
    var forceSkyblock = false


    init {
        setCategoryDescription(
            "ESP",
            "Disable Optifine fast render and Patcher entity culling."
        )

        // General
        addDependency("ScalableTooltipsScale", "ScalableTooltips")


        // Cosmetics
        listOf(
            "BlockOverlayType",
            "BlockOverlayOutlineThickness",
            "BlockOverlayOutlineColor",
            "BlockOverlayOverlayColor",
            "BlockOverlayESP",
        ).forEach { addDependency(it, "BlockOverlay") }


        listOf(
            "PlayerScaleOnEveryone",
            "PlayerScaleValue",
        ).forEach { addDependency(it, "PlayerScale") }


        addDependency("TimeChangerMode", "TimeChanger")
        addDependency("CustomFovValue", "CustomFov")



        // ESP
        listOf(
            "espColorUnstable",
            "espColorYoung",
            "espColorSuperior",
            "espColorHoly",
            "espColorFrozen",
            "espColorAngryArchaeologist"
        ).forEach { addDependency(it, "espSeperateMinibossColor") }


        listOf(
            "espColorStarMobs",
            "removeStarMobsNametag"
        ).forEach{ addDependency(it, "espStarMobs") }


        addDependency("espColorLivid", "lividFinder")
        addDependency("espColorBats", "espBats")
        addDependency("espColorFels", "espFels")
        addDependency("espColorShadowAssassin", "espShadowAssassin")
        addDependency("removeStarMobsNametag", "espStarMobs")



        // GUI
        listOf(
            "overlayColorSalvageable",
            "overlayColorTopSalvageable"
        ).forEach{ addDependency(it, "overlaySalvageable") }

    }

    fun init() {
        initialize()
    }

    private object Sorting : SortingBehavior() {
        override fun getCategoryComparator(): Comparator<in Category> = Comparator.comparingInt { o: Category ->
            configCategories.indexOf(o.name)
        }
    }

    private val configCategories = listOf(
        "General", "Dungeons", "Terminals", "ESP", "Alerts", "GUI", "Cosmetic", "Dev"
    )

}
