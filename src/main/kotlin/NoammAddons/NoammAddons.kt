package NoammAddons

import NoammAddons.commands.NoammAddonsCommands
import NoammAddons.commands.SkyBlockCommands.*
import NoammAddons.config.*
import NoammAddons.events.RegisterEvents
import NoammAddons.features.General.*
import NoammAddons.features.alerts.*
import NoammAddons.features.cosmetics.*
import NoammAddons.features.dungeons.*
import NoammAddons.features.dungeons.ESP.*
import NoammAddons.features.dungeons.terminals.*
import NoammAddons.features.gui.*
import NoammAddons.features.hud.*
import NoammAddons.features.*
import NoammAddons.utils.*
import net.minecraft.client.Minecraft
import net.minecraftforge.client.ClientCommandHandler
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
	clientSideOnly = true,
)

class NoammAddons {
    @Mod.EventHandler
    @Suppress("UNUSED_PARAMETER", "unused")
    fun OnInit(event: FMLInitializationEvent) {
        config.init()
        MinecraftForge.EVENT_BUS.register(EventHandlers)
	    MinecraftForge.EVENT_BUS.register(RegisterEvents)

        listOf(
	        NoammAddonsCommands(),
	        DungeonHub(),
	        CrimonIsle(),
	        End(),
	        hub(),
	        Skyblock()
        ).forEach{ClientCommandHandler.instance.registerCommand(it)}

	    
        KeyBinds.allBindings.forEach { ClientRegistry.registerKeyBinding(it) }


        listOf(
	        this,

            // General
	        LeftClickEtherwarp,
	        CustomItemEntity,
//          EnderPearlFix,            WHY DID THEY HAVE TO PATCH THIS :(
	        ChatCoordsWaypoint,
	        GyroCircle,
	        ChatEmojis,
	        SlotBinding,
	        PartyOutline,
//          VisualWords, @see MixinFontRenderer
	        SBKickDuration,
	        RemoveUselessMessages,

            // Dungeons
	        TeammatesNames,
	        TeammatesOutline,
	        AbilityKeybinds,
	        IHATEDIORITE,
	        AutoCloseChest,
	        HighlightMimicChest,
	        GhostPick,
	        ShowExtraStats,
	        TraceKeys,
	        AutoUlt,
	        AutoRefillEnderPearls,
	        AutoI4,
	        F7PhaseStartTimers,
	        AnnounceSpiritLeaps,
	        AnnounceDraftResets,
	        BetterFloors,
	        BloodDialogueSkip,
	        AutoPotion,
	        AutoReaperArmorSwap,


            // Terminals
	        Melody,
	        Numbers,
	        Rubix,
	        RedGreen,
	        Colors,
	        StartWith,
	        MelodyAlert,
	        TerminalNumbers,
	        BetterF7TerminalsTitles,


            // ESP
	        HiddenMobs,
	        MobESP,
	        LividESP,


            // Alerts
	        BloodReady,
	        EnergyCrystal,
	        ThunderBottle,
	        M7P5RagAxe,
	        RNGSound,
	        AHSoldNotification,
	        ShadowAssassinAlert,


            // GUI
	        SalvageOverlay,
	        PartyFinderOverlay,
//          ScalableTooltips - @see MixinGuiUtils
	        CustomSpiritLeapMenu,


            // HUD
	        FpsDisplay,
	        ClockDisplay,
	        BonzoMask,
	        SpiritMask,
	        PhoenixPet,
	        WitherShieldTimer, // Todo: add more sounds
	        SpringBootsDisplay,
	        CustomScoreboard,
	        PlayerHud,
	        PetDisplay,


            // Cosmetics
	        BlockOverlay,
	        PlayerScale,
	        PlayerSpin,
	        TimeChanger,
	        HideFallingBlocks,
	        DamageSplash,
	        RemoveSelfieCam,
	        CustomFov,
	        AntiBlind,
	        AntiPortal,
	        NoBlockAnimation,
	        NoWaterFOV,
	        CustomBowHitSound, // Todo: add more sounds

            // Utilities
	        GuiUtils,
	        LocationUtils,
	        DungeonUtils,
	        ActionBarParser,
	        PartyUtils
        ).forEach(MinecraftForge.EVENT_BUS::register)
    }


    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (KeyBinds.Config.isPressed) {
            GuiUtils.openScreen(config.gui())
        }
    }
	
	
    companion object {
        const val MOD_NAME = "NoammAddons"
        const val MOD_ID = "noammaddons"
        const val MOD_VERSION = "1.9.6"
        const val CHAT_PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
        const val FULL_PREFIX = "§d§l§nNoamm§b§l§nAddons"
        const val DEBUG_PREFIX = "§8[§b§lN§d§lA §7DEBUG§8]"

        val mc: Minecraft = Minecraft.getMinecraft()
        val config = Config
        val hudData = PogObject("hudData", HudElementConfig())
    }
}
//    TODO
//     - Vanquisher esp + tracer
