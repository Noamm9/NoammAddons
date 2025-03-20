package noammaddons.features

import net.minecraftforge.common.MinecraftForge
import noammaddons.features.alerts.*
import noammaddons.features.dungeons.*
import noammaddons.features.dungeons.dmap.DungeonMap
import noammaddons.features.dungeons.esp.*
import noammaddons.features.dungeons.solvers.*
import noammaddons.features.dungeons.terminals.*
import noammaddons.features.general.*
import noammaddons.features.general.PestESP.PestESP
import noammaddons.features.gui.*
import noammaddons.features.gui.Menus.impl.*
import noammaddons.features.hud.*
import noammaddons.features.misc.*
import noammaddons.features.slayers.ExtraSlayerInfo
import noammaddons.noammaddons.Companion.Logger

object FeatureManager {
    val features = mutableSetOf(

        // General
        MotionBlur, LeftClickEtherwarp, ChatCoordsWaypoint,
        GyroCircle, ChatEmojis, SlotBinding, PartyOutline,
        PartyNames, SBKickDuration, CakeNumbers, EnderPearlFix,
        PartyCommands, ShowItemEntityName, BlockGloomlockOverUse,
        VisualWords, RemoveUselessMessages, ShowItemRarity,
        SkyBlockExpInChat, ShowItemsPrice,

        // Dungeons
        TeammatesNames, TeammatesESP, AbilityKeybinds,
        IHATEDIORITE, AutoCloseChest, MimicDetector,
        GhostPick, ShowExtraStats, HighlightDoorKeys,
        AutoUlt, AutoRefillEnderPearls, BetterPartyFinderMessages,
        F7PhaseStartTimers, AnnounceSpiritLeaps, AutoI4,
        AnnounceDraftResets, BetterFloors, BloodDialogueSkip,
        AutoPotion, AutoReaperArmorSwap, M7Relics,
        HidePlayersAfterLeap, M7Dragons, CreeperBeamSolver,
        BlazeSolver, CrystalPlaceTimer, EtherwarpSound,
        DungeonChestProfit, BatDeadTitle, DungeonAbilityCooldowns,
        Floor4BossFight, CrystalSpawnTimer, ArrowAlignSolver,
        DungeonFpsBoost, ThreeWeirdosSolver, DungeonMap,
        SimonSaysSolver, DungeonWarpCooldown, AutoRequeue,
        BoulderSolver, DungeonSecrets,


        // Terminals
        Melody, Numbers, Rubix,
        RedGreen, Colors, StartWith,
        MelodyAlert, TerminalNumbers,
        BetterF7Titles,


        // ESP
        HiddenMobs, StarMobESP,
        LividSolver, WitherESP,
        PestESP,


        // Alerts
        BloodReady, EnergyCrystal,
        ThunderBottle, M7P5RagAxe, RNGSound,
        AHSoldNotification, ShadowAssassinAlert,
        SkyblockKick, PartyFinderSound,
        RoomAlerts,


        // Slayers
        ExtraSlayerInfo,


        // GUI
        SalvageOverlay, CustomPartyFinderMenu,
        CustomSpiritLeapMenu, CustomWardrobeMenu,
        CustomPetMenu, StopCloseMyChat,
        ProfleViewer, ScalableTooltips,


        // HUD
        FpsDisplay, ClockDisplay, BonzoMask,
        SpiritMask, PhoenixPet, TpsDisplay,
        WitherShieldTimer, SpringBootsDisplay,
        CustomScoreboard, PlayerHud, SecretDisplay,
        PetDisplay, CustomTabList,


        // Misc
        BlockOverlay, PlayerScale, PlayerSpin,
        HideFallingBlocks, DamageSplash,
        RemoveSelfieCam, CustomFov, AntiBlind,
        AntiPortal, NoBlockAnimation, NoWaterFOV,
        CustomBowHitSound, ClearBlocks, NoRotate,
        RatProtection, SmoothSneaking, SmoothBossBar
    )


    fun createFeatureList(): String {
        return features.joinToString("\n") { getFeatureName(it) }
    }

    fun getFeatureName(feature: Feature): String = feature::class.simpleName ?: "Unknown"

    fun registerFeatures(log: Boolean = true) {
        features.forEach {
            try {
                if (log) Logger.info("Registering feature: ${getFeatureName(it)}")
                MinecraftForge.EVENT_BUS.register(it)
            }
            catch (e: Exception) {
                if (log) Logger.error("Failed to register feature: ${getFeatureName(it)}", e)
            }
        }
    }
}