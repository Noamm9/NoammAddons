package noammaddons.features

import kotlinx.coroutines.DelicateCoroutinesApi
import noammaddons.features.alerts.*
import noammaddons.features.dungeons.*
import noammaddons.features.dungeons.ESP.*
import noammaddons.features.dungeons.terminals.*
import noammaddons.features.general.*
import noammaddons.features.gui.CustomSpiritLeapMenu
import noammaddons.features.gui.Menus.CustomMenuRenderer
import noammaddons.features.gui.Menus.impl.CustomPartyFinderMenu
import noammaddons.features.gui.Menus.impl.CustomPetMenu
import noammaddons.features.gui.Menus.impl.CustomWardrobeMenu
import noammaddons.features.gui.SalvageOverlay
import noammaddons.features.hud.*
import noammaddons.features.misc.*

object FeatureManager {
    @OptIn(DelicateCoroutinesApi::class)
    val features = setOf(

        // General
        Motionblur, LeftClickEtherwarp, ChatCoordsWaypoint,
        GyroCircle, ChatEmojis, SlotBinding, PartyOutline,
        PartyNames, SBKickDuration, CakeNumbers, EnderPearlFix,
        /*CustomMainMenu @see MixinGuiMainMenu & MixinGuiIngameMenu*/
        /*VisualWords, @see MixinFontRenderer*/
        /*CustomItemEntity, @see MixinRenderEntityItem */
        RemoveUselessMessages, // @see MixinGuiNewChat
        PartyCommands, ShowItemEntityName,


        // Dungeons
        TeammatesNames, TeammatesESP, AbilityKeybinds,
        IHATEDIORITE, AutoCloseChest, HighlightMimicChest,
        GhostPick, ShowExtraStats, TraceKeys, AutoUlt,
        AutoRefillEnderPearls, AutoI4, F7PhaseStartTimers,
        AnnounceSpiritLeaps, AnnounceDraftResets, BetterFloors,
        BloodDialogueSkip, AutoPotion, AutoReaperArmorSwap,
        HidePlayersAfterLeap, M7RelicOutline, M7RelicSpawnTimer,
        M7Dragons, BlazeSolver,
        // BoulderSolver, AutoIceFill,


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


        // GUI
        SalvageOverlay, CustomPartyFinderMenu,
        CustomSpiritLeapMenu, CustomMenuRenderer,
        CustomWardrobeMenu, CustomPetMenu,
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
        HideFallingBlocks, DamageSplash,
        RemoveSelfieCam, CustomFov, AntiBlind,
        AntiPortal, NoBlockAnimation, NoWaterFOV,
        CustomBowHitSound, ClearBlocks, NoRotate
    )

    fun createFeatureList(): String {
        return features.joinToString("\n") { getFeatureName(it) }
    }

    fun getFeatureName(feature: Feature): String = feature::class.simpleName ?: "Unknown"

    fun registerFeatures() {
        features.forEach {
            println("Registering feature ${getFeatureName(it)}")
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(it)
        }
    }
}