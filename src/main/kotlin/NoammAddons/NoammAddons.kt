package NoammAddons

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import org.lwjgl.input.Keyboard
import NoammAddons.config.Config
import NoammAddons.features.Cosmetics.*
import NoammAddons.features.dungeons.*
import NoammAddons.utils.GuiUtils
import NoammAddons.utils.LocationUtils
import NoammAddons.command.NoammAddonsCommands
import NoammAddons.utils.DungeonUtils
import NoammAddons.utils.RenderUtils
import gg.essential.api.EssentialAPI
import net.minecraftforge.fml.client.registry.ClientRegistry
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
        config.init()

        ClientCommandHandler.instance.registerCommand(NoammAddonsCommands())
        keybinds.forEach { ClientRegistry.registerKeyBinding(it) }

        listOf(
            this,
            TimeChanger,
            BlockOverlay,
            CustomPlayerScale,
            HideFallingBlocks,
            CustomDamageSplash,
            IHATEDIORITE,
            RemoveSellfieCam,
            CustomFov,
            AntiBlind,
            AutoCloseChest,
            BloodReady,
            F7PreGhostBlocks,
            GhostBlock,
            HiddenMobs,
            LividESP,
            MobESP,
            NoBlockAnimation,
            NoWaterFOV,

            // Utilities
            GuiUtils,
            LocationUtils,
            RenderUtils,
            DungeonUtils
        ).forEach(MinecraftForge.EVENT_BUS::register)
    }


    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (keybinds[1].isKeyDown) EssentialAPI.getGuiUtil().openScreen(Config.gui())
    }

    companion object {
        const val MOD_ID = "noammaddons"
        const val MOD_NAME = "NoammAddons"
        const val MOD_VERSION = "1.0.0"
        const val CHAT_PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
        const val Full_Name_Prefix = "§d§l§nNoamm§b§l§nAddons"

        val mc: Minecraft = Minecraft.getMinecraft()
        var config = Config

        val keybinds = listOf(
            KeyBinding("Ghost Pick", Keyboard.KEY_Z, MOD_NAME),
            KeyBinding("Config", Keyboard.KEY_RSHIFT, MOD_NAME)
        )
    }
}
