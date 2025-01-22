package noammaddons.features

import net.minecraftforge.common.MinecraftForge
import noammaddons.features.alerts.*
import noammaddons.features.dungeons.*
import noammaddons.features.dungeons.ESP.*
import noammaddons.features.dungeons.terminals.*
import noammaddons.features.general.*
import noammaddons.features.gui.Menus.CustomMenuRenderer
import noammaddons.features.gui.Menus.impl.*
import noammaddons.features.gui.ProfleViewer
import noammaddons.features.gui.SalvageOverlay
import noammaddons.features.gui.StopCloseMyChat
import noammaddons.features.hud.*
import noammaddons.features.misc.*
import noammaddons.features.slayers.ExtraSlayerInfo
import noammaddons.noammaddons.Companion.Logger

object FeatureManager {
    val features = setOf(

        // General
        Motionblur, LeftClickEtherwarp, ChatCoordsWaypoint,
        GyroCircle, ChatEmojis, SlotBinding, PartyOutline,
        PartyNames, SBKickDuration, CakeNumbers, EnderPearlFix,
        /*CustomMainMenu @see MixinGuiMainMenu & MixinGuiIngameMenu*/
        /*VisualWords, @see MixinFontRenderer*/
        /*CustomItemEntity, @see MixinRenderEntityItem */
        RemoveUselessMessages, // @see MixinGuiNewChat
        PartyCommands, ShowItemEntityName, BlockGloomlockOverUse,
        /*CustomSlotHighlight, @see MixinGuiContainer*/
        /*ShowItemRarity, @see MixinRenderItem*/

        // Dungeons
        TeammatesNames, TeammatesESP, AbilityKeybinds,
        IHATEDIORITE, AutoCloseChest, HighlightMimicChest,
        GhostPick, ShowExtraStats, TraceKeys, AutoUlt,
        AutoRefillEnderPearls, AutoI4, F7PhaseStartTimers,
        AnnounceSpiritLeaps, AnnounceDraftResets, BetterFloors,
        BloodDialogueSkip, AutoPotion, AutoReaperArmorSwap,
        HidePlayersAfterLeap, M7RelicOutline, M7RelicSpawnTimer,
        M7Dragons, BlazeSolver, EtherwarpSound, DungeonChestProfit,
        Floor4BossFight, // BoulderSolver, AutoIceFill || not soon


        // Terminals
        Melody, Numbers, Rubix,
        RedGreen, Colors, StartWith,
        MelodyAlert, TerminalNumbers,
        BetterF7Titles,


        // ESP
        HiddenMobs, MobESP,
        LividESP, WitherESP,


        // Alerts
        BloodReady, EnergyCrystal,
        ThunderBottle, M7P5RagAxe, RNGSound,
        AHSoldNotification, ShadowAssassinAlert,
        SkyblockKick, PartyFinderSound,


        // Slayers
        ExtraSlayerInfo, // Todo: make a settings toggle lmao

        // GUI
        SalvageOverlay, CustomPartyFinderMenu,
        CustomSpiritLeapMenu, CustomMenuRenderer,
        CustomWardrobeMenu, CustomPetMenu,
        StopCloseMyChat, ProfleViewer,
        /*ScalableTooltips - @see MixinGuiUtils*/


        // HUD
        FpsDisplay, ClockDisplay, BonzoMask,
        SpiritMask, PhoenixPet, TpsDisplay,
        WitherShieldTimer, SpringBootsDisplay,
        CustomScoreboard, PlayerHud,
        PetDisplay, CustomTabList,


        // Misc
        BlockOverlay, PlayerScale, PlayerSpin,
        /*TimeChanger - @see MixinWorldProvider*/
        /*NoPushOutOfBlocks - @see MixinEntityPlayerSP*/
        /*SmoothSneaking - @see MixinEntityPlayer*/
        HideFallingBlocks, DamageSplash,
        RemoveSelfieCam, CustomFov, AntiBlind,
        AntiPortal, NoBlockAnimation, NoWaterFOV,
        CustomBowHitSound, ClearBlocks, NoRotate
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