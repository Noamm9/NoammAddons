package noammaddons

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.commands.CommandManager.registerCommands
import noammaddons.config.*
import noammaddons.events.PreKeyInputEvent
import noammaddons.events.RegisterEvents
import noammaddons.features.FeatureManager.registerFeatures
import noammaddons.features.misc.Cosmetics.CosmeticRendering
import noammaddons.utils.*


@Mod(
    modid = noammaddons.MOD_ID,
    name = noammaddons.MOD_NAME,
    version = noammaddons.MOD_VERSION,
    clientSideOnly = true
)

class noammaddons {
    companion object {
        const val MOD_NAME = "NoammAddons"
        const val MOD_ID = "noammaddons"
        const val MOD_VERSION = "3.1.1"
        const val CHAT_PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
        const val FULL_PREFIX = "§d§l§nNo§lamm§b§l§nAddons"
        const val DEBUG_PREFIX = "§8[§b§lN§d§lA §7DEBUG§8]"

        val mc: Minecraft = Minecraft.getMinecraft()

        @OptIn(DelicateCoroutinesApi::class)
        val scope = GlobalScope

        val config = Config
        val hudData = PogObject("hudData", HudElementConfig())
        val firstLoad = PogObject("firstLoad", true)
        val SlotBindingData = PogObject("SlotBinding", mutableMapOf<String, Double?>())
    }

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        config.init()

        KeyBinds.allBindings.forEach(ClientRegistry::registerKeyBinding)

        listOf(
            this, RegisterEvents,
            TestGround, GuiUtils,
            LocationUtils, DungeonUtils,
            ActionBarParser, PartyUtils,
            ChatUtils, EspUtils
        ).forEach(MinecraftForge.EVENT_BUS::register)
    }

    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        mc.renderManager.skinMap.let {
            it["slim"]?.run { this.addLayer(CosmeticRendering(this)) }
            it["default"]?.run { this.addLayer(CosmeticRendering(this)) }
        }

        registerFeatures()
        registerCommands()
    }

    @SubscribeEvent
    fun onTick(event: PreKeyInputEvent) {
        if (KeyBinds.Config.isKeyDown) {
            GuiUtils.openScreen(config.gui())
        }

        if (firstLoad.getData()) {
            firstLoad.setData(false)
            Utils.playFirstLoadMessage()
            ThreadUtils.setTimeout(11_000) {
                config.openDiscordLink()
                GuiUtils.openScreen(config.gui())
            }
        }
    }
}