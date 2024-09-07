package NoammAddons


import NoammAddons.commands.NoammAddonsCommands
import NoammAddons.commands.SkyBlockCommands.*
import NoammAddons.config.Config
import NoammAddons.config.HudElementConfig
import NoammAddons.config.KeyBinds
import NoammAddons.config.KeyBinds.allBindings
import NoammAddons.config.PogObject
import NoammAddons.features.General.*
import NoammAddons.features.alerts.*
import NoammAddons.features.cosmetics.*
import NoammAddons.features.dungeons.*
import NoammAddons.features.dungeons.terminals.*
import NoammAddons.features.gui.SalvageOverlay
import NoammAddons.features.hud.ClockDisplay
import NoammAddons.features.hud.FpsDisplay
import NoammAddons.features.hud.PhoenixPet
import NoammAddons.features.hud.SpiritMask
import NoammAddons.utils.DungeonUtils
import NoammAddons.utils.GuiUtils
import NoammAddons.utils.GuiUtils.openScreen
import NoammAddons.utils.LocationUtils
import NoammAddons.utils.RenderUtils.drawPlayerHead
import net.minecraft.client.Minecraft
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent


@Mod(
    modid = NoammAddons.MOD_ID,
    name = NoammAddons.MOD_NAME,
    version = NoammAddons.MOD_VERSION,
    clientSideOnly = true
)

class NoammAddons {
    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {

        // Registering config
        config.init()
        MinecraftForge.EVENT_BUS.register(PogObject.EventHandlers)


        // Registering all Commands
        listOf(
            NoammAddonsCommands(),
            DungeonHub(),
            CrimonIsle(),
            End(),
            hub(),
            Skyblock()
        ).forEach{ClientCommandHandler.instance.registerCommand(it)}


        // Registering all Keybinds
        allBindings.forEach { ClientRegistry.registerKeyBinding(it) }

        // Registering all features
        listOf(
            this,

            // General
    //      EnderPearlFix,   WHY DID THEY HAVE TO PATCH THIS :(
            LeftClickEtherwarp,
            CustomItemEntity,
            ChatCoordsWaypoint,
            GyroCircle,
            ChatEmojis,

            // Dungeons
            TeammatesNames,
            TeammatesOutline,
            AbilityKeybinds,
            IHATEDIORITE,
            AutoCloseChest,
            F7PreGhostBlocks,
            HighlightMimicChest,
            GhostPick,
            HiddenMobs,
            ShowExtraStats,
            TraceKeys,
            AutoUlt,
            AutoRefillEnderPearls,
            AutoI4,
            F7PhaseStartTimers,
            AnnounceSpiritLeaps,
            AnnounceDraftResets,


            // Terminals
            Melody,
            Numbers,
            Rubix,
            RedGreen,
            Colors,
            StartWith,
            MelodyAlert,
            TerminalNumbers,
            BetterF7TerminalsTitles, // Todo: test it


            // ESP
            MobESP,
            LividESP,


            // Alerts
            BloodReady,
            EnergyCrystal,
            ThunderBottle,
            M7P5RagAxe,
            RNGSound,
            AHSoldNotification,
            ShadowAssasianAlert,


            // GUI
            SalvageOverlay,
            PartyFinderOverlay,
//          ScaleableTooltips - @see MixinGuiUtils
            CustomSpiritLeapMenu, // Todo: fix bug with head drawing


            // HUD
            FpsDisplay,
            ClockDisplay,
            BonzoMask,
            SpiritMask,
            PhoenixPet,
            WitherShieldTimer, // Todo: add more sounds
            SpringBootsDisplay, // Todo: Test it


            // Cosmetics
            BlockOverlay,
            PlayerScale,
            PlayerSpin,
            TimeChanger,
            HideFallingBlocks,
            DamageSplash,
            RemoveSellfieCam,
            CustomFov,
            AntiBlind,
            AntiPortal,
            NoBlockAnimation,
            NoWaterFOV,

            // Utilities
            GuiUtils,
            LocationUtils,
            DungeonUtils

        ).forEach(MinecraftForge.EVENT_BUS::register)
    }


    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (KeyBinds.Config.isPressed) {
            mc.openScreen(config.gui())
        }
    }


    companion object {
        const val MOD_NAME = "NoammAddons"
        const val MOD_ID = "noammaddons"
        const val MOD_VERSION = "1.1.1"
        const val CHAT_PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
        const val FULL_PREFIX = "§d§l§nNoamm§b§l§nAddons"

        val mc: Minecraft = Minecraft.getMinecraft()
        val config = Config
        val hudData = PogObject("hudData", HudElementConfig())
    }


//    TODO
//     - Add more features
//     https://github.com/Noamm9/OdinClient/blob/main/odinmain/src/main/kotlin/me/odinmain/utils/skyblock/SkyblockPlayer.kt
}