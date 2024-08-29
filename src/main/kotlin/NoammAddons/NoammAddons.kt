package NoammAddons

import NoammAddons.Sounds.AYAYA
import NoammAddons.Sounds.ihavenothing
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.client.ClientCommandHandler
import gg.essential.api.EssentialAPI
import org.lwjgl.input.Keyboard
import NoammAddons.config.Config
import NoammAddons.commands.NoammAddonsCommands
import NoammAddons.commands.SkyBlockCommands.*
import NoammAddons.events.ClickEvent
import NoammAddons.features.General.*
import NoammAddons.features.Cosmetics.*
import NoammAddons.features.dungeons.*
import NoammAddons.features.Alerts.*
import NoammAddons.features.gui.*
import NoammAddons.utils.*
import NoammAddons.utils.ThreadUtils.setTimeout


@Mod(
    modid = NoammAddons.MOD_ID,
    name = NoammAddons.MOD_NAME,
    version = NoammAddons.MOD_VERSION,
    clientSideOnly = true
)

class NoammAddons {
    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        config.init()

        // Registering all Commands
        listOf(
            NoammAddonsCommands(),
            DungeonHub(),
            CrimonIsle(),
            End(),
            hub(),
            Skyblock()
        ).forEach{ClientCommandHandler.instance.registerCommand(it)}


        // Registering all keybinds
        keybinds.forEach { ClientRegistry.registerKeyBinding(it) }


        listOf(
            this,
            // General
            EnderPearlFix,
            LeftClickEtherwarp,
            CustomItemEntity,
            ChatCoordsWaypoint,

            // Dungeons
            TeammatesNames,
            TeammatesOutline,
            AbilityKeybinds,
            IHATEDIORITE,
            AutoCloseChest,
            F7PreGhostBlocks,
            HighlightMimicChest,
            GhostBlock,
            HiddenMobs,
            ShowExtraStats,
            TraceKeys,
            AutoUlt,
            AutoRefillEnderPearls,
            AutoI4,


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

            // Cosmetics
            BlockOverlay,
            PlayerScale,
            TimeChanger,
            HideFallingBlocks,
            DamageSplash,
            RemoveSellfieCam,
            CustomFov,
            AntiBlind,
            NoBlockAnimation,
            NoWaterFOV,

            // Utilities
            GuiUtils,
            LocationUtils,
            DungeonUtils

        ).forEach(MinecraftForge.EVENT_BUS::register)

        print(event.modState)
    }


    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (keybinds[1].isPressed) {
            EssentialAPI.getGuiUtil().openScreen(config.gui())
            setTimeout(1000) {PlayerUtils.swapToSlot(10)}
        }
    }


    companion object {
        const val MOD_NAME = "NoammAddons"
        const val MOD_ID = "noammaddons"
        const val MOD_VERSION = "1.0.0"
        const val CHAT_PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
        const val FULL_PREFIX = "§d§l§nNoamm§b§l§nAddons"

        val mc: Minecraft = Minecraft.getMinecraft()
        var config = Config

        val keybinds = listOf(
            KeyBinding("Ghost Pick", Keyboard.KEY_Z, MOD_NAME),
            KeyBinding("Config", Keyboard.KEY_RSHIFT, MOD_NAME),
            KeyBinding("Dungeon class Ultimate", Keyboard.KEY_GRAVE, MOD_NAME),
            KeyBinding("Dungeon class Ability", 56, MOD_NAME),
        )
    }
}