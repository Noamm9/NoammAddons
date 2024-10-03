package noammaddons

import net.minecraft.client.Minecraft
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noammaddons.commands.NoammAddonsCommands
import noammaddons.commands.SkyBlockCommands.*
import noammaddons.config.*
import noammaddons.events.RegisterEvents
import noammaddons.features.General.*
import noammaddons.features.alerts.*
import noammaddons.features.cosmetics.*
import noammaddons.features.dungeons.*
import noammaddons.features.dungeons.ESP.HiddenMobs
import noammaddons.features.dungeons.ESP.LividESP
import noammaddons.features.dungeons.ESP.MobESP
import noammaddons.features.dungeons.terminals.*
import noammaddons.features.gui.CustomSpiritLeapMenu
import noammaddons.features.gui.PartyFinderOverlay
import noammaddons.features.gui.SalvageOverlay
import noammaddons.features.hud.*
import noammaddons.utils.*


@Mod(
	modid = noammaddons.MOD_ID,
	name = noammaddons.MOD_NAME,
	version = noammaddons.MOD_VERSION,
	clientSideOnly = true
)

class noammaddons {
    @Mod.EventHandler
    @Suppress("unused", "UNUSED_PARAMETER")
    fun onInit(event: FMLInitializationEvent) {
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
        ).forEach(ClientCommandHandler.instance::registerCommand)
	    
	    
        KeyBinds.allBindings.forEach(ClientRegistry::registerKeyBinding)


        listOf(
	        this,
	        TestGround,

            // General
	        Motionblur,
	        LeftClickEtherwarp,
	        CustomItemEntity,
//	        EnderPearlFix,
	        ChatCoordsWaypoint,
	        GyroCircle,
	        ChatEmojis,
	        SlotBinding,
	        PartyOutline,
//          VisualWords, @see MixinFontRenderer
	        SBKickDuration,
//	        RemoveUselessMessages, @see MixinGuiNewChat
// 	        CustomMainMenu @see MixinGuiMainMenu & MixinGuiIngameMenu

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
	        HidePlayersAfterLeap,
	        M7RelicOutline,
	        M7Dragons,
	        M7RelicSpawnTimer,


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
        const val MOD_VERSION = "2.5.1"
        const val CHAT_PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
        const val FULL_PREFIX = "§d§l§nNo§lamm§b§l§nAddons"
        const val DEBUG_PREFIX = "§8[§b§lN§d§lA §7DEBUG§8]"
	    
        val mc: Minecraft = Minecraft.getMinecraft()
        val config = Config
        val hudData = PogObject("hudData", HudElementConfig())
    }
}
//    TODO
//     - Vanquisher esp + tracer