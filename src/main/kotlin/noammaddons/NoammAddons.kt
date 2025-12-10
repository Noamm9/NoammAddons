package noammaddons

import kotlinx.coroutines.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.commands.CommandManager.registerCommands
import noammaddons.config.PogObject
import noammaddons.events.EventDispatcher
import noammaddons.events.PreKeyInputEvent
import noammaddons.features.FeatureManager
import noammaddons.features.FeatureManager.registerFeatures
import noammaddons.features.impl.DevOptions
import noammaddons.features.impl.misc.ClientBranding
import noammaddons.features.impl.misc.Cosmetics
import noammaddons.features.impl.misc.Cosmetics.CosmeticRendering
import noammaddons.ui.font.GlyphPageFontRenderer
import noammaddons.ui.font.TextRenderer
import noammaddons.utils.*
import noammaddons.utils.JsonUtils.getObj
import noammaddons.websocket.WebSocket
import org.apache.logging.log4j.LogManager


@Mod(
    modid = NoammAddons.MOD_ID,
    name = NoammAddons.MOD_NAME,
    version = NoammAddons.MOD_VERSION,
    clientSideOnly = true,
    dependencies = "before:*"
)
class NoammAddons {
    private var loadTime = 0L

    companion object {
        const val MOD_NAME = "@NAME@"
        const val MOD_ID = "@MODID@"
        const val MOD_VERSION = "@VER@"
        const val CHAT_PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
        const val FULL_PREFIX = "§d§l§nNo§lamm§b§l§nAddons"
        const val DEBUG_PREFIX = "§8[§b§lN§d§lA §7DEBUG§8]"

        @JvmField
        val Logger = LogManager.getLogger(MOD_NAME)

        @JvmField
        val mc = Minecraft.getMinecraft()

        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        val hudData = PogObject("hudData", HudElementConfig())
        private val firstLoad = PogObject("firstLoad", true)
        val personalBests = PogObject("PersonalBests", PersonalBestData())

        @JvmField
        val ahData = mutableMapOf<String, Double>()

        @JvmField
        val bzData = mutableMapOf<String, Double>()

        @JvmField
        val npcData = mutableMapOf<String, Double>()

        @JvmField
        val itemIdToNameLookup = mutableMapOf<String, String>()
        var mayorData: ApiMayor = ApiMayor.empty

        @JvmStatic
        lateinit var textRenderer: TextRenderer
            private set

        @JvmStatic
        var initialized = false
            private set
    }

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        Logger.info("Initializing $MOD_NAME")
        loadTime = System.currentTimeMillis()

        ClientBranding.setCustomIcon()
        ClientBranding.setCustomTitle()

        textRenderer = TextRenderer(GlyphPageFontRenderer.create("Inter", 20, true, true, true))

        listOf(
            this, EventDispatcher, ThreadUtils,
            TestGround, GuiUtils, ScanUtils,
            LocationUtils, DungeonUtils,
            ActionBarParser, PartyUtils,
            ChatUtils, EspUtils, ActionUtils,
            ServerPlayer, ScoreboardUtils,
            PacketManager, ServerUtils,
            SlayerUtils,
            FeatureManager
        ).forEach(MinecraftForge.EVENT_BUS::register)

        mc.renderManager.skinMap.let {
            it["slim"]?.run { addLayer(CosmeticRendering(this)) }
            it["default"]?.run { addLayer(CosmeticRendering(this)) }
        }

        MinecraftForge.EVENT_BUS.register(Cosmetics.CustomPlayerSize)

        WebSocket.init()

        this.init()
        registerFeatures()
        registerCommands()

        Logger.info("Finished Initializing $MOD_NAME")
        Logger.info("Load Time: ${(System.currentTimeMillis() - loadTime) / 1000.0} seconds")
        initialized = true
    }

    @SubscribeEvent
    fun onKey(event: PreKeyInputEvent) {
        if (! firstLoad.getData()) return
        firstLoad.setData(false)
        firstLoad.save()
        Utils.playFirstLoadMessage()
    }

    fun init() = ThreadUtils.loop(600_000) {
        if (DevOptions.updateChecker) UpdateUtils.update()

        WebUtils.get("https://api.noammaddons.workers.dev/lowestbin") { obj ->
            ahData.putAll(JsonUtils.json.decodeFromJsonElement(MapSerializer(String.serializer(), Double.serializer()), obj))
        }

        WebUtils.get("https://api.noammaddons.workers.dev/bazaar") { obj ->
            bzData.putAll(JsonUtils.json.decodeFromJsonElement(MapSerializer(String.serializer(), Double.serializer()), obj))
        }

        WebUtils.get("https://api.noammaddons.workers.dev/items") { obj ->
            itemIdToNameLookup.putAll(JsonUtils.json.decodeFromJsonElement(MapSerializer(String.serializer(), String.serializer()), obj["itemIdToName"] !!))
            npcData.putAll(JsonUtils.json.decodeFromJsonElement(MapSerializer(String.serializer(), Double.serializer()), obj["sellPrice"] !!))
        }

        WebUtils.get("https://api.noammaddons.workers.dev/mayor") { jsonObject ->
            val mayorElement = jsonObject.getObj("mayor") ?: return@get
            val ministerElement = mayorElement["minister"]
            mayorData = ApiMayor(
                JsonUtils.json.decodeFromJsonElement(ApiMayor.Candidate.serializer(), mayorElement),
                ministerElement?.let { JsonUtils.json.decodeFromJsonElement(ApiMayor.Minister.serializer(), it) }
                    ?: ApiMayor.Minister("", ApiMayor.Perk.empty)
            )
        }
    }
}