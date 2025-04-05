package noammaddons.config

import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.UDesktop
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.*
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.features.dungeons.dmap.core.DungeonMapConfig
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.noammaddons.Companion.MOD_NAME
import noammaddons.noammaddons.Companion.MOD_VERSION
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.GuiUtils.openScreen
import java.awt.Color
import java.io.File
import java.net.URI
import kotlin.reflect.jvm.javaField


object Config: Vigilant(
    File("./config/$MOD_NAME/config.toml"),
    "$FULL_PREFIX&r &6($MOD_VERSION)".addColor(),
    sortingBehavior = Sorting
) {
    private const val EMPTY_CATEGORY = ""
    private const val GENERAL_CATEGORY = "General"
    private const val ZPT_CATEGORY = "ZPT"
    private const val SLAYER_CATEGORY = "Slayers"
    private const val DMAP_CATEGORY = "Dungeon Map"
    private const val DUNGEONS_CATEGORY = "Dungeons"
    private const val TERMINALS_CATEGORY = "Terminals"
    private const val ESP_CATEGORY = "ESP"
    private const val ALERTS_CATEGORY = "Alerts"
    private const val GUI_CATEGORY = "GUI"
    private const val HUD_CATEGORY = "HUD"
    private const val MISC_CATEGORY = "Misc"
    private const val DEV_CATEGORY = "Dev"


    @Property(
        type = PropertyType.BUTTON,
        name = "Join my Discord Server",
        description = "Feel free to join my Discord Server.",
        category = EMPTY_CATEGORY,
        placeholder = "CLICK"
    )
    fun openDiscordLink() {
        UDesktop.browse(URI("https://discord.gg/pj9mQGxMxB"))
    }

    private const val EDIT_HUD_CONFIG_DESCRIPTION =
        "Opens the Hud Edit GUI\n\n" +
                "Left Click + Drag: Move Element around the screen\n" +
                "Left Click + Scroll Wheel: Control the scale"

    @Property(
        type = PropertyType.BUTTON,
        name = "Edit Hud Config",
        description = EDIT_HUD_CONFIG_DESCRIPTION,
        category = EMPTY_CATEGORY,
        placeholder = "CLICK"
    )
    fun openHudEditGUI() {
        openScreen(HudEditorScreen)
    }

    @Property(
        type = PropertyType.SWITCH,
        name = "Item Rarity",
        description = "Draws the rarity of the item in the Inventory",
        category = GENERAL_CATEGORY,
        subcategory = "ItemRarity"
    )
    var DrawItemRarity = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Item Rarity Opacity",
        description = "The opacity of the item rarity",
        category = GENERAL_CATEGORY,
        subcategory = "ItemRarity",
    )
    var ItemRarityOpacity = 0.6f

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Slot Highlight",
        description = "Highlights the currently hovered slot in the Inventory",
        category = GENERAL_CATEGORY,
        subcategory = "SlotHighlight"
    )
    var CustomSlotHighlight = false

    @Property(
        type = PropertyType.COLOR,
        name = "Slot Highlight Color",
        description = "The color of the slot highlight",
        category = GENERAL_CATEGORY,
        subcategory = "SlotHighlight",
        allowAlpha = true
    )
    var CustomSlotHighlightColor = Color.CYAN.withAlpha(100)

    @Property(
        type = PropertyType.SWITCH,
        name = "Slot Binding",
        description = "Allows binding of slots to hotbar slots for quick item swaps, configurable via the SlotBinding Keybind in Options/Controls.\n" +
                "Usage: Hold the Bind key, click the source slot, then the target hotbar slot. To remove bindings, hold the Keybind and click the first slot to clear.",
        category = GENERAL_CATEGORY,
        subcategory = "SlotBinding"

    )
    var SlotBinding = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Show Bound Slots",
        description = "Shows the currently bound slots in the Inventory",
        category = GENERAL_CATEGORY,
        subcategory = "SlotBinding"
    )
    var SlotBindingShowBinding = true

    @Property(
        type = PropertyType.COLOR,
        name = "Line Color",
        description = "The color of the line that is connecting between the slots",
        category = GENERAL_CATEGORY,
        subcategory = "SlotBinding",
        allowAlpha = false
    )
    var SlotBindingLineColor = Color.CYAN !!

    @Property(
        type = PropertyType.COLOR,
        name = "Border Color",
        description = "The color of the border that is being draw on the items",
        category = GENERAL_CATEGORY,
        subcategory = "SlotBinding",
        allowAlpha = false
    )
    var SlotBindingBorderColor = Color.CYAN !!

    @Property(
        type = PropertyType.SWITCH,
        name = "Chat Emojis",
        description = "Replaces chat emojis with their unicode representations.\n\nSame as [MVP++]",
        category = GENERAL_CATEGORY,
        subcategory = "Chat"
    )
    var ChatEmojis = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Cake's Year",
        description = "Shows the new year cake's year on the item in the inventory",
        category = GENERAL_CATEGORY,
        subcategory = "Cake"
    )
    var cakeNumbers = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Simon Says Solver",
        description = "allows left click, blocks wrong clicks (sneak to force click)",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var simonSaysSolver = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Remove Useless Messages",
        description = "Removes messages from chat.",
        category = GENERAL_CATEGORY,
        subcategory = "Chat"
    )
    var RemoveUselessMessages = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Blaze Solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers",
    )
    var BlazeSolver = false

    @Property(
        type = PropertyType.COLOR,
        name = "Blaze Solver First Color",
        description = "Color of the first blaze used by the Blaze Solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var BlazeSolverFirstBlazeColor = Color(0, 114, 255, 85)

    @Property(
        type = PropertyType.COLOR,
        name = "Blaze Solver Second Color",
        description = "Color of the second blaze used by the Blaze Solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var BlazeSolverSecondBlazeColor = Color(255, 255, 0, 85)

    @Property(
        type = PropertyType.COLOR,
        name = "Blaze Solver Third Color",
        description = "Color of the third blaze used by the Blaze Solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var BlazeSolverThirdBlazeColor = Color(255, 0, 0, 85)

    @Property(
        type = PropertyType.COLOR,
        name = "Blaze Solver Line Color",
        description = "Color of the line used by the Blaze Solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var BlazeSolverLineColor = Color(255, 255, 255, 255)

    @Property(
        type = PropertyType.SWITCH,
        name = "Creeper Beam",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers",
    )
    var CreeperBeamSolver = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Creeper Beam Lines",
        description = "Draws lines between each solve",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var CreeperBeamSolverLines = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Creeper Beam Phase",
        description = "Toggles whether the solver's solition is see through walls",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var CreeperBeamSolverPhase = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Boulder Solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers",
    )
    var boulderSolver = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Show all Clickes",
        description = "Shows all clicks in Boulder solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var boulderSolverShowAll = false

    @Property(
        type = PropertyType.COLOR,
        name = "Boulder Box Color",
        description = "Color of the Boxes used by the Boulder Solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var boulderSolverBoxColor = Color(255, 0, 0, 85)

    @Property(
        type = PropertyType.COLOR,
        name = "boulder Click Color",
        description = "Color of The clicks in Boulder solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var boulderSolverClickColor = Color(0, 114, 255, 85)

    @Property(
        type = PropertyType.SWITCH,
        name = "Three Weirdos Solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var ThreeWeirdosSolver = false

    @Property(
        type = PropertyType.COLOR,
        name = "Three Weirdos Solver Color",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var ThreeWeirdosSolverColor = Color(0, 114, 255)

    @Property(
        type = PropertyType.SWITCH,
        name = "Livid Solver",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var lividFinder = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Hide Wrong Livids",
        description = "Hide The Livids that you shouldn't be killing",
        category = DUNGEONS_CATEGORY,
        subcategory = "Solvers"
    )
    var hideWrongLivids = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Left Click Etherwarp",
        description = "Allows you to use Etherwarp with left click.",
        category = GENERAL_CATEGORY,
        subcategory = "Etherwarp"
    )
    var LeftClickEtherwarp = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Etherwarp Sound",
        description = "Plays a sound when using Etherwarp.",
        category = GENERAL_CATEGORY,
        subcategory = "Etherwarp"
    )
    var EtherwarpSound = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Teleport Overlay",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay"
    )
    var teleportOverlay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Etherwarp",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay"
    )
    var teleportOverlayEtherwarp = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Instant Transmission",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay"
    )
    var teleportOverlayInstantTransmission = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Wither Impact",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay"
    )
    var teleportOverlayWitherImpact = false


    @Property(
        type = PropertyType.SELECTOR,
        name = "Teleport Overlay Type",
        description = "How to highlight the block",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay",
        options = ["Outline", "Overlay", "Outlined Overlay"],
    )
    var teleportOverlayType = 0

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Teleport Overlay Outline Thickness",
        description = "",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay",
        minF = 1f,
        maxF = 10f
    )
    var teleportOverlayOutlineThickness = 5f

    @Property(
        type = PropertyType.COLOR,
        name = "Valid Teleport Overlay Outline Color",
        description = "The color of the Outline",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay"
    )
    var teleportOverlayOutlineColor = Color(0, 114, 255, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Valid Teleport Overlay Color",
        description = "The color of the Overlay",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay"
    )
    var teleportOverlayOverlayColor = Color(0, 114, 255, 75)

    @Property(
        type = PropertyType.COLOR,
        name = "Invalid Teleport Overlay Outline Color",
        description = "The color of the Outline",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay"
    )
    var etherwarpOverlayOutlineColorInvalid = Color(255, 0, 0, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Invalid Teleport Overlay Color",
        description = "The color of the Overlay",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay"
    )
    var etherwarpOverlayOverlayColorInvalid = Color(255, 0, 0, 75)

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Phase Teleport Overlay",
        description = "Whether to Enable or Disable Depth Checking.",
        category = GENERAL_CATEGORY,
        subcategory = "Teleport Overlay"
    )
    var TeleportOverlayESP = true

    @Property(
        type = PropertyType.SWITCH,
        name = "ZPT Toggle",
        category = ZPT_CATEGORY,
        subcategory = "!Toggles"
    )
    var zeroPingTeleportation = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Etherwarp",
        category = ZPT_CATEGORY,
        subcategory = "!Toggles"
    )
    var zeroPingEtherwarp = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Instant Transmission",
        category = ZPT_CATEGORY,
        subcategory = "!Toggles"
    )
    var zeroPingInstantTransmission = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Wither Impact",
        category = ZPT_CATEGORY,
        subcategory = "!Toggles"
    )
    var zeroPingWitherImpact = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Etherwarp Keep Motion",
        category = ZPT_CATEGORY,
        subcategory = "KeepMotion"
    )
    var zeroPingEtherwarpKeepMotion = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Instant Transmission Keep Motion",
        category = ZPT_CATEGORY,
        subcategory = "KeepMotion"
    )
    var zeroPingInstantTransmissionKeepMotion = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Wither Impact Keep Motion",
        category = ZPT_CATEGORY,
        subcategory = "KeepMotion"
    )
    var zeroPingWitherImpactKeepMotion = false


    @Property(
        type = PropertyType.SWITCH,
        name = "Extra Slayer Info",
        description = "Sends in chat how many bosses you have to kill for the next level",
        category = SLAYER_CATEGORY,
        subcategory = "Chat"
    )
    var extraSlayerInfo = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Ability Cooldowns",
        description = "Displays a timer for your class abilities like explosive arrow, sheep, etc..\n\nDisplays a timer that is based on server ticks so will work perfectly even if the server is lagging.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Dungeon Abilities"
    )
    var dungeonAbilityCooldowns = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Item Price",
        description = "Displays the ah/bz price of items in your inventory",
        category = GENERAL_CATEGORY,
        subcategory = MISC_CATEGORY
    )
    var showItemPrice = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dragon Spawn Timer / Prio",
        description = "Displays a timer that is based on server ticks so will work perfectly even if the server is lagging. and draws a tracer to the dragon statue\n\n Also acts as a Prio, Automaticly detects your class",
        category = DUNGEONS_CATEGORY,
        subcategory = "M7 Dragons"
    )
    var M7dragonsSpawnTimer = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dragon Kill Box",
        description = "Renders a box that shows the area that the dragon needs to be killed it for it to count as dead",
        category = DUNGEONS_CATEGORY,
        subcategory = "M7 Dragons"
    )
    var M7dragonsKillBox = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Dragon Arrows Hit/Debuff in chat",
        description = "If you are playing a debuff class, it will show how many arrows you have hit the dragon and if u hit your ice spray. and if you are playing arch/bers it will show the ammount of arrows you hit on the dragon",
        category = DUNGEONS_CATEGORY,
        subcategory = "M7 Dragons"
    )
    var M7dragonsShowDebuff = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Bow Hit Sound",
        description = "Plays a custom sound effect when you hit a mob",
        category = GENERAL_CATEGORY,
        subcategory = "Chat"
    )
    var CustomBowHitSound = false

    @Property(
        type = PropertyType.SWITCH,
        name = "M7 Relic Spawn Timer",
        description = "Displays a timer for the M7 Relic in the chat. \nBased on server ticks so will work perfectly even if the server is lagging",
        category = DUNGEONS_CATEGORY,
        subcategory = "M7"
    )
    var M7RelicSpawnTimer = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Wither Shield Display",
        description = "Shows the cooldown of the Wither shield ability on a wither blade with wither impact ability",
        category = HUD_CATEGORY,
        subcategory = "Items"
    )
    var WitherShieldTimer = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Party Names",
        description = "Displays the names of the people in the party.",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var partyNames = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Party Outline",
        description = "Draws a rainbow Outline around the people in the party.",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var partyOutline = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Party Commands",
        description = "If this switch is disabled all features below will be off regardless of their configuration.\n\nCredits to oCookie for the original code. All i did was modify it to fit my needs.\n\nAllows Party members to execute leader commands in chat \n\nExsample: \n!w => will make you warp the party\n!ai => will Toggle the allinvite setting of the party",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var PartyCommands = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!ptme {name} (Transfers the party)",
        description = "Alias: !pt",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcPtme = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!warp (warps)",
        description = "Alias: !w",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcWarp = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!ai (Toggles allinvite)",
        description = "Alias: !allinv, !ai",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcAllinv = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!invite {name} (invites a player)",
        description = "Alias: !inv",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcInv = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!floor { 0 - 7 } (joins Normal Dungeon)",
        description = "Alias: !f",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcFloor = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!master { 1 - 7 } (joins Master Dungeon)",
        description = "Alias: !m",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcMasterFloor = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!downtime {Reason}",
        description = "Alias: !dt",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcDt = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!coords (sends coords)",
        description = "Alias: !cords",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcCoords = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!tps (sends server tps)",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcTPS = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!ping (sends ping)",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcPing = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "!gay {name} (gay check)",
        category = GENERAL_CATEGORY,
        subcategory = "Party"
    )
    var pcGay = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Spring Boots Display",
        description = "Renders Sexy Spring Boots Charge Display",
        category = HUD_CATEGORY,
        subcategory = "Items",
    )
    var SpringBootsDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Grounded Items Names",
        description = "Shows the name of the grounded item.",
        category = GENERAL_CATEGORY,
        subcategory = MISC_CATEGORY
    )
    var ShowItemEntityName = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Chat Coords WayPoint",
        description = "Creates a waypoint whenever a received chat message matches\n\nx: 1, y: 1, z: 1",
        category = GENERAL_CATEGORY,
        subcategory = "Chat"
    )
    var ChatCoordsWayPoint = true

    @Property(
        type = PropertyType.COLOR,
        name = "WayPoint Color",
        description = "The Color of the waypoint",
        category = GENERAL_CATEGORY,
        subcategory = "Chat"
    )
    var ChatCoordsWayPointColor = Color(0, 114, 255, 85)

    @Property(
        type = PropertyType.SWITCH,
        name = "Better Floors",
        description = "Global Switch\nBasically My FunnyMapExtras's Config port\nPlaces and remove a some of Blocks in the boss fight",
        category = DUNGEONS_CATEGORY,
        subcategory = "Better Floors"
    )
    var BetterFloors = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Better 7",
        description = "Enables the 7th Floor config",
        category = DUNGEONS_CATEGORY,
        subcategory = "Better Floors"
    )
    var BetterFloor7 = true

    @Property(
        type = PropertyType.SWITCH,
        name = "SB Kick Duration",
        description = "Shows a timer on screen for when you can rejoin SkyBlock after being kicked",
        category = GENERAL_CATEGORY,
        subcategory = MISC_CATEGORY
    )
    var SBKickDuration = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Better 6",
        description = "Enables the 6th Floor config",
        category = DUNGEONS_CATEGORY,
        subcategory = "Better Floors"
    )
    var BetterFloor6 = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Better 5",
        description = "Enables the 5th Floor config",
        category = DUNGEONS_CATEGORY,
        subcategory = "Better Floors"
    )
    var BetterFloor5 = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Ghost Pick",
        description = "Main toggle of this Category\nChoose the options you want to use below",
        category = DUNGEONS_CATEGORY,
        subcategory = "GhostPick"
    )
    var GhostPick = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Party Finder Messages",
        description = "Reformat some messages to make them more readable",
        category = DUNGEONS_CATEGORY,
        subcategory = "PartyFinder"
    )
    var betterPFMessage = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Legit Ghost Pick",
        description = "Makes the block you mine regularly stay as air blocks",
        category = DUNGEONS_CATEGORY,
        subcategory = "GhostPick"
    )
    var LegitGhostPick = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Ghost Blocks",
        description = "Makes the blocks you look at turn into air with a reach of up to 100 blocks",
        category = DUNGEONS_CATEGORY,
        subcategory = "GhostPick"
    )
    var GhostBlocks = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Efficiency 10",
        description = "Make you instantly mine blocks",
        category = DUNGEONS_CATEGORY,
        subcategory = "GhostPick"
    )
    var MimicEffi10 = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Teammates Names",
        description = "Shows the names your teammates in dungeon\nColored By Class.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Dungeon Teammates"
    )
    var dungeonTeammatesNames = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Teammates ESP",
        description = "ESP your teammates in dungeon.\nColored By Class.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Dungeon Teammates"
    )
    var dungeonTeammatesEsp = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Ender Pearl Fix",
        description = "Disables Hypixel's stupid Ender Pearls throw block when you are too close to a wall/floor/ceiling.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Ender Pearls"
    )
    var enderPearlFix = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Refill Ender Pearls",
        description = "Automatically refills your Ender Pearls at the start of the run.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Ender Pearls"
    )
    var refillEnderPearls = false

    @Property(
        type = PropertyType.TEXT,
        name = "Announce Spirit Leaps",
        description = "Says in party chat who did you leaped to\n You can use {name} to get the leaped player's name\n leave empty to disable.",
        category = DUNGEONS_CATEGORY,
        subcategory = GENERAL_CATEGORY,
        placeholder = "ILY ❤ {name}"
    )
    var AnnounceSpiritLeaps = "ILY ❤ {name}"

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Show Extra Stats",
        description = "Automatically sends /showextrastats after the end of the run.",
        category = DUNGEONS_CATEGORY,
        subcategory = GENERAL_CATEGORY
    )
    var showExtraStats = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Ult",
        description = "Automatically uses your ULTIMATE whenever you need.",
        category = DUNGEONS_CATEGORY,
        subcategory = GENERAL_CATEGORY
    )
    var autoUlt = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Potion",
        description = "Automatically takes a potion before you join a dungeon",
        category = DUNGEONS_CATEGORY,
        subcategory = GENERAL_CATEGORY
    )
    var AutoPotion = false

    @Property(
        type = PropertyType.TEXT,
        name = "Auto Potion Command",
        description = "The Command to use to take a potion, e.g '/pb', '/bp {num}', '/ec {num}'.",
        category = DUNGEONS_CATEGORY,
        subcategory = GENERAL_CATEGORY,
        placeholder = "/pb",
        hidden = true
    )
    var AutoPotionCommand = "/pb"

    @Property(
        type = PropertyType.SWITCH,
        name = "Highlight Door Keys",
        description = "Draws a box and a line to the Wither/Blood key.",
        category = DUNGEONS_CATEGORY,
        subcategory = GENERAL_CATEGORY
    )
    var HighlightDoorKeys = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Relic Outline",
        description = "Highlights the Relic Cauldron. of the Relic you picked",
        category = DUNGEONS_CATEGORY,
        subcategory = "M7"
    )
    var M7RelicOutline = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Blood Dialogue Skip",
        description = "displays a timer for 24 seconds after you open the blood room \n\nTip: You need to be in blood when timer ends",
        category = DUNGEONS_CATEGORY,
        subcategory = GENERAL_CATEGORY
    )
    var BloodDialogueSkip = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Reaper Armor Swap",
        description = "Automatically does the Reaper Armor Swap before the dragons on M7 P5 Spawns\n\n Need to have the Reaper Armor on the first page in your wardrobe \n\n Can also be Triggered with /na ras command ",
        category = DUNGEONS_CATEGORY,
        subcategory = GENERAL_CATEGORY,
        searchTags = ["reaper", "armor", "slot", "reaperarmor", "auto"]
    )
    var AutoReaperArmorSwap = false

    @Property(
        type = PropertyType.SLIDER,
        name = "Auto Reaper Armor Slot",
        description = "The slot where the Reaper Armor is located\n\n from 1 to 9",
        category = DUNGEONS_CATEGORY,
        subcategory = GENERAL_CATEGORY,
        min = 1,
        max = 9,
        searchTags = ["reaper", "armor", "slot", "reaperarmor", "auto"]
    )
    var AutoReaperArmorSlot = 1

    @Property(
        type = PropertyType.SWITCH,
        name = "Announce Drafts Resets",
        description = "Says in party chat when you used Architect's First Draft to reset a failed puzzle.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Puzzles",
    )
    var AnnounceDraftResets = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Architect Draft",
        description = "Automatically runs /gfs architect's first draft 1 when you fail a puzzle in dungeon.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Puzzles",
    )
    var AutoArchitectDraft = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Highlight Mimic Chest",
        description = "Highlights the Mimic Chest",
        category = DUNGEONS_CATEGORY,
        subcategory = "Mimic"
    )
    var highlightMimicChest = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Send Mimic Kill Message",
        description = "Sends in party chat when you kill a Mimic.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Mimic"
    )
    var sendMimicKillMessage = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Block Gloomlock OverUse",
        description = "Blocks right click when you already have above 90% hp and blocks left click if you have less than 30% hp or already at 600 overflow mana",
        category = GENERAL_CATEGORY,
        subcategory = MISC_CATEGORY
    )
    var blockGloomlockOverUse = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Ability Keybinds",
        description = "Allows to use the Your Class ULTIMATE/ABILITY with a keybind witch can be configirate in Minecraft's Options/Controls",
        category = DUNGEONS_CATEGORY,
        subcategory = GENERAL_CATEGORY
    )
    var DungeonAbilityKeybinds = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Close Secrets Chest",
        category = DUNGEONS_CATEGORY,
        subcategory = "Secrets"
    )
    var autoCloseSecretChests = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Secret Sound",
        description = "The sound that plays when clicking a Secret",
        category = DUNGEONS_CATEGORY,
        subcategory = "Secrets",
    )
    var secretSound = false

    @Property(
        type = PropertyType.TEXT,
        name = "Secret Sound Name",
        description = "The name of the sound that plays when clicking a Secret.\nExamples: random.orb, mob.cat.meow",
        category = DUNGEONS_CATEGORY,
        subcategory = "Secrets",
        placeholder = "random.orb"
    )
    var secretSoundName = "random.orb"

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Secret Sound Volume",
        description = "The volume of the sound that plays when clicking a Secret",
        category = DUNGEONS_CATEGORY,
        subcategory = "Secrets",
        minF = 0f,
        maxF = 1f
    )
    var secretSoundVolume = 0.5f

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Secret Sound Pitch",
        description = "The pitch of the sound that plays when clicking a Secret",
        category = DUNGEONS_CATEGORY,
        subcategory = "Secrets",
        minF = 0f,
        maxF = 2f
    )
    var secretSoundPitch = 1f

    @Property(
        type = PropertyType.BUTTON,
        name = "Play Secret Sound",
        description = "Plays the sound that plays when clicking a Secret",
        category = DUNGEONS_CATEGORY,
        subcategory = "Secrets",
        placeholder = "CLICK"
    )
    fun playSecretSound() = repeat(5) {
        mc.thePlayer?.playSound(secretSoundName, secretSoundVolume, secretSoundPitch)
    }

    @Property(
        type = PropertyType.SWITCH,
        name = "Clicked Secrets",
        description = "Highlights the Secret you gets",
        category = DUNGEONS_CATEGORY,
        subcategory = "Secrets"
    )
    var clickedSecrets = false

    @Property(
        type = PropertyType.COLOR,
        name = "Clicked Secrets Color",
        description = "Color of the Secrets used by the Clicked Secrets",
        category = DUNGEONS_CATEGORY,
        subcategory = "Secrets"
    )
    var secretClickedColor = Color(0, 114, 255, 85)

    @Property(
        type = PropertyType.SWITCH,
        name = "I HATE DIORITE",
        description = "Replaces the diorite blocks in storm pillars with colored stained glass.",
        category = DUNGEONS_CATEGORY,
        subcategory = "F7"
    )
    var IHATEDIORITE = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto I4",
        description = "Fully Automated I4. \naims and shoots the emerald block at the forth dev in P3, Predict the next one for faster time. \nMacro: Rod swap, Mask swap, safe leap \n\n [ Need a term and 100 atk speed ] ",
        category = DUNGEONS_CATEGORY,
        subcategory = "Auto I4"
    )
    var autoI4 = false

    @Property(
        type = PropertyType.NUMBER,
        name = "Auto I4 Rotation time",
        description = "How fast should the head movements be for the shoting action\n\nNote: Will effect the consistency of the Auto I4 for better or worse",
        category = DUNGEONS_CATEGORY,
        subcategory = "Auto I4",
        increment = 10,
        min = 0,
        max = 250
    )
    var AutoI4RotatinTime = 200

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Auto I4 Prediction",
        description = "Attempts to predict the next emerald block and shot it.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Auto I4"
    )
    var autoI4Prediction = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Auto I4 Auto Rod",
        category = DUNGEONS_CATEGORY,
        subcategory = "Auto I4"
    )
    var autoI4AutoRod = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Auto I4 Auto Mask",
        category = DUNGEONS_CATEGORY,
        subcategory = "Auto I4"
    )
    var autoI4AutoMask = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Auto I4 Auto Leap",
        description = "Automatically leaps after the device is done.\nThe leap order goes like this:\n(Mage, Tank, Healer, Archer) correct me if wrong",
        category = DUNGEONS_CATEGORY,
        subcategory = "Auto I4"
    )
    var autoI4AutoLeap = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Better F7 Titles",
        description = "Replaces the big and annoying f7 titles with smaller and cleaner ones and display them on screen\n\nExsamples:\n\n 1/2 Energy Crystals are now active! ==> (1/2) \n Noamm9 activated a Terminal! (6/7) ==> (6/7)",
        category = DUNGEONS_CATEGORY,
        subcategory = "F7"
    )
    var BetterF7Titles = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Hide Players After Leap",
        description = "Hides players after you leap to them for 2 seconds allowing you to see clearly",
        category = DUNGEONS_CATEGORY,
        subcategory = "Spirit leap"
    )
    var hidePlayersAfterLeap = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Gyro Radius",
        description = "Shows the Gyrokinetic wand sucking radius",
        category = DUNGEONS_CATEGORY,
        subcategory = "Gyrokinetic Wand"
    )
    var ShowGyroCircle = false

    @Property(
        type = PropertyType.COLOR,
        name = "Gyro Radius Ring Color",
        description = "the Color of the Gyrokinetic wand sucking radius",
        category = DUNGEONS_CATEGORY,
        subcategory = "Gyrokinetic Wand"
    )
    var ShowGyroCircleRingColor = Color.GREEN !!

    @Property(
        type = PropertyType.COLOR,
        name = "Gyro Radius Block Color",
        description = "the Color of the Gyrokinetic wand sucking radius",
        category = DUNGEONS_CATEGORY,
        subcategory = "Gyrokinetic Wand"
    )
    var ShowGyroCircleBlockColor = Color.GREEN.withAlpha(85)

    @Property(
        type = PropertyType.SWITCH,
        name = "Stop Close My Chat",
        description = "Prevents your chat from being closed by the server or world swapping",
        category = GUI_CATEGORY,
        subcategory = "chat"
    )
    var StopCloseMyChat = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Stonk Swap Sound",
        description = "The sound that plays when you successfully stonk swap",
        category = DUNGEONS_CATEGORY,
        subcategory = "Stonk Swap",
    )
    var stonkSwapSound = false

    @Property(
        type = PropertyType.TEXT,
        name = "Stonk Swap Sound Name",
        description = "The name of the sound that plays when Stonk Swaping.\nExamples: random.orb, mob.cat.meow",
        category = DUNGEONS_CATEGORY,
        subcategory = "Stonk Swap",
        placeholder = "random.orb"
    )
    var stonkSwapSoundName = "random.orb"

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Stonk Swap Sound Volume",
        description = "The volume of the sound that plays when Stonk Swaping",
        category = DUNGEONS_CATEGORY,
        subcategory = "Stonk Swap",
        minF = 0f,
        maxF = 1f
    )
    var stonkSwapVolume = 0.5f

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Stonk Swap Sound Pitch",
        description = "The pitch of the sound that plays when Stonk Swaping",
        category = DUNGEONS_CATEGORY,
        subcategory = "Stonk Swap",
        minF = 0f,
        maxF = 2f
    )
    var stonkSwapPitch = 1f

    @Property(
        type = PropertyType.BUTTON,
        name = "Play Stonk Swap Sound",
        description = "Plays the sound that plays when clicking a Secret",
        category = DUNGEONS_CATEGORY,
        subcategory = "Stonk Swap",
        placeholder = "CLICK"
    )
    fun playStonkSwapSound() = repeat(5) {
        mc.thePlayer?.playSound(stonkSwapSoundName, stonkSwapVolume, stonkSwapPitch)
    }

    @Property(
        type = PropertyType.CHECKBOX,
        name = "F7/M7 Phase Start Timers",
        description = "Global Toggle, Based on Server Ticks",
        category = DUNGEONS_CATEGORY,
        subcategory = "Timers"
    )
    var F7M7PhaseStartTimers = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "P1 Start Timer",
        description = "Shows a Timer on screen when Maxor Phase will start",
        category = DUNGEONS_CATEGORY,
        subcategory = "Timers"
    )
    var P1StartTimer = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "P2 Start Timer",
        description = "Shows a Timer on screen when Storm Phase will start",
        category = DUNGEONS_CATEGORY,
        subcategory = "Timers"
    )
    var P2StartTimer = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "P3 Start Timer",
        description = "Shows a Timer on screen when Goldor Phase will start",
        category = DUNGEONS_CATEGORY,
        subcategory = "Timers"
    )
    var P3StartTimer = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "P4 Start Timer",
        description = "Shows a Timer on screen when Necron Phase will start",
        category = DUNGEONS_CATEGORY,
        subcategory = "Timers"
    )
    var P4StartTimer = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Hide Starred Mobs Nametags",
        description = "Removes the nametags of Starred Mobs\nPlease use this with a star mob esp\n\nDevNote: This is a very good for fps",
        category = DUNGEONS_CATEGORY,
        subcategory = "Render"
    )
    var removeStarMobsNametag = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Remove Non-Starred Mob Nametags",
        description = "Removes the nametags of Non-Starred Mobs\n\nDevNote: Has a very good impact on fps",
        category = DUNGEONS_CATEGORY,
        subcategory = "Render"
    )
    var removeNonStarMobsNametag = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Fels",
        category = DUNGEONS_CATEGORY,
        subcategory = "Render"
    )
    var showFels = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Shadow Assassins",
        category = DUNGEONS_CATEGORY,
        subcategory = "Render"
    )
    var showShadowAssassin = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Stealthy Mobs",
        category = DUNGEONS_CATEGORY,
        subcategory = "Render"
    )
    var showStealthy = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Terminal Guis",
        description = "Global Switch for Custom Terminal Guis",
        category = TERMINALS_CATEGORY,
        subcategory = "Custom Terminal Guis",
    )
    var CustomTerminalsGui = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Custom Terminals Gui Click Mode",
        description = "The method used to click on the Custom Terminals Menu\n\n NORMAL: as usual just click on the menu\n HOVER: hover over the solutions and it will click them\n AUTO: Automatically Clicks on the Solutions\n\nNote: Auto and Hover are UAYOR!",
        category = TERMINALS_CATEGORY,
        subcategory = "Custom Terminal Guis",
        options = ["NORMAL", "HOVER", "AUTO"],
        hidden = true
    )
    var CustomTerminalMenuClickMode = 0

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom Terminals Gui Scale",
        description = "Scale of the Custom Terminals Menu",
        category = TERMINALS_CATEGORY,
        subcategory = "Custom Terminal Guis",
    )
    var CustomTerminalMenuScale = 1f

    @Property(
        type = PropertyType.COLOR,
        name = "Solution Color",
        description = "The Color' of the Solution",
        category = TERMINALS_CATEGORY,
        subcategory = "Custom Terminal Guis",
    )
    var CustomTerminalMenuSolutionColor = Color(0, 114, 255)

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Light Mode Gui?",
        description = "Changes the Color Mode of the Custom Terminals Gui",
        category = TERMINALS_CATEGORY,
        subcategory = "Custom Terminal Guis",
    )
    var CustomTerminalMenuLightMode = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Arrow Align Solver",
        description = "Displays the required clicks to complete\n the Arrow Align device in P3 S3.\nAutomatically blocks incorrect inputs when sneaking.\n\nDevNote: OMG, finally a solver that isn’t garbage!",
        category = TERMINALS_CATEGORY,
        subcategory = "Extras"
    )
    var arrowAlignSolver = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Terminal Numbers",
        description = "Places a number on each terminal so you know what number it is.",
        category = TERMINALS_CATEGORY,
        subcategory = "Extras"
    )
    var TerminalNumbers = false

    @Property(
        type = PropertyType.TEXT,
        name = "Melody Alert",
        description = "Sends a Message in chat when you open Melody Terminal\nDelete all text to disable \n\nleave empty to disable",
        category = TERMINALS_CATEGORY,
        subcategory = "Extras",
        placeholder = ""
    )
    var MelodyAlert = "I ❤ Melody"

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Melody Terminal",
        description = "",
        category = TERMINALS_CATEGORY,
        subcategory = TERMINALS_CATEGORY
    )
    var CustomMelodyTerminal = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Numbers Terminal",
        description = "",
        category = TERMINALS_CATEGORY,
        subcategory = TERMINALS_CATEGORY
    )
    var CustomNumbersTerminal = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Rubix Terminal",
        description = "[TIP] No need to swap between Rightclick and Leftclick",
        category = TERMINALS_CATEGORY,
        subcategory = TERMINALS_CATEGORY
    )
    var CustomRubixTerminal = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Red Green Terminal",
        description = "",
        category = TERMINALS_CATEGORY,
        subcategory = TERMINALS_CATEGORY
    )
    var CustomRedGreenTerminal = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Start With Terminal",
        description = "",
        category = TERMINALS_CATEGORY,
        subcategory = TERMINALS_CATEGORY
    )
    var CustomStartWithTerminal = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Color Terminal",
        description = "",
        category = TERMINALS_CATEGORY,
        subcategory = TERMINALS_CATEGORY
    )
    var CustomColorsTerminal = true

    @Property(
        type = PropertyType.SELECTOR,
        name = "ESP Type",
        category = ESP_CATEGORY,
        options = ["Outline", "3D Box", "Filled Outline", "2D BOX", "Chum"]
    )
    var espType = 0

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Outline Opacity",
        category = ESP_CATEGORY,
    )
    var espOutlineOpacity = 1f

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Filled Opacity",
        category = ESP_CATEGORY,
    )
    var espFilledOpacity = 0.3f

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "OutlineESP Width",
        category = ESP_CATEGORY,
        minF = 1f,
        maxF = 10f,
    )
    var espOutlineWidth = 2f

    @Property(
        type = PropertyType.SWITCH,
        name = "Pest Esp",
        description = "ESP for the Pest in the garden",
        category = ESP_CATEGORY,
        subcategory = "Garden"
    )
    var pestEsp = false

    @Property(
        type = PropertyType.COLOR,
        name = "Pest Esp Color",
        description = "ESP for the Pest in the garden",
        category = ESP_CATEGORY,
        subcategory = "Garden ESP Colors",
        allowAlpha = false
    )
    var pestEspColor = Color.CYAN

    @Property(
        type = PropertyType.SWITCH,
        name = "ESP Thorn",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP"
    )
    var espThorn = false

    @Property(
        type = PropertyType.SWITCH,
        name = "ESP Spirit Bear",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP"
    )
    var espSpiritBear = false

    @Property(
        type = PropertyType.SWITCH,
        name = "ESP Bats",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP"
    )
    var espBats = true

    @Property(
        type = PropertyType.SWITCH,
        name = "ESP Withers",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP"
    )
    var espWithers = true

    @Property(
        type = PropertyType.SWITCH,
        name = "ESP Fels",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP"
    )
    var espFels = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Shadow Assassins",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP"
    )
    var espShadowAssassin = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Minibosses",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP"
    )
    var espMiniboss = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Seperate Miniboss Colors",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP"
    )
    var espSeperateMinibossColor = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Starred Mobs",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP"
    )
    var espStarMobs = true

    @Property(
        type = PropertyType.COLOR,
        name = "ESP Thorn Color",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors"
    )
    var espThornColor = Color(255, 0, 0)

    @Property(
        type = PropertyType.COLOR,
        name = "ESP Spirit Bear Color",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors"
    )
    var espSpiritBearColor = Color(255, 0, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Bat Color",
        description = "Default #2FEE2F.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorBats = Color(47, 238, 47)

    @Property(
        type = PropertyType.COLOR,
        name = "Fel Color",
        description = "Default #CB59FF.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorFels = Color(203, 89, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Shadow Assassin Color",
        description = "Default #AA00AA.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorShadowAssassin = Color(170, 0, 170)

    @Property(
        type = PropertyType.COLOR,
        name = "Miniboss Color",
        description = "Used for all minibosses except Shadow Assassins if seperate miniboss colors is off. Default #D70000.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorMiniboss = Color(215, 0, 0)

    @Property(
        type = PropertyType.COLOR,
        name = "Unstable Dragon Adventurer Color",
        description = "Default #B212E3.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorUnstable = Color(178, 18, 227)

    @Property(
        type = PropertyType.COLOR,
        name = "Young Dragon Adventurer Color",
        description = "Default #DDE4F0.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorYoung = Color(221, 228, 240)

    @Property(
        type = PropertyType.COLOR,
        name = "Superior Dragon Adventurer Color",
        description = "Default #F2DF11.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorSuperior = Color(242, 223, 17)

    @Property(
        type = PropertyType.COLOR,
        name = "Holy Dragon Adventurer Color",
        description = "Default #47D147.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorHoly = Color(71, 209, 71)

    @Property(
        type = PropertyType.COLOR,
        name = "Frozen Dragon Adventurer Color",
        description = "Default #A0DAEF.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorFrozen = Color(160, 218, 239)

    @Property(
        type = PropertyType.COLOR,
        name = "Angry Archaeologist Color",
        description = "Default #5555FF.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorAngryArchaeologist = Color(85, 85, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Star Mobs Color",
        description = "Default #FFFF00.",
        category = ESP_CATEGORY,
        subcategory = "Dungeon ESP Colors",
        allowAlpha = false
    )
    var espColorStarMobs = Color(255, 255, 0)

    @Property(
        type = PropertyType.SWITCH,
        name = "Blood Ready Notify",
        description = "Notification when the watcher has finished spawning mobs.",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var bloodReadyNotify = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Warp Cooldown",
        description = "Notification when the watcher has finished spawning mobs.",
        category = DUNGEONS_CATEGORY,
        subcategory = "General"
    )
    var dungeonWarpCooldown = false

    @Property(
        type = PropertyType.SWITCH,
        name = "SB Kick Alert",
        description = "Sends a party message when you are kicked from SkyBlock",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var SBKick = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Shadow Assasian Alert",
        description = "Shows a notification on screen when an invinsable Shadow Assasian is about to teleport",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var ShadowAssassinAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Place Energy Crystal Alert",
        description = "Alerts when you have an unplaced energy crystal in your inventory.",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var energyCrystalAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Full Thunder In A Bottle Alert",
        description = "Alerts when your Thunder In A Bottle finish charging.",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var FullThunderBottleAlert = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Bat Dead Title",
        description = "Shows a notification on screen when the bat secret died",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var batDeadTitle = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Party Finder Alert",
        description = "Plays a sound when someone joins your party",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var PartyFinderSound = true

    @Property(
        type = PropertyType.SWITCH,
        name = "M7 Ragnarock Axe Alert",
        description = "Shows on screen when to use Ragnarock Axe before P5 starts",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var M7P5RagAxe = false

    @Property(
        type = PropertyType.SWITCH,
        name = "RNG Meter Reset Alert",
        description = "Shows on screen when the RNG Meter Resets\nAlso Plays Really cool intro music",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var RNGSound = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Secrets Done Alert",
        description = "Alerts you when Secrets are done in your room",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var roomSecretsDoneAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Room cleared Alert",
        description = "Alerts you when your room is cleared",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var roomClearedAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Sold AH Notification",
        description = "Plays A sound when an item on your AH sold",
        category = ALERTS_CATEGORY,
        subcategory = GENERAL_CATEGORY
    )
    var SoldAHNotification = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Bonzo Mask Alert",
        description = "Shows on screen when the Bonzo Mask Ability has been used",
        category = ALERTS_CATEGORY,
        subcategory = "Masks"
    )
    var BonzoMaskAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Spirit Mask Alert",
        description = "Shows on screen when the Spirit Mask Ability has been used",
        category = ALERTS_CATEGORY,
        subcategory = "Masks"
    )
    var SpiritMaskAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Phoenix Pet Alert",
        description = "Shows on screen when the Phoenix Pet Ability has been used",
        category = ALERTS_CATEGORY,
        subcategory = "Masks"
    )
    var PhoenixPetAlert = false

    private const val TOOLTIP_DESCRIPTION =
        "Allows you to scale the size of the item tooltips and move them around your screen.\n" +
                "Scroll Wheel: Scrolls vertically\n" +
                "Left Shift + Scroll Wheel: Scrolls horizontally\n" +
                "Left Control + Scroll Wheel: Adjusts the scale\n" +
                "Space Bar: Resets the position and the scale to the default"

    @Property(
        type = PropertyType.SWITCH,
        name = "Scalable Tooltips",
        description = TOOLTIP_DESCRIPTION,
        category = GUI_CATEGORY,
        subcategory = "Tooltips"
    )
    var ScalableTooltips = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Tooltips Scale",
        description = "What should be the base scaling value of the tooltips",
        category = GUI_CATEGORY,
        subcategory = "Tooltips"
    )
    var ScalableTooltipsScale = 1f

    @Property(
        type = PropertyType.SWITCH,
        name = "Wardrobe Keybinds",
        description = "Allows you to use your hotbar binds to swap armors in your wardrobe",
        category = GUI_CATEGORY,
        subcategory = "Wardrobe Keybinds"
    )
    var wardrobeKeybinds = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Wardrobe Keybinds Close After Use",
        description = "Closes the wardrobe equiping an armor with keybind",
        category = GUI_CATEGORY,
        subcategory = "Wardrobe Keybinds"
    )
    var wardrobeKeybindsCloseAfterUse = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Highlight Salvageable Items",
        category = GUI_CATEGORY,
        subcategory = "Inventory"
    )
    var overlaySalvageable = false

    @Property(
        type = PropertyType.COLOR,
        name = "Salvageable Items Color",
        description = "Default #55FFFFAA.",
        category = GUI_CATEGORY,
        subcategory = "Inventory"
    )
    var overlayColorSalvageable = Color(92, 157, 255, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Top Quality Salvageable Items Color",
        description = "Default 6AFF6AAA.",
        category = GUI_CATEGORY,
        subcategory = "Inventory"
    )
    var overlayColorTopSalvageable = Color(255, 0, 0, 255)

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Leap Menu",
        description = "Renders a Custom Menu for leaps",
        category = GUI_CATEGORY,
        subcategory = "Custom Leap Menu"
    )
    var CustomLeapMenu = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom Leap Menu Scale",
        description = "Scale of the Custom Leap Menu",
        category = GUI_CATEGORY,
        subcategory = "Custom Leap Menu",
    )
    var CustomLeapMenuScale = 1f

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Show Door Openner",
        description = "highlights the last player who open a wither door in the leap menu",
        category = GUI_CATEGORY,
        subcategory = "Custom Leap Menu",
    )
    var showLastDoorOpenner = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Tint Dead Players",
        description = "Tints in red the players that are dead in the leap menu",
        category = GUI_CATEGORY,
        subcategory = "Custom Leap Menu",
    )
    var tintDeadPlayers = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Custom Menus",
        description = "Tints in red the players that are disconnected in the leap menu",
        category = GUI_CATEGORY,
        subcategory = "Menus",
    )
    var customMenus = true

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom Menus Scale",
        description = "Scale of the Custom Menus",
        category = GUI_CATEGORY,
        subcategory = "Menus",
    )
    var customMenusScale = 0.85f

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Party Finder Menus",
        category = GUI_CATEGORY,
        subcategory = "Menus",
    )
    var CustomPartyFinderMenu = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Pet Menu",
        category = GUI_CATEGORY,
        subcategory = "Menus",
    )
    var CustomPetMenu = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Custom Wardrobe Menu",
        category = GUI_CATEGORY,
        subcategory = "Menus",
    )
    var CustomWardrobeMenu = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Player HUD",
        description = "Global Switch for Player HUD\nDraws the hp, currentDefense, currentMana, and effective hp of the player on screen",
        category = HUD_CATEGORY,
        subcategory = "PlayerHUD"
    )
    var PlayerHUD = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Health Display",
        description = "Draws your players health in currentHealth/maxHealth format on screen",
        category = HUD_CATEGORY,
        subcategory = "PlayerHUD",
    )
    var PlayerHUDHealth = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Defense Display",
        description = "Draws your players currentDefense on screen",
        category = HUD_CATEGORY,
        subcategory = "PlayerHUD",
    )
    var PlayerHUDDefense = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Mana Display",
        description = "Draws your players currentMana in currentMana/maxMana format on screen",
        category = HUD_CATEGORY,
        subcategory = "PlayerHUD",
    )
    var PlayerHUDMana = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Overflow Mana Display",
        description = "Draws your players Overflow Mana on screen",
        category = HUD_CATEGORY,
        subcategory = "PlayerHUD",
    )
    var PlayerHUDOverflowMana = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Alternate Overflow Mana",
        description = "Draws your player Overflow Mana only if it is greater than 0",
        category = HUD_CATEGORY,
        subcategory = "PlayerHUD",
    )
    var PlayerHUDAlternateOverflowMana = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Effective HP Display",
        description = "Draws your players effective health on screen",
        category = HUD_CATEGORY,
        subcategory = "PlayerHUD",
    )
    var PlayerHUDEffectiveHP = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Speed Display",
        description = "Draws your players speed on screen",
        category = HUD_CATEGORY,
        subcategory = "PlayerHUD",
    )
    var PlayerHUDSpeed = true

    @Property(
        type = PropertyType.SWITCH,
        name = "FPS Display",
        description = "Displays the FPS on screen",
        category = HUD_CATEGORY,
        subcategory = "FPS"
    )
    var FpsDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Pet Display",
        description = "Displays your current active pet's name on screen",
        category = HUD_CATEGORY,
        subcategory = "PlayerHUD"
    )
    var PetDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Secrets Display",
        description = "Displays the current secrets in the dungeon room you are currently in",
        category = HUD_CATEGORY,
        subcategory = "Secret Display"

    )
    var secretDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "TPS Display",
        description = "Displays the TPS on screen",
        category = HUD_CATEGORY,
        subcategory = "Tps"
    )
    var TpsDisplay = false

    @Property(
        type = PropertyType.COLOR,
        name = "Tps Display Color",
        description = "The Color of the TPS Display",
        category = HUD_CATEGORY,
        subcategory = "Tps",
    )
    var TpsDisplayColor = Color(0, 114, 255)

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Scoreboard",
        description = "Renders Sexy Custom Dark Scoreboard",
        category = HUD_CATEGORY,
        subcategory = "ScoreBoard"
    )
    var CustomScoreboard = false

    @Property(
        type = PropertyType.COLOR,
        name = "FPS Display Color",
        description = "The Color of the FPS Display",
        category = HUD_CATEGORY,
        subcategory = "FPS",
        allowAlpha = false
    )
    var FpsDisplayColor = Color(255, 0, 255)

    @Property(
        type = PropertyType.SWITCH,
        name = "Clock Display",
        description = "Displays the System Time on screen",
        category = HUD_CATEGORY,
        subcategory = "Clock"
    )
    var ClockDisplay = false

    @Property(
        type = PropertyType.COLOR,
        name = "Clock Display Color",
        description = "The Color of the Clock Display",
        category = HUD_CATEGORY,
        subcategory = "Clock",
        allowAlpha = false
    )
    var ClockDisplayColor = Color(255, 116, 0)

    @Property(
        type = PropertyType.SWITCH,
        name = "Bonzo Mask Display",
        description = "Displays the Bonzo Mask Cooldown on screen",
        category = HUD_CATEGORY,
        subcategory = "Masks"
    )
    var BonzoMaskDisplay = false

    /* todo: Invornability Timers for Bonzo, Spirit and Phoenix


        @Property(
            type = PropertyType.SWITCH,
            name = "Bonzo Mask Invulnerability Display",
            description = "Displays the Bonzo Mask Invulnerability time on screen",
            category = HUD_CATEGORY,
            subcategory = "Bonzo Mask"
        )
        var BonzoMaskInvulnerabilityDisplay = false*/

    @Property(
        type = PropertyType.SWITCH,
        name = "Spirit Mask Display",
        description = "Displays the Spirit Mask Cooldown on screen",
        category = HUD_CATEGORY,
        subcategory = "Masks"
    )
    var SpiritMaskDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Phoenix Pet Display",
        description = "Displays the Phoenix Pet Cooldown on screen",
        category = HUD_CATEGORY,
        subcategory = "Masks"
    )
    var PhoenixPetDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Tab List",
        description = "Custom Tab List design",
        category = HUD_CATEGORY,
        subcategory = "TabList"
    )
    var CustomTabList = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom Tab List Scale",
        description = "The scale of the Custom Tab List",
        category = HUD_CATEGORY,
        subcategory = "TabList"
    )
    var CustomTabListScale = 1f

    @Property(
        type = PropertyType.SWITCH,
        name = "Block Overlay",
        category = MISC_CATEGORY,
        subcategory = "Block Overlay"
    )
    var BlockOverlay = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Block Overlay Type",
        description = "How to highlight the block",
        category = MISC_CATEGORY,
        subcategory = "Block Overlay",
        options = ["Outline", "Overlay", "Outlined Overlay"],
    )
    var BlockOverlayType = 0

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Outline Thickness",
        description = "",
        category = MISC_CATEGORY,
        subcategory = "Block Overlay",
        minF = 1f,
        maxF = 10f
    )
    var BlockOverlayOutlineThickness = 5f

    @Property(
        type = PropertyType.COLOR,
        name = "Outline Color",
        description = "The color of the Outline",
        category = MISC_CATEGORY,
        subcategory = "Block Overlay"
    )
    var BlockOverlayOutlineColor = Color(0, 114, 255, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Overlay Color",
        description = "The color of the Overlay",
        category = MISC_CATEGORY,
        subcategory = "Block Overlay",
    )
    var BlockOverlayOverlayColor = Color(0, 114, 255, 75)

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Show Through Blocks?",
        description = "Whether to Enable or Disable Depth Checking.",
        category = MISC_CATEGORY,
        subcategory = "Block Overlay"
    )
    var BlockOverlayESP = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Player Scale",
        description = "Allows to dynamically adjust the size of the player character.\n\n" +
                "Now you can match your IRL Height ❤.",
        category = MISC_CATEGORY,
        subcategory = "Player"
    )
    var PlayerScale = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Player Scale On Everyone?",
        description = "Whether to make this feature work on everyone. or just yourself.",
        category = MISC_CATEGORY,
        subcategory = "Player"
    )
    var PlayerScaleOnEveryone = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom Scale",
        description = "How much to scale the player character's scale from the default 100% down to 10%.",
        category = MISC_CATEGORY,
        subcategory = "Player"
    )
    var PlayerScaleValue = 1f

    @Property(
        type = PropertyType.SWITCH,
        name = "Player Spin",
        description = "A client-side feature that makes the player's avatar spin in place, visible only to the user and without affecting gameplay or other players.",
        category = MISC_CATEGORY,
        subcategory = "Player"
    )
    var PlayerSpin = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Spin direction",
        description = "",
        category = MISC_CATEGORY,
        subcategory = "Player",
        options = ["Right", "Left"]
    )
    var SpinDirection = 0

    @Property(
        type = PropertyType.SLIDER,
        name = "Spin speed",
        description = "",
        category = MISC_CATEGORY,
        subcategory = "Player",
        min = 30,
        max = 200
    )
    var SpinSpeed = 50

    /*
    @Property(
        type = PropertyType.CHECKBOX,
        name = "Should I spin everyone?",
        description = "",
        category = MISC_CATEGORY,
        subcategory = "Player"
    )
    var SpinOnEveryone = false;*/

    @Property(
        type = PropertyType.SWITCH,
        name = "Time Changer",
        description = "Allows to adjust the World's time.",
        category = MISC_CATEGORY,
        subcategory = "Time Changer"
    )
    var TimeChanger = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Time Changer Mode",
        description = "How to adjust the World's time.",
        category = MISC_CATEGORY,
        subcategory = "Time Changer",
        options = ["Day", "Noon", "Sunset", "Night", "Midnight", "Sunrise"]
    )
    var TimeChangerMode = 0

    @Property(
        type = PropertyType.SWITCH,
        name = "No Blindness",
        description = "Disables blindness.",
        category = MISC_CATEGORY,
        subcategory = "Clear Sight"
    )
    var antiBlind = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Smooth Sneak",
        description = "Backport for newer minecraft version's sneak animation.\nMay messup some mods.",
        category = MISC_CATEGORY,
        subcategory = "Clear Sight"
    )
    var smoothSneak = true

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Smooth Sneak Offset",
        description = "Controls how much your camera will move down while sneaking",
        category = MISC_CATEGORY,
        subcategory = "Clear Sight",
        minF = 0f,
        maxF = 1f,
        hidden = true
    )
    var smoothSneakOffset = 0.08f

    @Property(
        type = PropertyType.SWITCH,
        name = "Bonzo Boss Revived Alert",
        description = "Plays A sound when the Bonzo Boss is revived",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var bonzoBossRespawnAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Crypts Done Alert",
        description = "Shows on Screen when you have 5 Crypts",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var cryptsDoneAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Clear Blocks",
        description = "When clipping into blocks it allows you to see around you instead of just blocking your view",
        category = MISC_CATEGORY,
        subcategory = "Clear Sight"
    )
    var clearBlocks = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dont push out of blocks",
        description = "Disables blocks pushing you out of them when clipping into them.",
        category = MISC_CATEGORY,
        subcategory = "Clear Sight"
    )
    var noPushOutOfBlocks = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Portal Effect",
        description = "Disables nether portal overlay.",
        category = MISC_CATEGORY,
        subcategory = "Clear Sight"
    )
    var antiPortal = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Rotate",
        description = "Disables rotations from server.\n\nUSE AT YOUR OWN RISK, COULD GET YOU TIMER BANNED",
        category = MISC_CATEGORY,
        subcategory = "Clear Sight"
    )
    var NoRotate = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "No Rotate Keep Motion",
        description = "Keeps your momentum when you are teleporting.",
        category = MISC_CATEGORY,
        subcategory = "Clear Sight"
    )
    var NoRotateKeepMotion = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Water FOV",
        description = "Disables FOV change in water.",
        category = MISC_CATEGORY,
        subcategory = "Clear Sight"
    )
    var antiWaterFOV = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Block Animation",
        description = "Disable block animation on all swords with right click ability.",
        category = MISC_CATEGORY,
        subcategory = "Clear Sight"
    )
    var noBlockAnimation = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Remove Selfie camera",
        description = "Removes selfie camera In F5.",
        category = MISC_CATEGORY,
        subcategory = "QOL"
    )
    var removeSelfieCamera = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Remove Selfie camera only with Hype",
        description = "Removes selfie camera In F5 only when you have a Wither Blade with Wither Impact.",
        category = MISC_CATEGORY,
        subcategory = "QOL"
    )
    var removeSelfieCameraOnlyWithHype = false

    @Property(
        type = PropertyType.SWITCH,
        name = "CustomFOV",
        description = "Allows to set a higher or lower FOV than Minecraft default.",
        category = MISC_CATEGORY,
        subcategory = "QOL"
    )
    var CustomFov = false

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "FOV Value",
        description = "What to set your FOV to.",
        category = MISC_CATEGORY,
        subcategory = "QOL",
        minF = 1f,
        maxF = 179f,
        decimalPlaces = 1
    )
    var CustomFovValue = 90f

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Damage Splash",
        description = "Reformats Hypixel SkyBlock's Shitty Damage Splash for a more readable one.",
        category = MISC_CATEGORY,
        subcategory = "QOL"
    )
    var customDamageSplash = false

    @Property(
        type = PropertyType.SWITCH,
        name = "hide Damage In Sadan Boss fight",
        description = "removes all of the damage numbers in the sadan boss fight\n\nNote: Very Good impact on fps",
        category = MISC_CATEGORY,
        subcategory = "QOL"
    )
    var disableDmgNumbersInSadanBossfight = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Hide Falling Blocks",
        description = "Hides falling blocks, Good for fps.",
        category = MISC_CATEGORY,
        subcategory = "QOL"
    )
    var hideFallingBlocks = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Chest Profit",
        description = "Shows the profit you get from Dungeon Chests.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Dungeon Chect Profit"
    )
    var DungeonChectProfit = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Croesus Chests Profit",
        description = "Shows the profit you get for each chest type in Croesus's menu.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Dungeon Chect Profit"
    )
    var CroesusChestsProfit = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Includes Essence Profit",
        description = "Includes the profit from Essence in the profit overlay.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Dungeon Chect Profit"
    )
    var dungeonChestProfitIncludesEssence = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Sort the chests by profit",
        description = "Sorts the chests in the profit overlay by profit.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Dungeon Chect Profit"
    )
    var CroesusChestsProfitSortByProfit = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Croesus Chest Highlight",
        description = "Highlights the runs you did in Croesus's menu according to the chests state\n\nGreen: None of the chests has been opened.\nYellow: Has 1 opened chests and you can open another one.\nRed: All possible chests have been opened.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Dungeon Chect Profit"
    )
    var CroesusChestHighlight = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Hide Red Highlighted Chests",
        description = "Instand of highlighting the runs that All possible chests have been opened, just hide them.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Dungeon Chect Profit"
    )
    var CroesusChestHighlightHideRedChests = false

    @Property(
        type = PropertyType.SWITCH,
        name = "RNG Drop Announcer",
        description = "Sends in party chat the RNG drop you got with the profit you made",
        category = DUNGEONS_CATEGORY,
        subcategory = "Dungeon Chect Profit"
    )
    var RNGDropAnnouncer = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Requeue",
        description = "Automatically starts a new run in after it ends.",
        category = DUNGEONS_CATEGORY,
        subcategory = "AutoRequeue"
    )
    var autoRequeue = false

    @Property(
        type = PropertyType.NUMBER,
        name = "Auto Requeue Delay",
        description = "Automatically starts a new run in after it ends.",
        category = DUNGEONS_CATEGORY,
        subcategory = "AutoRequeue",
        min = 500,
        max = 7500,
        increment = 250
    )
    var autoRequeueDelay = 5000

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Disable Auto Requeue On Leave",
        description = "Disables Auto Requeue when someone leave/disconnect/kicked from the party Or asked for DownTime.",
        category = DUNGEONS_CATEGORY,
        subcategory = "AutoRequeue"
    )
    var disableAutoRequeueOnLeave = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Auto Requeue Feedback Messages",
        description = "Sends info about the Auto Requeue in party chat.",
        category = DUNGEONS_CATEGORY,
        subcategory = "Floor 4"
    )
    var autoRequeueFeedback = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Spirit Bear Spawn Timer",
        description = "Displays a timer on screen for when the spirit bear is about to spawn\n\nBased on Server Ticks",
        category = DUNGEONS_CATEGORY,
        subcategory = "Floor 4"
    )
    var spiritBearSpawnTimer = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Trace Spirit Bow",
        description = "Shows a tracer for the Spirit Bow",
        category = DUNGEONS_CATEGORY,
        subcategory = "Floor 4"
    )
    var traceSpiritBow = false

    @Property(
        type = PropertyType.COLOR,
        name = "Trace Spirit Bow Color",
        description = "Sets the color for the tracer of the Spirit Bow",
        category = DUNGEONS_CATEGORY,
        subcategory = "Floor 4"
    )
    var traceSpiritBowColor = Color(255, 0, 255)

    @Property(
        type = PropertyType.SWITCH,
        name = "Spirit Bow Hit/Miss Alert",
        description = "Plays a sound when you miss or hit the spirit bow shot in floor 4 on Thorn and displays a title on screen",
        category = ALERTS_CATEGORY,
        subcategory = DUNGEONS_CATEGORY
    )
    var spiritBowHitMissAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Bigger Buttons",
        description = "Modifies the hitbox size of buttons",
        category = DUNGEONS_CATEGORY,
        subcategory = "Full Block"
    )
    var fullblockButton = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Bigger Skulls",
        description = "Modifies the hitbox size of Skulls",
        category = DUNGEONS_CATEGORY,
        subcategory = "Full Block"
    )
    var fullblockSkulls = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Bigger Levers",
        description = "Modifies the hitbox size of Levers",
        category = DUNGEONS_CATEGORY,
        subcategory = "Full Block"
    )
    var fullblockLevers = false

    @Property(
        name = "Bigger Mushrooms",
        type = PropertyType.SWITCH,
        description = "Modifies the hitbox size of Mushrooms",
        category = DUNGEONS_CATEGORY,
        subcategory = "Full Block"
    )
    var fullblockMushroom = false


    @Property(
        type = PropertyType.SWITCH,
        name = "Smooth BossBar Health",
        description = "Smooths the bossbar health going up and down",
        category = MISC_CATEGORY,
        subcategory = "QOL"
    )
    var smoothBossBarHealth = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Chum NameTags",
        description = "make it so the nametags visibility wont change behind blocks",
        category = MISC_CATEGORY,
        subcategory = "QOL"
    )
    var chumNameTags = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Print SkyBlock Exp In Chat",
        description = "Sends the SkyBlock Exp you get in chat",
        category = GENERAL_CATEGORY,
        subcategory = "Chat"
    )
    var SkyBlockExpInChat = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Relic Timer",
        description = "Sends in chat the time took to pickup the relic/place it down",
        category = DUNGEONS_CATEGORY,
        subcategory = "M7"
    )
    var M7RelicPickupTimer = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Relic Look",
        description = "Rotates your head to look at the relic's cauldron",
        category = DUNGEONS_CATEGORY,
        subcategory = "M7"
    )
    var M7RelicLook = false

    @Property(
        type = PropertyType.NUMBER,
        name = "Relic Look Rotation Time",
        category = DUNGEONS_CATEGORY,
        subcategory = "M7",
        min = 0,
        max = 500,
        increment = 10
    )
    var M7RelicLookTime = 200

    @Property(
        type = PropertyType.SWITCH,
        name = "Crystal Spawn Timer",
        description = "Displays a timer for when Maxor's crystal are about to spawn. \n\nBased on server ticks so will work perfectly even if the server is lagging",
        category = DUNGEONS_CATEGORY,
        subcategory = "F7"
    )
    var CrystalSpawnTimer = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Crystal Place Timer",
        description = "Sends a message about the time it took to place the crystal at Maxor Phase.",
        category = DUNGEONS_CATEGORY,
        subcategory = "F7"
    )
    var CrystalPlaceTimer = false

    @Property(
        type = PropertyType.BUTTON,
        name = "Dungeon Map",
        description = "Toggles the Map",
        category = DMAP_CATEGORY,
        subcategory = "Map"
    )
    fun openDungeonMapConfig() {
        openScreen(DungeonMapConfig.gui())
    }


    private const val DEV_MODE_DESCRIPTION =
        "Forces all features to enable, even if you are not on skyblock.\n\n" +
                "enables console logging and disables a few safety checks\n" +
                "Q: Why is this a thing?\n" +
                "A: So I can properly test features in the mod without needing to be in skyblock\n\n" +
                "DONT USE IT IF U ARE NOT ME, CAN GET YOU BANNED!\n\n" +
                "[R.I.P] FININ1, NoamIsSad"

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Dev Mode",
        description = DEV_MODE_DESCRIPTION,
        category = DEV_CATEGORY
    )
    var DevMode = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Rat Protection",
        description = "Blocks anyone who tries to log into your account while you are playing\n\n It works by spamming your endpoint with requests to tempererly block it making it impossible for " +
                "servers to authenticate you",
        category = DEV_CATEGORY,
        subcategory = "Experimental",
        hidden = false
    )
    var ratProtection = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Disable Visual Words",
        category = DEV_CATEGORY,
        subcategory = "Experimental"
    )
    var disableVisualWords = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Motion Blur",
        description = "Enables motion blur",
        category = DEV_CATEGORY,
        subcategory = "Experimental"
    )
    var MotionBlur = false

    @Property(
        type = PropertyType.SLIDER,
        name = "Motion Blur Amount",
        description = "Amount of motion blur",
        category = DEV_CATEGORY,
        subcategory = "Experimental",
        min = 1,
        max = 10
    )
    var MotionBlurAmount = 5

    @Property(
        type = PropertyType.SWITCH,
        name = "Profile Viewer Command",
        description = "disables the pv command from the mod so it wont conflict with neu's pv command",
        category = DEV_CATEGORY,
        subcategory = "Experimental"
    )
    var pvCommand = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Update Check",
        description = "A switch for enabling/disabling the update checker.",
        category = DEV_CATEGORY,
        subcategory = "Data"
    )
    var UpdateCheck = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Toggle Client Branding",
        category = DEV_CATEGORY,
        subcategory = "Experimental"
    )
    var toggleClientBranding = true

    /*
        @Property(
            type = PropertyType.BUTTON,
            name = "Refetch all data",
            description = "Refetch all json files the mod gets at startup",
            category = DEV_CATEGORY,
            subcategory = "Data"
        )
        fun refetchAllData() {
            JsonUtils.refetchAllData()
        }*/

    init {

        /*
        registerListener("pvCommand") { newValue: Boolean ->
            if (newValue) CommandManager.registerCommand(ProfileViewerCommand)
            else CommandManager.unregisterCommand(ProfileViewerCommand)
        }*/


        setCategoryDescription(
            EMPTY_CATEGORY,
            """
                $FULL_PREFIX&r &6($MOD_VERSION)
                
                &fThanks for using NoammAddons!
                
                &fIf you run into any &cbugs &for have &asuggestions&f
                &ffeel free to report them on my GitHub or Discord!
                
                &6For a list of all commands type &f/na help&r&6.
            """.trimIndent()
        )

        setCategoryDescription(
            ZPT_CATEGORY, listOf(
                "Instantly Teleport without waiting for the server's teleport packet.",
                "Instead, it performs all necessary calculations client-side and teleports you immediately.",
                "While the prediction is good, it still far from being perfect.",
                "There are some failsafes to prevent you from teleporting to places ",
                "you cant like in some dungeon puzzles or Skyblock zones.",
                "And on top of that if the two last teleport attempts fail",
                "the feature will temporarily revert to normal etherwarp behavior.",
                "Regardless of that dont be stupid with this feature on."
            ).joinToString("\n")
        )
        setSubcategoryDescription(ZPT_CATEGORY, "KeepMotion", "Keeps your momentum between teleports.")


        setCategoryDescription(ESP_CATEGORY, "Disable Optifine's fast render and Patcher's entity culling.")
        setCategoryDescription(DEV_CATEGORY, "A list of Broken/Unfinished features and UAYOR.")


        // General
        addDependency(::ChatCoordsWayPointColor.javaField !!, ::ChatCoordsWayPoint.javaField !!)
        addDependency(::CustomSlotHighlightColor.javaField !!, ::CustomSlotHighlight.javaField !!)
        addDependency(::wardrobeKeybindsCloseAfterUse.javaField !!, ::wardrobeKeybinds.javaField !!)

        listOf(
            ::teleportOverlayEtherwarp,
            ::teleportOverlayInstantTransmission,
            ::teleportOverlayWitherImpact,
            ::teleportOverlayType,
            ::teleportOverlayOutlineThickness,
            ::teleportOverlayOutlineColor,
            ::teleportOverlayOverlayColor,
            ::etherwarpOverlayOutlineColorInvalid,
            ::etherwarpOverlayOverlayColorInvalid,
            ::TeleportOverlayESP,
        ).forEach { addDependency(it.javaField !!, ::teleportOverlay.javaField !!) }


        listOf(
            ::pcPtme, ::pcWarp, ::pcAllinv,
            ::pcInv, ::pcFloor, ::pcMasterFloor,
            ::pcDt, ::pcCoords, ::pcTPS,
            ::pcPing, ::pcGay,
        ).forEach { addDependency(it.javaField !!, ::PartyCommands.javaField !!) }

        // Zero Ping Teleportation
        listOf(
            ::zeroPingEtherwarp,
            ::zeroPingInstantTransmission,
            ::zeroPingWitherImpact,
            ::zeroPingEtherwarpKeepMotion,
            ::zeroPingInstantTransmissionKeepMotion,
            ::zeroPingWitherImpactKeepMotion
        ).forEach { addDependency(it.javaField !!, ::zeroPingTeleportation.javaField !!) }

        // Dungeons
        listOf(
            ::CroesusChestsProfit,
            ::dungeonChestProfitIncludesEssence,
            ::CroesusChestsProfitSortByProfit,
            ::CroesusChestHighlight,
            ::CroesusChestHighlightHideRedChests,
            ::RNGDropAnnouncer
        ).forEach { addDependency(it.javaField !!, ::DungeonChectProfit.javaField !!) }

        listOf(
            ::secretSoundName,
            ::secretSoundPitch,
            ::secretSoundVolume,
        ).forEach { addDependency(it.javaField !!, ::secretSound.javaField !!) }
        addDependency(::playSecretSound.name, ::boulderSolver.name)

        addDependency(::secretClickedColor.javaField !!, ::clickedSecrets.javaField !!)

        listOf(
            ::boulderSolverShowAll,
            ::boulderSolverBoxColor,
            ::boulderSolverClickColor
        ).forEach { addDependency(it.javaField !!, this::boulderSolver.javaField !!) }

        listOf(
            ::AutoI4RotatinTime,
            ::autoI4Prediction,
            ::autoI4AutoRod,
            ::autoI4AutoMask,
            ::autoI4AutoLeap
        ).forEach { addDependency(it.javaField !!, ::autoI4.javaField !!) }

        listOf(
            ::autoRequeueDelay,
            ::disableAutoRequeueOnLeave
        ).forEach { addDependency(it.javaField !!, ::autoRequeue.javaField !!) }

        listOf(
            ::BetterFloor7,
            ::BetterFloor6,
            ::BetterFloor5
        ).forEach { addDependency(it.javaField !!, ::BetterFloors.javaField !!) }
        listOf(
            ::MimicEffi10,
            ::GhostBlocks,
            ::LegitGhostPick
        ).forEach { addDependency(it.javaField !!, ::GhostPick.javaField !!) }
        listOf(
            ::P1StartTimer,
            ::P2StartTimer,
            ::P3StartTimer,
            ::P4StartTimer
        ).forEach { addDependency(it.javaField !!, ::F7M7PhaseStartTimers.javaField !!) }

        listOf(
            ::BlazeSolverFirstBlazeColor,
            ::BlazeSolverSecondBlazeColor,
            ::BlazeSolverThirdBlazeColor,
            ::BlazeSolverLineColor
        ).forEach { addDependency(it.javaField !!, ::BlazeSolver.javaField !!) }

        listOf(
            ::CreeperBeamSolverLines,
            ::CreeperBeamSolverPhase
        ).forEach { addDependency(it.javaField !!, ::CreeperBeamSolver.javaField !!) }


        addDependency(::AutoPotionCommand.javaField !!, ::AutoPotion.javaField !!)
        //  addDependency(AutoReaperArmorSlot, AutoReaperArmorSwap)
        addDependency(::CroesusChestsProfitSortByProfit.javaField !!, ::CroesusChestsProfit.javaField !!)
        addDependency(::CroesusChestHighlightHideRedChests.javaField !!, ::CroesusChestHighlight.javaField !!)

        // Terminals
        listOf(
            ::CustomTerminalMenuClickMode,
            ::CustomTerminalMenuScale,
            ::CustomTerminalMenuLightMode,
            ::CustomTerminalMenuSolutionColor,
            ::CustomMelodyTerminal,
            ::CustomNumbersTerminal,
            ::CustomRubixTerminal,
            ::CustomRedGreenTerminal,
            ::CustomStartWithTerminal,
            ::CustomColorsTerminal,
        ).forEach { addDependency(it.javaField !!, ::CustomTerminalsGui.javaField !!) }


        // Miscs
        listOf(
            ::BlockOverlayType,
            ::BlockOverlayOutlineThickness,
            ::BlockOverlayOutlineColor,
            ::BlockOverlayOverlayColor,
            ::BlockOverlayESP,
        ).forEach { addDependency(it.javaField !!, ::BlockOverlay.javaField !!) }
        listOf(
            ::PlayerScaleOnEveryone,
            ::PlayerScaleValue,
        ).forEach { addDependency(it.javaField !!, ::PlayerScale.javaField !!) }
        addDependency(::TimeChangerMode.javaField !!, ::TimeChanger.javaField !!)
        addDependency(::CustomFovValue.javaField !!, ::CustomFov.javaField !!)
        addDependency(::removeSelfieCameraOnlyWithHype.javaField !!, ::removeSelfieCamera.javaField !!)
        listOf(
            ::SpinDirection,
            ::SpinSpeed
        ).forEach { addDependency(it.javaField !!, ::PlayerSpin.javaField !!) }
        addDependency(::NoRotateKeepMotion.javaField !!, ::NoRotate.javaField !!)
        addDependency(::disableDmgNumbersInSadanBossfight.javaField !!, ::customDamageSplash.javaField !!)


        // ESP
        listOf(
            ::espColorUnstable,
            ::espColorYoung,
            ::espColorSuperior,
            ::espColorHoly,
            ::espColorFrozen,
            ::espColorAngryArchaeologist
        ).forEach { addDependency(it.javaField !!, ::espSeperateMinibossColor.javaField !!) }
        addDependency(::espColorMiniboss.javaField !!, ::espMiniboss.javaField !!)
        listOf(
            ::espColorStarMobs,
            ::removeStarMobsNametag
        ).forEach { addDependency(it.javaField !!, ::espStarMobs.javaField !!) }
        addDependency(::hideWrongLivids.javaField !!, ::lividFinder.javaField !!)
        addDependency(::espColorBats.javaField !!, ::espBats.javaField !!)
        addDependency(::espColorFels.javaField !!, ::espFels.javaField !!)
        addDependency(::espColorShadowAssassin.javaField !!, ::espShadowAssassin.javaField !!)
        addDependency(::removeStarMobsNametag.javaField !!, ::espStarMobs.javaField !!)
        addDependency(::pestEspColor.javaField !!, ::pestEsp.javaField !!)
        addDependency(::espThornColor.javaField !!, ::espThorn.javaField !!)
        addDependency(::espSpiritBearColor.javaField !!, ::espSpiritBear.javaField !!)


        // GUI
        listOf(
            ::overlayColorSalvageable,
            ::overlayColorTopSalvageable
        ).forEach { addDependency(it.javaField !!, ::overlaySalvageable.javaField !!) }
        listOf(
            ::CustomLeapMenuScale,
            //    CustomLeapMenuLightMode
        ).forEach { addDependency(it.javaField !!, ::CustomLeapMenu.javaField !!) }
        addDependency(::ScalableTooltipsScale.javaField !!, ::ScalableTooltips.javaField !!)
        addDependency(::SlotBindingShowBinding.javaField !!, ::SlotBinding.javaField !!)


        listOf(
            ::customMenusScale,
            ::CustomPartyFinderMenu,
            ::CustomPetMenu,
            ::CustomWardrobeMenu
        ).forEach { addDependency(it.javaField !!, ::customMenus.javaField !!) }


        // Hud
        addDependency(::FpsDisplayColor.javaField !!, ::FpsDisplay.javaField !!)
        addDependency(::ClockDisplayColor.javaField !!, ::ClockDisplay.javaField !!)
        listOf(
            ::PlayerHUDSpeed,
            ::PlayerHUDEffectiveHP,
            ::PlayerHUDOverflowMana,
            ::PlayerHUDAlternateOverflowMana,
            ::PlayerHUDMana,
            ::PlayerHUDDefense,
            ::PlayerHUDHealth,
        ).forEach { addDependency(it.javaField !!, ::PlayerHUD.javaField !!) }

    }

    private object Sorting: SortingBehavior() {
        override fun getCategoryComparator(): Comparator<in Category> = Comparator.comparingInt { c: Category ->
            listOf(
                GENERAL_CATEGORY,
                ZPT_CATEGORY,
                DMAP_CATEGORY,
                DUNGEONS_CATEGORY,
                TERMINALS_CATEGORY,
                ESP_CATEGORY,
                SLAYER_CATEGORY,
                ALERTS_CATEGORY,
                GUI_CATEGORY,
                HUD_CATEGORY,
                MISC_CATEGORY,
                DEV_CATEGORY
            ).indexOf(c.name)
        }
    }
}