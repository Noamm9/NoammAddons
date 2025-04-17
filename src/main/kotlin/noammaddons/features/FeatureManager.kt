package noammaddons.features


import net.minecraftforge.common.MinecraftForge
import noammaddons.features.alerts.*
import noammaddons.features.dungeons.*
import noammaddons.features.dungeons.dmap.DungeonMap
import noammaddons.features.dungeons.esp.*
import noammaddons.features.dungeons.solvers.*
import noammaddons.features.dungeons.solvers.devices.*
import noammaddons.features.dungeons.solvers.puzzles.*
import noammaddons.features.dungeons.solvers.terminals.*
import noammaddons.features.general.*
import noammaddons.features.general.teleport.*
import noammaddons.features.gui.*
import noammaddons.features.gui.Menus.impl.*
import noammaddons.features.hud.*
import noammaddons.features.misc.*
import noammaddons.features.slayers.ExtraSlayerInfo
import noammaddons.noammaddons.Companion.Logger

object FeatureManager {
    val features = mutableSetOf(
        MotionBlur, LeftClickEtherwarp, ChatCoordsWaypoint, GyroCircle, ChatEmojis,
        SlotBinding, PartyESP, SBKickDuration, CakeNumbers, EnderPearlFix, WardrobeKeybinds,
        PartyCommands, ShowItemEntityName, BlockGloomlockOverUse, VisualWords,
        RemoveUselessMessages, ShowItemRarity, SkyBlockExpInChat, ShowItemsPrice,
        TeammatesESP, AbilityKeybinds, IHATEDIORITE, AutoCloseChest, StonkSwapSound,
        MimicDetector, GhostPick, ShowExtraStats, HighlightDoorKeys, AutoUlt, AutoRefillEnderPearls,
        BetterPartyFinderMessages, F7PhaseStartTimers, AnnounceSpiritLeaps, AutoReaperArmorSwap,
        AutoI4, AnnounceDraftResets, BetterFloors, BloodDialogueSkip, AutoPotion, CryptsDoneAlert,
        M7Relics, HidePlayersAfterLeap, M7Dragons, CreeperBeamSolver, BlazeSolver, CrystalPlaceTimer,
        EtherwarpSound, DungeonChestProfit, BatDeadTitle, DungeonAbilityCooldowns, Floor4BossFight,
        CrystalSpawnTimer, ArrowAlignSolver, DungeonFpsBoost, ThreeWeirdosSolver, DungeonMap,
        SimonSaysSolver, DungeonWarpCooldown, AutoRequeue, BoulderSolver, DungeonSecrets, Melody,
        Numbers, Rubix, RedGreen, Colors, StartWith, MelodyAlert, TerminalNumbers, BetterF7Titles,
        HiddenMobs, StarMobESP, LividSolver, WitherESP, PestESP, BloodReady, EnergyCrystal, ThunderBottle,
        M7P5RagAxe, RNGSound, AHSoldNotification, ShadowAssassinAlert, SkyblockKick, PartyFinderSound,
        RoomAlerts, ExtraSlayerInfo, SalvageOverlay, CustomPartyFinderMenu, CustomSpiritLeapMenu,
        CustomWardrobeMenu, CustomPetMenu, StopCloseMyChat, ProfileViewer, ScalableTooltips, FpsDisplay,
        ClockDisplay, MaskTimers, TpsDisplay, WitherShieldTimer, PlayerHud, SpringBootsDisplay,
        CustomScoreboard, SecretDisplay, PetDisplay, CustomTabList, BlockOverlay, PlayerScale,
        PlayerSpin, HideFallingBlocks, DamageSplash, RemoveSelfieCam, CustomFov, AntiBlind,
        AntiPortal, NoBlockAnimation, NoWaterFOV, CustomBowHitSound, ClearBlocks, NoRotate,
        RatProtection, SmoothSneaking, SmoothBossBar, ZeroPingTeleportation, TeleportOverlay,
        DungeonPlayerDeathAlert, DungeonRunSplits
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