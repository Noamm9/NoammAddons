package noammaddons.features


import net.minecraft.network.Packet
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.impl.DevOptions
import noammaddons.features.impl.alerts.*
import noammaddons.features.impl.dungeons.*
import noammaddons.features.impl.dungeons.dmap.DungeonMap
import noammaddons.features.impl.dungeons.solvers.LividSolver
import noammaddons.features.impl.dungeons.solvers.devices.*
import noammaddons.features.impl.dungeons.solvers.puzzles.*
import noammaddons.features.impl.dungeons.solvers.terminals.TerminalSolver
import noammaddons.features.impl.esp.*
import noammaddons.features.impl.general.*
import noammaddons.features.impl.general.teleport.*
import noammaddons.features.impl.gui.*
import noammaddons.features.impl.gui.Menus.impl.CustomPetMenu
import noammaddons.features.impl.gui.Menus.impl.CustomWardrobeMenu
import noammaddons.features.impl.hud.*
import noammaddons.features.impl.misc.*
import noammaddons.features.impl.slayers.ExtraSlayerInfo
import noammaddons.ui.config.core.save.Config
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.RenderHelper.getStringWidth

object FeatureManager {
    data class PacketListener<T: Packet<*>>(val type: Class<T>, val shouldRun: () -> Boolean, val function: (T) -> Unit)
    data class MessageListener(val filter: Regex, val shouldRun: () -> Boolean, val function: (MatchResult) -> Unit)
    data class ServerTickListener(val shouldRun: () -> Boolean, val function: () -> Unit)

    val packetListeners = arrayListOf<PacketListener<Packet<*>>>()
    val chatListeners = arrayListOf<MessageListener>()
    val serverTickListeners = arrayListOf<ServerTickListener>()
    val worldLoadListeners = arrayListOf<() -> Unit>()

    val features = mutableSetOf(
        GlobalEspSettings, GlobalCustomMenuSettings, MotionBlur, GyroHelper,
        ChatEmojis, LeftClickEtherwarp, ChatCoordsWaypoint, SlotBinding,
        PartyESP, SBKick, CakeNumbers, EnderPearlFix, WardrobeKeybinds,
        PartyCommands, ShowItemEntityName, BlockGloomlockOverUse, VisualWords,
        RemoveUselessMessages, ShowItemRarity, SkyblockExp, ShowItemsPrice,
        TeammatesESP, AbilityKeybinds, IHateDiorite, CloseChest, StonkSwapSound,
        MimicDetector, GhostPick, AutoExtraStats, HighlightDoorKeys, AutoUlt,
        PartyFinder, TickTimers, LeapMenu, ReaperArmor, AutoI4, ArchitectDraft,
        BetterFloors, BloodDialogueSkip, AutoGFS, AutoPotion, CryptsDoneAlert,
        M7Relics, EtherwarpSound, MaxorsCrystals, DungeonChestProfit, FpsBoost,
        Camera, PlayerModel, DamageSplash, AutoRequeue, DungeonPlayerDeathAlert,
        DungeonWarpCooldown, RoomAlerts, DungeonRunSplits, RatProtection, M7Dragons,
        CreeperBeamSolver, BlazeSolver, BoulderSolver, ThreeWeirdosSolver, LividSolver,
        SimonSaysSolver, ArrowAlignSolver, Floor4BossFight, WitherESP, StarMobESP,
        HiddenMobs, PestESP, DungeonSecrets, SmoothBossBar, NoRotate, RNGSound,
        BloodReady, ExtraSlayerInfo, SalvageOverlay, RagAxe, ShadowAssassinAlert,
        MelodyAlert, PlayerHud, FullBlock, TerminalNumbers, F7Titles, ScalableTooltips,
        FpsDisplay, ClockDisplay, TpsDisplay, PetDisplay, MaskTimers, SpringBootsDisplay,
        NoBlockAnimation, WitherShieldTimer, CustomBowHitSound, `ZeroPingTeleportation (ZPT)`,
        TeleportOverlay, CustomScoreboard, BlockOverlay, CustomWardrobeMenu,
        CustomPetMenu, TimeChanger, CustomTabList, TerminalSolver, ChamNametags,
        DevOptions, CustomSlotHighlight,

        DungeonMap,
        //ProfileViewer,
    ).sortedBy { it.name }

    @SubscribeEvent(receiveCanceled = true)
    fun onReceivePacket(event: PacketEvent.Received) = packetListeners.forEach { if (it.shouldRun() && it.type.isInstance(event.packet)) it.function(event.packet) }

    @SubscribeEvent(receiveCanceled = true)
    fun onSendPacket(event: PacketEvent.Sent) = packetListeners.forEach { if (it.shouldRun() && it.type.isInstance(event.packet)) it.function(event.packet) }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatPacket(event: Chat) = chatListeners.forEach { if (it.shouldRun()) it.function(it.filter.find(event.component.noFormatText) ?: return@forEach) }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) = worldLoadListeners.forEach { it.invoke() }

    @SubscribeEvent
    fun onServerTick(event: ServerTick) = serverTickListeners.forEach { if (it.shouldRun()) it.function() }

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