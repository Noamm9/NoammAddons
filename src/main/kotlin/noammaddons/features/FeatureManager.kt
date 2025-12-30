package noammaddons.features


import noammaddons.features.impl.CompTest
import noammaddons.features.impl.DevOptions
import noammaddons.features.impl.alerts.*
import noammaddons.features.impl.dungeons.*
import noammaddons.features.impl.dungeons.dmap.DungeonMap
import noammaddons.features.impl.dungeons.dragons.WitherDragons
import noammaddons.features.impl.dungeons.solvers.LividSolver
import noammaddons.features.impl.dungeons.solvers.devices.*
import noammaddons.features.impl.dungeons.solvers.puzzles.PuzzleSolvers
import noammaddons.features.impl.dungeons.solvers.terminals.TerminalSolver
import noammaddons.features.impl.dungeons.waypoints.DungeonWaypoints
import noammaddons.features.impl.esp.*
import noammaddons.features.impl.general.*
import noammaddons.features.impl.general.teleport.Etherwarp
import noammaddons.features.impl.general.teleport.TeleportOverlay
import noammaddons.features.impl.gui.*
import noammaddons.features.impl.gui.Menus.impl.CustomPetMenu
import noammaddons.features.impl.gui.Menus.impl.WardrobeMenu
import noammaddons.features.impl.hud.*
import noammaddons.features.impl.misc.*
import noammaddons.features.impl.slayers.ExtraSlayerInfo
import noammaddons.features.impl.slayers.SlayerFeatures
import noammaddons.ui.config.core.save.Config
import noammaddons.utils.RenderHelper.getStringWidth

object FeatureManager {
    val features = mutableSetOf(
        EspSettings, CustomMenuSettings, MotionBlur, GyroHelper, ChatEmojis,
        Etherwarp, SlotBinding, PartyESP, SBKick, CakeNumbers, EnderPearlFix,
        ItemEntity, Gloomlock, VisualWords, Chat, ItemRarity, ItemsPrice,
        TeammatesESP, AbilityKeybinds, IHateDiorite, MimicDetector, DoorKeys,
        AutoUlt, PartyFinder, TickTimers, LeapMenu, AutoI4, ArchitectDraft, AutoGFS,
        AutoPotion, CryptsDone, M7Relics, MaxorsCrystals, ChestProfit, FpsBoost,
        Camera, PlayerModel, DamageSplash, AutoRequeue, WitherDragons, DungeonPlayerDeath,
        WarpCooldown, RoomAlerts, RunSplits, RatProtection, PuzzleSolvers, LividSolver,
        SimonSaysSolver, ArrowAlignSolver, Floor4BossFight, WitherESP, StarMobESP,
        HiddenMobs, PestESP, Secrets, SmoothBossBar, RNGSound, BloodRoom,
        ExtraSlayerInfo, SalvageOverlay, Ragnarock, ShadowAssassin, MelodyAlert,
        PlayerHud, FullBlock, TerminalNumbers, F7Titles, ScalableTooltips, FpsDisplay,
        ClockDisplay, TpsDisplay, PetDisplay, MaskTimers, SpringBootsDisplay,
        WitherShieldTimer, BowHitSound, TeleportOverlay, CustomScoreboard,
        ScoreCalculator, BlockOverlay, WardrobeMenu, CustomPetMenu, TimeChanger,
        CustomTabList, TerminalSolver, ChamNametags, CustomSlotHighlight,
        InventoryDisplay, StopCloseMyChat, InventorySearchbar, BlessingDisplay,
        ClientTimer, DarkMode, Animations, EnchantmentsColors, AutoClicker,
        ArrowFix, PartyHelper, DungeonMap, DungeonBreaker, DungeonWaypoints,
        NoBlockAnimation, SlayerFeatures, LifelineHud, FreezeDisplay,

        // ProfileViewer, // never finished + Skycrpt api went down
        // ZeroPingTeleportation, // - R.I.P fuck hypixel, fuck blinkers
        // BetterFloors, StonkSwap, GhostPick, // - R.I.P goodbye stonking
        // ReaperArmor, // - R.I.P fuck reaper fix
        // NoRotate, // - R.I.P Prediction AC

        DevOptions, CompTest, ConfigGui,
    ).sortedBy { it.name }


    fun createFeatureList(): String {
        val featureList = StringBuilder()
        for ((category, features) in features.groupBy { it.category }.entries) {
            featureList.appendLine("Category: ${category.catName}")
            for (feature in features.sortedByDescending { getStringWidth(it.name) }) {
                featureList.appendLine("- ${feature.name}: ${feature.desc}")
            }
            featureList.appendLine()
        }
        return featureList.toString()
    }

    fun registerFeatures() {
        features.forEach(Feature::_init)
        Config.load()
    }

    fun getFeatureByName(name: String) = features.find {
        it.name == name
    }
}