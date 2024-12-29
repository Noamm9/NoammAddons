package noammaddons.config

import gg.essential.universal.UDesktop
import gg.essential.vigilance.Vigilant
import gg.essential.vigilance.data.*
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.noammaddons.Companion.FULL_PREFIX
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.GuiUtils.openScreen
import noammaddons.utils.RenderHelper.applyAlpha
import java.awt.Color
import java.io.File
import java.net.URI


object Config: Vigilant(File("./config/NoammAddons/config.toml"), FULL_PREFIX, sortingBehavior = Sorting) {
    @Property(
        type = PropertyType.BUTTON,
        name = "Join my Discord Server",
        description = "Feel free to join my Discord Server.",
        category = "General",
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
        category = "General",
        placeholder = "CLICK"
    )
    fun openHudEditGUI() {
        openScreen(HudEditorScreen())
    }

    @Property(
        type = PropertyType.SWITCH,
        name = "Slot Binding",
        description = "Allows binding of slots to hotbar slots for quick item swaps, configurable via the SlotBinding Keybind in Options/Controls.\n" +
                "Usage: Hold the Bind key, click the source slot, then the target hotbar slot. To remove bindings, hold the Keybind and click the first slot to clear.",
        category = "General",
        subcategory = "SlotBinding"

    )
    var SlotBinding = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Show Bound Slots",
        description = "Shows the currently bound slots in the Inventory",
        category = "General",
        subcategory = "SlotBinding"
    )
    var SlotBindingShowBinding = true

    @Property(
        type = PropertyType.COLOR,
        name = "Line Color",
        description = "The color of the line that is connecting between the slots",
        category = "General",
        subcategory = "SlotBinding",
        allowAlpha = false
    )
    var SlotBindingLineColor = Color.CYAN !!

    @Property(
        type = PropertyType.COLOR,
        name = "Border Color",
        description = "The color of the border that is being draw on the items",
        category = "General",
        subcategory = "SlotBinding",
        allowAlpha = false
    )
    var SlotBindingBorderColor = Color.CYAN !!

    @Property(
        type = PropertyType.SWITCH,
        name = "Chat Emojis",
        description = "Replaces chat emojis with their unicode representations.\n\nSame as &6[MVP&d++&6]",
        category = "General",
        subcategory = "Chat"
    )
    var ChatEmojis = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Cake's Year",
        description = "Shows the new year cake's year on the item in the inventory",
        category = "General",
        subcategory = "Cake"
    )
    var cakeNumbers = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Remove Useless Messages",
        description = "Removes messages from chat.",
        category = "General",
        subcategory = "Chat"
    )
    var RemoveUselessMessages = false

    @Property(
        type = PropertyType.SWITCH,
        name = "§e§lBlaze §6§lSolver",
        category = "Dungeons",
        subcategory = "Solvers",
    )
    var BlazeSolver = false

    @Property(
        type = PropertyType.COLOR,
        name = "§eBlaze §6Solver §fFirst §aC§bo§cl§do§er§f",
        description = "Color of the first blaze used by the Blaze Solver",
        category = "Dungeons",
        subcategory = "Solvers"
    )
    var BlazeSolverFirstBlazeColor = Color(0, 114, 255, 85)

    @Property(
        type = PropertyType.COLOR,
        name = "§eBlaze §6Solver §fSecond §aC§bo§cl§do§er§f",
        description = "Color of the second blaze used by the Blaze Solver",
        category = "Dungeons",
        subcategory = "Solvers"
    )
    var BlazeSolverSecondBlazeColor = Color(255, 255, 0, 85)

    @Property(
        type = PropertyType.COLOR,
        name = "§eBlaze §6Solver §fThird §aC§bo§cl§do§er§f",
        description = "Color of the third blaze used by the Blaze Solver",
        category = "Dungeons",
        subcategory = "Solvers"
    )
    var BlazeSolverThirdBlazeColor = Color(255, 0, 0, 85)

    @Property(
        type = PropertyType.COLOR,
        name = "§eBlaze §6Solver §fLine §aC§bo§cl§do§er§f",
        description = "Color of the line used by the Blaze Solver",
        category = "Dungeons",
        subcategory = "Solvers"
    )
    var BlazeSolverLineColor = Color(255, 255, 255, 255)

    @Property(
        type = PropertyType.SWITCH,
        name = "Left Click Etherwarp",
        description = "Allows you to use Etherwarp with left click.",
        category = "General",
        subcategory = "Etherwarp"
    )
    var LeftClickEtherwarp = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Etherwarp Sound",
        description = "Plays a sound when using Etherwarp.",
        category = "General",
        subcategory = "Etherwarp"
    )
    var EtherwarpSound = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dragon Spawn Timer / Prio",
        description = "Displays a timer that is based on server ticks so will work perfectly even if the server is lagging. and draws a tracer to the dragon statue\n\n Also acts as a Prio, Automaticly detects your class",
        category = "Dungeons",
        subcategory = "M7 Dragons"
    )
    var M7dragonsSpawnTimer = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dragon Kill Box",
        description = "Renders a box that shows the area that the dragon needs to be killed it for it to count as dead",
        category = "Dungeons",
        subcategory = "M7 Dragons"
    )
    var M7dragonsKillBox = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Dragon Arrows Hit/Debuff in chat",
        description = "If you are playing a debuff class, it will show how many arrows you have hit the dragon and if u hit your ice spray. and if you are playing arch/bers it will show the ammount of arrows you hit on the dragon",
        category = "Dungeons",
        subcategory = "M7 Dragons"
    )
    var M7dragonsShowDebuff = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Bow Hit Sound",
        description = "Plays a custom sound effect when you hit a mob",
        category = "General",
        subcategory = "Chat"
    )
    var CustomBowHitSound = false

    @Property(
        type = PropertyType.SWITCH,
        name = "M7 Relic Spawn Timer",
        description = "Displays a timer for the M7 Relic in the chat. \nBased on server ticks so will work perfectly even if the server is lagging",
        category = "Dungeons",
        subcategory = "M7"
    )
    var M7RelicSpawnTimer = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Wither Shield Display",
        description = "Shows the cooldown of the Wither shield ability on a wither blade with wither impact ability",
        category = "HUD",
        subcategory = "Items"
    )
    var WitherShieldTimer = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Party Names",
        description = "Displays the names of the people in the party.",
        category = "General",
        subcategory = "Party"
    )
    var partyNames = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Party Outline",
        description = "Draws a rainbow Outline around the people in the party.",
        category = "General",
        subcategory = "Party"
    )
    var partyOutline = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Party Commands",
        description = "§fIf this switch is disabled all features below§f will be off regardless of their §fconfiguration.\n\n§dCredits to §d§loCookie§r§d for the original code. §bAll i did was modify it to fit my needs.\n\n§fAllows §9Party members§f to §cexecute §6leader commands§f in chat \n\n§b§nExsample: \n§6!w §f=> §bwill make you warp the party\n§d!ai §f=> §bwill Toggle the allinvite setting of the party",
        category = "General",
        subcategory = "Party"
    )
    var PartyCommands = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§e!ptme {name} (Transfers the party)",
        description = "§bAlias: §e!pt",
        category = "General",
        subcategory = "Party"
    )
    var pcPtme = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§6!warp (warps)",
        description = "§bAlias: §6!w",
        category = "General",
        subcategory = "Party"
    )
    var pcWarp = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§d!ai (Toggles allinvite)",
        description = "§bAlias: §d!allinv, §d!ai",
        category = "General",
        subcategory = "Party"
    )
    var pcAllinv = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§b!invite {name} (invites a player)",
        description = "§bAlias: §b!inv",
        category = "General",
        subcategory = "Party"
    )
    var pcInv = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§a!floor { 0 - 7 } (joins Normal Dungeon)",
        description = "§bAlias: §a!f",
        category = "General",
        subcategory = "Party"
    )
    var pcFloor = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§4!master { 1 - 7 } (joins Master Dungeon)",
        description = "§bAlias: §4!m",
        category = "General",
        subcategory = "Party"
    )
    var pcMasterFloor = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§c!downtime {Reason}",
        description = "§bAlias: §c!dt",
        category = "General",
        subcategory = "Party"
    )
    var pcDt = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§b!coords (sends coords)",
        description = "§bAlias: §b!cords",
        category = "General",
        subcategory = "Party"
    )
    var pcCoords = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§e!tps (sends server tps)",
        category = "General",
        subcategory = "Party"
    )
    var pcTPS = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§d!ping (sends ping)",
        category = "General",
        subcategory = "Party"
    )
    var pcPing = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "§b!gay {name} (gay check)",
        category = "General",
        subcategory = "Party"
    )
    var pcGay = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Spring Boots Display",
        description = "Renders Sexy Spring Boots Charge Display",
        category = "HUD",
        subcategory = "Items",
    )
    var SpringBootsDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Grounded Items Names",
        description = "Shows the name of the grounded item.",
        category = "General",
        subcategory = "Misc"
    )
    var ShowItemEntityName = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Chat Coords WayPoint",
        description = "Creates a waypoint whenever a received chat message matches\n\n&bx: 1, y: 1, z: 1",
        category = "General",
        subcategory = "Chat"
    )
    var ChatCoordsWayPoint = true

    @Property(
        type = PropertyType.COLOR,
        name = "WayPoint Color",
        description = "The Color of the waypoint",
        category = "General",
        subcategory = "Chat"
    )
    var ChatCoordsWayPointColor = Color(0, 114, 255, 85)

    @Property(
        type = PropertyType.SWITCH,
        name = "Better Floors",
        description = "Global Switch\nBasically My FunnyMapExtras's Config port\nPlaces and remove a some of Blocks in the boss fight",
        category = "Dungeons",
        subcategory = "Better Floors"
    )
    var BetterFloors = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Better 7",
        description = "Enables the 7th Floor config",
        category = "Dungeons",
        subcategory = "Better Floors"
    )
    var BetterFloor7 = true

    @Property(
        type = PropertyType.SWITCH,
        name = "SB Kick Duration",
        description = "Shows a timer on screen for when you can rejoin SkyBlock after being kicked",
        category = "General",
        subcategory = "Misc"
    )
    var SBKickDuration = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Better 6",
        description = "Enables the 6th Floor config",
        category = "Dungeons",
        subcategory = "Better Floors"
    )
    var BetterFloor6 = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Better 5",
        description = "Enables the 5th Floor config",
        category = "Dungeons",
        subcategory = "Better Floors"
    )
    var BetterFloor5 = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Ghost Pick",
        description = "Main toggle of this Category\nChoose the options you want to use below",
        category = "Dungeons",
        subcategory = "GhostPick"
    )
    var GhostPick = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Legit Ghost Pick",
        description = "Makes the block you mine regularly stay as air blocks",
        category = "Dungeons",
        subcategory = "GhostPick"
    )
    var LegitGhostPick = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Ghost Blocks",
        description = "Makes the blocks you look at turn into air with a reach of up to 100 blocks",
        category = "Dungeons",
        subcategory = "GhostPick"
    )
    var GhostBlocks = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Efficiency 10",
        description = "Make you instantly mine blocks",
        category = "Dungeons",
        subcategory = "GhostPick"
    )
    var MimicEffi10 = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Teammates Names",
        description = "Shows the names your teammates in dungeon\nColored By Class.",
        category = "Dungeons",
        subcategory = "Dungeon Teammates"
    )
    var dungeonTeammatesNames = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Teammates ESP",
        description = "ESP your teammates in dungeon.\nColored By Class.",
        category = "Dungeons",
        subcategory = "Dungeon Teammates"
    )
    var dungeonTeammatesEsp = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Ender Pearl Fix",
        description = "Disables Hypixel's stupid Ender Pearls throw block when you are too close to a wall/floor/ceiling.",
        category = "Dungeons",
        subcategory = "Ender Pearls"
    )
    var enderPearlFix = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Refill Ender Pearls",
        description = "Automatically refills your Ender Pearls at the start of the run.",
        category = "Dungeons",
        subcategory = "Ender Pearls"
    )
    var refillEnderPearls = false

    @Property(
        type = PropertyType.TEXT,
        name = "Announce Spirit Leaps",
        description = "Says in party chat who did you leaped to\n You can use {name} to get the leaped player's name\n &l&4leave empty to disable.",
        category = "Dungeons",
        subcategory = "General",
        placeholder = "ILY ❤ {name}"
    )
    var AnnounceSpiritLeaps = "ILY ❤ {name}"

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Show Extra Stats",
        description = "Automatically sends /showextrastats after the end of the run.",
        category = "Dungeons",
        subcategory = "General"
    )
    var showExtraStats = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Ult",
        description = "Automatically uses your ULTIMATE whenever you need.",
        category = "Dungeons",
        subcategory = "General"
    )
    var autoUlt = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Potion",
        description = "Automatically takes a potion before you join a dungeon",
        category = "Dungeons",
        subcategory = "General"
    )
    var AutoPotion = false

    @Property(
        type = PropertyType.TEXT,
        name = "Auto Potion Command",
        description = "The Command to use to take a potion, e.g '/pb', '/bp {num}', '/ec {num}'.",
        category = "Dungeons",
        subcategory = "General",
        placeholder = "/pb"
    )
    var AutoPotionCommand = "/pb"

    @Property(
        type = PropertyType.SWITCH,
        name = "Trace Keys",
        description = "Draws a line from your mouse cursor to the Wither/Blood key.",
        category = "Dungeons",
        subcategory = "General"
    )
    var TraceKeys = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Relic Outline",
        description = "Highlights the Relic Cauldron. of the Relic you picked",
        category = "Dungeons",
        subcategory = "M7"
    )
    var M7RelicOutline = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Blood Dialogue Skip",
        description = "displays a timer for §n24 seconds§r after you open the blood room \n\n§b§lTip: §c&lYou need to be in blood when timer ends",
        category = "Dungeons",
        subcategory = "General"
    )
    var BloodDialogueSkip = false

    @Property(
        type = PropertyType.SWITCH,
        name = "§aAuto §0Reaper §cArmor §6Swap",
        description = "§aAutomatically§f does the §0Reaper §cArmor §6Swap§f before the dragons on §4M7 §cP5§f Spawns\n\n §b§nNeed to have the Reaper Armor on the first page in your wardrobe&r \n\n §6Can also be Triggered with /na ras command ",
        category = "Dungeons",
        subcategory = "General",
        searchTags = ["reaper", "armor", "slot", "reaperarmor", "auto"]
    )
    var AutoReaperArmorSwap = false

    @Property(
        type = PropertyType.SLIDER,
        name = "§aAuto §0Reaper §cArmor Slot",
        description = "§aThe slot where the §0Reaper §cArmor§f is located\n\n §c§lfrom 1 to 9",
        category = "Dungeons",
        subcategory = "General",
        min = 1,
        max = 9,
        searchTags = ["reaper", "armor", "slot", "reaperarmor", "auto"]
    )
    var AutoReaperArmorSlot = 1

    @Property(
        type = PropertyType.SWITCH,
        name = "Announce Drafts Resets",
        description = "Says in party chat when you used Architect's First Draft to reset a failed puzzle.",
        category = "Dungeons",
        subcategory = "Puzzles",
    )
    var AnnounceDraftResets = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Architect Draft",
        description = "Automatically runs /gfs architect's first draft 1 when you fail a puzzle in dungeon.",
        category = "Dungeons",
        subcategory = "Puzzles",
    )
    var AutoArchitectDraft = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Highlight Mimic Chest",
        description = "Highlights the Mimic Chest",
        category = "Dungeons",
        subcategory = "Secrets"
    )
    var HighlightMimicChest = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Ability Keybinds",
        description = "Allows to use the Your Class ULTIMATE/ABILITY with a keybind witch can be configirate in Minecraft's Options/Controls",
        category = "Dungeons",
        subcategory = "General"
    )
    var DungeonAbilityKeybinds = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto Close Secrets Chest",
        category = "Dungeons",
        subcategory = "Secrets"
    )
    var autoCloseSecretChests = false

    @Property(
        type = PropertyType.SWITCH,
        name = "I HATE DIORITE",
        description = "Replaces the diorite blocks in storm pillars with colored stained glass.",
        category = "Dungeons",
        subcategory = "F7"
    )
    var IHATEDIORITE = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Auto I4",
        description = "Fully Automated I4. \naims and shoots the emerald block at the forth dev in P3, Predict the next one for faster time. \nMacro: Rod swap, Mask swap, safe leap \n\n §f[ §b§nNeed a term&r §f§n§land§r §e§n100 atk speed§r §f] ",
        category = "Dungeons",
        subcategory = "F7"
    )
    var autoI4 = false

    @Property(
        type = PropertyType.NUMBER,
        name = "Auto I4 Rotation time",
        description = "How fast should the head movements be for the shoting action\n\n&fNote: Will effect the consistency of the Auto I4 for better or worse",
        category = "Dungeons",
        subcategory = "F7",
        increment = 10,
        min = 100,
        max = 350
    )
    var AutoI4RotatinTime = 200

    @Property(
        type = PropertyType.SWITCH,
        name = "Better F7 Titles",
        description = "Replaces the big and annoying f7 titles with smaller and cleaner ones and display them on screen\n\n&b&nExsamples:\n\n&r &a1/2 Energy Crystals are now active!&f ==> &f(&c1&f/&b2&f) \n &aNoamm9&a activated a Terminal! (&c6&f/&a7&f)&f ==> &f(&c6&a/7&f)",
        category = "Dungeons",
        subcategory = "F7"
    )
    var BetterF7Titles = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Hide Players After Leap",
        description = "Hides players after you leap to them for 2 seconds allowing you to see clearly",
        category = "Dungeons",
        subcategory = "Spirit leap"
    )
    var hidePlayersAfterLeap = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Show Gyro Radius",
        description = "Shows the Gyrokinetic wand sucking radius",
        category = "Dungeons",
        subcategory = "Gyrokinetic Wand"
    )
    var ShowGyroCircle = false

    @Property(
        type = PropertyType.COLOR,
        name = "Gyro Radius Ring Color",
        description = "the Color of the Gyrokinetic wand sucking radius",
        category = "Dungeons",
        subcategory = "Gyrokinetic Wand"
    )
    var ShowGyroCircleRingColor = Color.GREEN !!

    @Property(
        type = PropertyType.COLOR,
        name = "Gyro Radius Block Color",
        description = "the Color of the Gyrokinetic wand sucking radius",
        category = "Dungeons",
        subcategory = "Gyrokinetic Wand"
    )
    var ShowGyroCircleBlockColor = Color.GREEN.applyAlpha(85)

    @Property(
        type = PropertyType.SWITCH,
        name = "Stop Close My Chat",
        description = "Prevents your chat from being closed by the server or world swapping",
        category = "GUI",
        subcategory = "chat"
    )
    var StopCloseMyChat = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "F7/M7 Phase Start Timers",
        description = "Global Toggle, Based on Server Ticks",
        category = "Dungeons",
        subcategory = "Timers"
    )
    var F7M7PhaseStartTimers = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "P1 Start Timer",
        description = "Shows a Timer on screen when Maxor Phase will start",
        category = "Dungeons",
        subcategory = "Timers"
    )
    var P1StartTimer = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "P2 Start Timer",
        description = "Shows a Timer on screen when Storm Phase will start",
        category = "Dungeons",
        subcategory = "Timers"
    )
    var P2StartTimer = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "P3 Start Timer",
        description = "Shows a Timer on screen when Goldor Phase will start",
        category = "Dungeons",
        subcategory = "Timers"
    )
    var P3StartTimer = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "P4 Start Timer",
        description = "Shows a Timer on screen when Necron Phase will start",
        category = "Dungeons",
        subcategory = "Timers"
    )
    var P4StartTimer = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Livid Solver",
        category = "Dungeons",
        subcategory = "Render"
    )
    var lividFinder = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Hide Wrong Livids",
        description = "Hide The Livids that you shouldn't be killing",
        category = "Dungeons",
        subcategory = "Render"
    )
    var hideWrongLivids = false

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

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Terminal Guis",
        description = "Global Switch for Custom Terminal Guis",
        category = "Terminals",
        subcategory = "Custom Terminal Guis",
    )
    var CustomTerminalsGui = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Custom Terminals Gui Click Mode",
        description = "The method used to click on the Custom Terminals Menu\n\n NORMAL: as usual just click on the menu\n HOVER: hover over the solutions and it will click them\n AUTO: Automatically Clicks on the Solutions\n\n&4&lNote: Auto and Hover are UAYOR!",
        category = "Terminals",
        subcategory = "Custom Terminal Guis",
        options = ["NORMAL", "HOVER", "AUTO"],
        hidden = true
    )
    var CustomTerminalMenuClickMode = 0

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom Terminals Gui Scale",
        description = "Scale of the Custom Terminals Menu",
        category = "Terminals",
        subcategory = "Custom Terminal Guis",
    )
    var CustomTerminalMenuScale = 1f

    @Property(
        type = PropertyType.COLOR,
        name = "Solution Color",
        description = "The Color' of the Solution",
        category = "Terminals",
        subcategory = "Custom Terminal Guis",
    )
    var CustomTerminalMenuSolutionColor = Color(0, 114, 255)

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Light Mode Gui?",
        description = "Changes the Color Mode of the Custom Terminals Gui",
        category = "Terminals",
        subcategory = "Custom Terminal Guis",
    )
    var CustomTerminalMenuLightMode = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Terminal Numbers",
        description = "Places a number on each terminal so you know what number it is.",
        category = "Terminals",
        subcategory = "Extras"
    )
    var TerminalNumbers = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Melody Terminal",
        description = "",
        category = "Terminals",
        subcategory = "Terminals"
    )
    var CustomMelodyTerminal = true

    @Property(
        type = PropertyType.TEXT,
        name = "Melody Alert",
        description = "Sends a Message in chat when you open Melody Terminal\nDelete all text to disable \n\n&l&4leave empty to disable",
        category = "Terminals",
        subcategory = "Extras",
        placeholder = "I ❤ Melody"
    )
    var MelodyAlert = "I ❤ Melody"

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Numbers Terminal",
        description = "",
        category = "Terminals",
        subcategory = "Terminals"
    )
    var CustomNumbersTerminal = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Rubix Terminal",
        description = "&6&l[&d&lTIP&6&l]&r &b&lNo need to swap between &n&lRightclick&r&b&l and &nLeftclick&r&b&l",
        category = "Terminals",
        subcategory = "Terminals"
    )
    var CustomRubixTerminal = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Red Green Terminal",
        description = "",
        category = "Terminals",
        subcategory = "Terminals"
    )
    var CustomRedGreenTerminal = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Start With Terminal",
        description = "",
        category = "Terminals",
        subcategory = "Terminals"
    )
    var CustomStartWithTerminal = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Color Terminal",
        description = "",
        category = "Terminals",
        subcategory = "Terminals"
    )
    var CustomColorsTerminal = true

    @Property(
        type = PropertyType.SELECTOR,
        name = "ESP Type",
        category = "ESP",
        options = ["Outline", "Box", "Filled Outline"]
    )
    var espType = 0

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Outline Opacity",
        category = "ESP",
    )
    var espOutlineOpacity = 1f

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Filled Opacity",
        category = "ESP",
    )
    var espFilledOpacity = 0.3f

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "OutlineESP Width",
        category = "ESP",
        minF = 1f,
        maxF = 10f,
    )
    var espOutlineWidth = 2f

    @Property(
        type = PropertyType.SWITCH,
        name = "ESP Bats",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espBats = true

    @Property(
        type = PropertyType.SWITCH,
        name = "ESP Withers",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espWithers = true

    @Property(
        type = PropertyType.SWITCH,
        name = "ESP Fels",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espFels = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Shadow Assassins",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espShadowAssassin = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Minibosses",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espMiniboss = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Seperate Miniboss Colors",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espSeperateMinibossColor = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Starred Mobs",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var espStarMobs = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Remove Starred Nametags",
        category = "ESP",
        subcategory = "Dungeon ESP"
    )
    var removeStarMobsNametag = false

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
        name = "SB Kick Alert",
        description = "Sends a party message when you are kicked from SkyBlock",
        category = "Alerts",
        subcategory = "Dungeons"
    )
    var SBKick = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Shadow Assasian Alert",
        description = "Shows a notification on screen when an invinsable Shadow Assasian is about to teleport",
        category = "Alerts",
        subcategory = "Dungeons"
    )
    var ShadowAssassinAlert = false

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
    var FullThunderBottleAlert = true

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
        name = "Party Finder Alert",
        description = "Plays a sound when someone joins your party",
        category = "Alerts",
        subcategory = "Dungeons"
    )
    var PartyFinderSound = true

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
    var RNGSound = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Sold AH Notification",
        description = "Plays A sound when an item on your AH sold",
        category = "Alerts",
        subcategory = "General"
    )
    var SoldAHNotification = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Bonzo Mask Alert",
        description = "Shows on screen when the Bonzo Mask Ability has been used",
        category = "Alerts",
        subcategory = "Masks"
    )
    var BonzoMaskAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Spirit Mask Alert",
        description = "Shows on screen when the Spirit Mask Ability has been used",
        category = "Alerts",
        subcategory = "Masks"
    )
    var SpiritMaskAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Phoenix Pet Alert",
        description = "Shows on screen when the Phoenix Pet Ability has been used",
        category = "Alerts",
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
        category = "GUI",
        subcategory = "Tooltips"
    )
    var ScalableTooltips = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Tooltips Scale",
        description = "What should be the base scaling value of the tooltips",
        category = "GUI",
        subcategory = "Tooltips"
    )
    var ScalableTooltipsScale = 1f

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
        description = "Default #55FFFFAA.",
        category = "GUI",
        subcategory = "Inventory"
    )
    var overlayColorSalvageable = Color(92, 157, 255, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Top Quality Salvageable Items Color",
        description = "Default 6AFF6AAA.",
        category = "GUI",
        subcategory = "Inventory"
    )
    var overlayColorTopSalvageable = Color(255, 0, 0, 255)

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Leap Menu",
        description = "Renders a Custom Menu for leaps",
        category = "GUI",
        subcategory = "Menus"
    )
    var CustomLeapMenu = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Custom Leap Menu Light Mode",
        description = "Changes the Color Mode of the Custom Leap Menu",
        category = "GUI",
        subcategory = "Menus",
        hidden = true
    )
    var CustomLeapMenuLightMode = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom Leap Menu Scale",
        description = "Scale of the Custom Leap Menu",
        category = "GUI",
        subcategory = "Menus",
    )
    var CustomLeapMenuScale = 1f;

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom SB Menus",
        description = "Renders a Custom Menu for a lot of Menus in Skyblock",
        category = "GUI",
        subcategory = "Menus"
    )
    var CustomMenus = true

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom SB Menu Scale",
        description = "Scale of the Custom SB Menu",
        category = "GUI",
        subcategory = "Menus"
    )
    var CustomSBMenusScale = 1f


    @Property(
        type = PropertyType.CHECKBOX,
        name = "Game Menu",
        category = "GUI",
        subcategory = "Menus",
    )
    var CustomMenusGameMenu = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "SkyBlock Menu",
        category = "GUI",
        subcategory = "Menus",
    )
    var CustomMenusSkyBlockMenu = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Storage Menu",
        category = "GUI",
        subcategory = "Menus",
    )
    var CustomMenusStorageMenu = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Party Finder Menus",
        category = "GUI",
        subcategory = "Menus",
    )
    var CustomPartyFinderMenu = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Pet Menu",
        category = "GUI",
        subcategory = "Menus",
    )
    var CustomPetMenu = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Custom Wardrobe Menu",
        category = "GUI",
        subcategory = "Menus",
    )
    var CustomWardrobeMenu = false


    @Property(
        type = PropertyType.SWITCH,
        name = "Player HUD",
        description = "Global Switch for Player HUD\nDraws the hp, currentDefense, currentMana, and effective hp of the player on screen",
        category = "HUD",
        subcategory = "PlayerHUD"
    )
    var PlayerHUD = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Health Display",
        description = "Draws your players health in currentHealth/maxHealth format on screen",
        category = "HUD",
        subcategory = "PlayerHUD",
    )
    var PlayerHUDHealth = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Defense Display",
        description = "Draws your players currentDefense on screen",
        category = "HUD",
        subcategory = "PlayerHUD",
    )
    var PlayerHUDDefense = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Mana Display",
        description = "Draws your players currentMana in currentMana/maxMana format on screen",
        category = "HUD",
        subcategory = "PlayerHUD",
    )
    var PlayerHUDMana = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Overflow Mana Display",
        description = "Draws your players Overflow Mana on screen",
        category = "HUD",
        subcategory = "PlayerHUD",
    )
    var PlayerHUDOverflowMana = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Alternate Overflow Mana",
        description = "Draws your player Overflow Mana only if it is greater than 0",
        category = "HUD",
        subcategory = "PlayerHUD",
    )
    var PlayerHUDAlternateOverflowMana = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Effective HP Display",
        description = "Draws your players effective health on screen",
        category = "HUD",
        subcategory = "PlayerHUD",
    )
    var PlayerHUDEffectiveHP = true

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Speed Display",
        description = "Draws your players speed on screen",
        category = "HUD",
        subcategory = "PlayerHUD",
    )
    var PlayerHUDSpeed = true

    @Property(
        type = PropertyType.SWITCH,
        name = "FPS Display",
        description = "Displays the System Time on screen",
        category = "HUD",
        subcategory = "FPS"
    )
    var FpsDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Pet Display",
        description = "Displays your current active pet's name on screen",
        category = "HUD",
        subcategory = "PlayerHUD"
    )
    var PetDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "TPS Display",
        description = "Displays the TPS on screen",
        category = "HUD",
        subcategory = "Tps"
    )
    var TpsDisplay = false

    @Property(
        type = PropertyType.COLOR,
        name = "Tps Display Color",
        description = "The Color of the TPS Display",
        category = "HUD",
        subcategory = "Tps",
    )
    var TpsDisplayColor = Color(0, 114, 255)

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Scoreboard",
        description = "Renders Sexy Custom Dark Scoreboard",
        category = "HUD",
        subcategory = "ScoreBoard"
    )
    var CustomScoreboard = false

    @Property(
        type = PropertyType.COLOR,
        name = "FPS Display Color",
        description = "The Color of the FPS Display",
        category = "HUD",
        subcategory = "FPS",
        allowAlpha = false
    )
    var FpsDisplayColor = Color(255, 0, 255)

    @Property(
        type = PropertyType.SWITCH,
        name = "Clock Display",
        description = "Displays the System Time on screen",
        category = "HUD",
        subcategory = "Clock"
    )
    var ClockDisplay = false

    @Property(
        type = PropertyType.COLOR,
        name = "Clock Display Color",
        description = "The Color of the Clock Display",
        category = "HUD",
        subcategory = "Clock",
        allowAlpha = false
    )
    var ClockDisplayColor = Color(255, 116, 0)

    @Property(
        type = PropertyType.SWITCH,
        name = "Bonzo Mask Display",
        description = "Displays the Bonzo Mask Cooldown on screen",
        category = "HUD",
        subcategory = "Masks"
    )
    var BonzoMaskDisplay = false

    /* todo: Invornability Timers for Bonzo, Spirit and Phoenix


        @Property(
            type = PropertyType.SWITCH,
            name = "Bonzo Mask Invulnerability Display",
            description = "Displays the Bonzo Mask Invulnerability time on screen",
            category = "HUD",
            subcategory = "Bonzo Mask"
        )
        var BonzoMaskInvulnerabilityDisplay = false*/

    @Property(
        type = PropertyType.SWITCH,
        name = "Spirit Mask Display",
        description = "Displays the Spirit Mask Cooldown on screen",
        category = "HUD",
        subcategory = "Masks"
    )
    var SpiritMaskDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Phoenix Pet Display",
        description = "Displays the Phoenix Pet Cooldown on screen",
        category = "HUD",
        subcategory = "Masks"
    )
    var PhoenixPetDisplay = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Tab List",
        description = "Custom Tab List design",
        category = "HUD",
        subcategory = "TabList"
    )
    var CustomTabList = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom Tab List Scale",
        description = "The scale of the Custom Tab List",
        category = "HUD",
        subcategory = "TabList"
    )
    var CustomTabListScale = 1f

    @Property(
        type = PropertyType.SWITCH,
        name = "Block Overlay",
        category = "Misc",
        subcategory = "Block Overlay"
    )
    var BlockOverlay = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Block Overlay Type",
        description = "How to highlight the block",
        category = "Misc",
        subcategory = "Block Overlay",
        options = ["Outline", "Overlay", "Outlined Overlay"],
    )
    var BlockOverlayType = 0

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "Outline Thickness",
        description = "",
        category = "Misc",
        subcategory = "Block Overlay",
        minF = 1f,
        maxF = 10f
    )
    var BlockOverlayOutlineThickness = 5f

    @Property(
        type = PropertyType.COLOR,
        name = "Outline Color",
        description = "The color of the Outline",
        category = "Misc",
        subcategory = "Block Overlay"
    )
    var BlockOverlayOutlineColor = Color(0, 114, 255, 255)

    @Property(
        type = PropertyType.COLOR,
        name = "Overlay Color",
        description = "The color of the Overlay",
        category = "Misc",
        subcategory = "Block Overlay",
    )
    var BlockOverlayOverlayColor = Color(0, 114, 255, 75)

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Show Through Blocks?",
        description = "Whether to Enable or Disable Depth Checking.",
        category = "Misc",
        subcategory = "Block Overlay"
    )
    var BlockOverlayESP = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Player Scale",
        description = "Allows to dynamically adjust the size of the player character.\n\n" +
                "§dNow you can match your IRL Height ❤.",
        category = "Misc",
        subcategory = "Player"
    )
    var PlayerScale = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Player Scale On Everyone?",
        description = "Whether to make this feature work on everyone. or just yourself.",
        category = "Misc",
        subcategory = "Player"
    )
    var PlayerScaleOnEveryone = false

    @Property(
        type = PropertyType.PERCENT_SLIDER,
        name = "Custom Scale",
        description = "How much to scale the player character's scale from the default 100% down to 10%.",
        category = "Misc",
        subcategory = "Player"
    )
    var PlayerScaleValue = 1f

    @Property(
        type = PropertyType.SWITCH,
        name = "Player Spin",
        description = "A client-side feature that makes the player's avatar spin in place, visible only to the user and without affecting gameplay or other players.",
        category = "Misc",
        subcategory = "Player"
    )
    var PlayerSpin = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Spin direction",
        description = "",
        category = "Misc",
        subcategory = "Player",
        options = ["Right", "Left"]
    )
    var SpinDirection = 0;

    @Property(
        type = PropertyType.SLIDER,
        name = "Spin speed",
        description = "",
        category = "Misc",
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
        category = "Misc",
        subcategory = "Player"
    )
    var SpinOnEveryone = false;*/

    @Property(
        type = PropertyType.SWITCH,
        name = "Time Changer",
        description = "Allows to adjust the World's time.",
        category = "Misc",
        subcategory = "Time Changer"
    )
    var TimeChanger = false

    @Property(
        type = PropertyType.SELECTOR,
        name = "Time Changer Mode",
        description = "How to adjust the World's time.",
        category = "Misc",
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
        category = "Misc",
        subcategory = "Clear Sight"
    )
    var antiBlind = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Bonzo Boss Revived Alert",
        description = "Plays A sound when the Bonzo Boss is revived",
        category = "Alerts",
        subcategory = "Dungeons"
    )
    var bonzoBossRespawnAlert = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Clear Blocks",
        description = "When clipping into blocks it allows you to see around you instead of just blocking your view",
        category = "Misc",
        subcategory = "Clear Sight"
    )
    var clearBlocks = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dont push out of blocks",
        description = "Disables blocks pushing you out of them when clipping into them.",
        category = "Misc",
        subcategory = "Clear Sight"
    )
    var noPushOutOfBlocks = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Portal Effect",
        description = "Disables nether portal overlay.",
        category = "Misc",
        subcategory = "Clear Sight"
    )
    var antiPortal = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Rotate",
        description = "Disables rotations from server.\n\n&c&lUSE AT YOUR OWN RISK, COULD GET YOU TIMER BANNED",
        category = "Misc",
        subcategory = "Clear Sight"
    )
    var NoRotate = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "No Rotate Keep Motion",
        description = "Keeps your momentum when you are teleporting.",
        category = "Misc",
        subcategory = "Clear Sight"
    )
    var NoRotateKeepMotion = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Water FOV",
        description = "Disables FOV change in water.",
        category = "Misc",
        subcategory = "Clear Sight"
    )
    var antiWaterFOV = false

    @Property(
        type = PropertyType.SWITCH,
        name = "No Block Animation",
        description = "Disable block animation on all swords with right click ability.",
        category = "Misc",
        subcategory = "Clear Sight"
    )
    var noBlockAnimation = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Remove Selfie camera",
        description = "Removes selfie camera In F5.",
        category = "Misc",
        subcategory = "QOL"
    )
    var removeSelfieCamera = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Remove Selfie camera only with Hype",
        description = "Removes selfie camera In F5 only when you have a Wither Blade with Wither Impact.",
        category = "Misc",
        subcategory = "QOL"
    )
    var removeSelfieCameraOnlyWithHype = false

    @Property(
        type = PropertyType.SWITCH,
        name = "CustomFOV",
        description = "Allows to set a higher or lower FOV than Minecraft default.",
        category = "Misc",
        subcategory = "QOL"
    )
    var CustomFov = false

    @Property(
        type = PropertyType.DECIMAL_SLIDER,
        name = "FOV Value",
        description = "What to set your FOV to.",
        category = "Misc",
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
        category = "Misc",
        subcategory = "QOL"
    )
    var customDamageSplash = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Hide Falling Blocks",
        description = "Hides falling blocks, Good for fps.",
        category = "Misc",
        subcategory = "QOL"
    )
    var hideFallingBlocks = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Dungeon Chect Profit",
        description = "Shows the profit you get from Dungeon Chests.",
        category = "Dungeons",
        subcategory = "DungeonChectProfit"
    )
    var DungeonChectProfit = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Croesus Chests Profit",
        description = "Shows the profit you get for each chest type in Croesus's menu.",
        category = "Dungeons",
        subcategory = "DungeonChectProfit"
    )
    var CroesusChestsProfit = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Sort the chests by profit",
        description = "Sorts the chests in the profit overlay by profit.",
        category = "Dungeons",
        subcategory = "DungeonChectProfit"
    )
    var CroesusChestsProfitSortByProfit = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Croesus Chest Highlight",
        description = "Highlights the runs you did in Croesus's menu according to the chests state\n\n&aGreen:&r None of the chests has been opened.\n&eYellow:&r Has 1 opened chests and you can open another one.\n&cRed:&r All possible chests have been opened.",
        category = "Dungeons",
        subcategory = "DungeonChectProfit"
    )
    var CroesusChestHighlight = false

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Hide Red Highlighted Chests",
        description = "Instand of highlighting the runs that All possible chests have been opened, just hide them.",
        category = "Dungeons",
        subcategory = "DungeonChectProfit"
    )
    var CroesusChestHighlightHideRedChests = false

    @Property(
        type = PropertyType.SWITCH,
        name = "RNG Drop Announcer",
        description = "Sends in party chat the RNG drop you got with the profit you made",
        category = "Dungeons",
        subcategory = "DungeonChectProfit"
    )
    var RNGDropAnnouncer = false


    private const val DEV_MODE_DESCRIPTION =
        "§fForces all features to enable, even if you are not on skyblock.\n\n" +
                "§eenables console logging and disables a few safety checks\n" +
                "§bQ: Why is this a thing?\n" +
                "§aA: So I can properly test features in the mod without needing to be in skyblock\n\n" +
                "§4§n§lDONT USE IT IF U ARE NOT ME, CAN GET YOU BANNED!\n\n" +
                "§d[R.I.P] §bFININ1"

    @Property(
        type = PropertyType.CHECKBOX,
        name = "Dev Mode",
        description = DEV_MODE_DESCRIPTION,
        category = "Dev"
    )
    var DevMode = false

    @Property(
        type = PropertyType.SWITCH,
        name = "Custom Main Menu",
        description = "Enables the Custom Main Menu",
        category = "Dev",
        subcategory = "Experimental"
    )
    var CustomMainMenu = true

    @Property(
        type = PropertyType.SWITCH,
        name = "Motion Blur",
        description = "Enables motion blur",
        category = "Dev",
        subcategory = "Experimental"
    )
    var MotionBlur = false

    @Property(
        type = PropertyType.SLIDER,
        name = "Motion Blur Amount",
        description = "Amount of motion blur",
        category = "Dev",
        subcategory = "Experimental",
        min = 1,
        max = 10
    )
    var MotionBlurAmount = 5

    init {
        setCategoryDescription(
            "ESP",
            "Disable Optifine's fast render and Patcher's entity culling."
        )

        setCategoryDescription(
            "Dev",
            "&4A list of Broken/Unfinished features and UAYOR.".addColor()
        )

        // General
        addDependency("ChatCoordsWayPointColor", "ChatCoordsWayPoint")


        // Dungeons
        listOf(
            "BetterFloor7",
            "BetterFloor6",
            "BetterFloor5"
        ).forEach { addDependency(it, "BetterFloors") }
        listOf(
            "MimicEffi10",
            "GhostBlocks",
            "LegitGhostPick"
        ).forEach { addDependency(it, "GhostPick") }
        listOf(
            "P1StartTimer",
            "P2StartTimer",
            "P3StartTimer",
            "P4StartTimer"
        ).forEach { addDependency(it, "F7M7PhaseStartTimers") }

        listOf(
            "BlazeSolverFirstBlazeColor",
            "BlazeSolverSecondBlazeColor",
            "BlazeSolverThirdBlazeColor",
            "BlazeSolverLineColor"
        ).forEach { addDependency(it, "BlazeSolver") }

        addDependency("AutoPotionCommand", "AutoPotion")
        //  addDependency("AutoReaperArmorSlot", "AutoReaperArmorSwap")
        addDependency("CroesusChestsProfitSortByProfit", "CroesusChestsProfit")
        addDependency("CroesusChestHighlightHideRedChests", "CroesusChestHighlight")

        // Terminals
        listOf(
            "CustomTerminalMenuClickMode",
            "CustomTerminalMenuScale",
            "CustomTerminalMenuLightMode",
            "CustomTerminalMenuSolutionColor",
            "CustomMelodyTerminal",
            "CustomNumbersTerminal",
            "CustomRubixTerminal",
            "CustomRedGreenTerminal",
            "CustomStartWithTerminal",
            "CustomColorsTerminal",
        ).forEach { addDependency(it, "CustomTerminalsGui") }


        // Miscs
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
        addDependency("removeSelfieCameraOnlyWithHype", "removeSelfieCamera")
        listOf(
            "SpinDirection",
            "SpinSpeed"
        ).forEach { addDependency(it, "PlayerSpin") }
        addDependency("NoRotateKeepMotion", "NoRotate")


        // ESP
        listOf(
            "espColorUnstable",
            "espColorYoung",
            "espColorSuperior",
            "espColorHoly",
            "espColorFrozen",
            "espColorAngryArchaeologist"
        ).forEach { addDependency(it, "espSeperateMinibossColor") }
        addDependency("espColorMiniboss", "espMiniboss")
        listOf(
            "espColorStarMobs",
            "removeStarMobsNametag"
        ).forEach { addDependency(it, "espStarMobs") }
        addDependency("hideWrongLivids", "lividFinder")
        addDependency("espColorBats", "espBats")
        addDependency("espColorFels", "espFels")
        addDependency("espColorShadowAssassin", "espShadowAssassin")
        addDependency("removeStarMobsNametag", "espStarMobs")


        // GUI
        listOf(
            "overlayColorSalvageable",
            "overlayColorTopSalvageable"
        ).forEach { addDependency(it, "overlaySalvageable") }
        listOf(
            "CustomLeapMenuScale",
            "CustomLeapMenuLightMode"
        ).forEach { addDependency(it, "CustomLeapMenu") }
        addDependency("ScalableTooltipsScale", "ScalableTooltips")
        addDependency("SlotBindingShowBinding", "SlotBinding")
        listOf(
            "CustomMenusGameMenu",
            "CustomMenusSkyBlockMenu",
            "CustomMenusStorageMenu",
            "CustomPartyFinderMenu",
            "CustomPetMenu",
            "CustomWardrobeMenu"
        ).forEach { addDependency(it, "CustomMenus") }


        // Hud
        addDependency("FpsDisplayColor", "FpsDisplay")
        addDependency("ClockDisplayColor", "ClockDisplay")
        listOf(
            "PlayerHUDSpeed",
            "PlayerHUDEffectiveHP",
            "PlayerHUDOverflowMana",
            "PlayerHUDAlternateOverflowMana",
            "PlayerHUDMana",
            "PlayerHUDDefense",
            "PlayerHUDHealth",
        ).forEach { addDependency(it, "PlayerHUD") }

    }

    fun init() {
        initialize()
    }

    private object Sorting: SortingBehavior() {
        override fun getCategoryComparator(): Comparator<in Category> = Comparator.comparingInt { c: Category ->
            listOf(
                "General",
                "Dungeons",
                "Terminals",
                "ESP",
                "Alerts",
                "GUI",
                "HUD",
                "Misc",
                "Dev"
            ).indexOf(c.name)
        }
    }
}
