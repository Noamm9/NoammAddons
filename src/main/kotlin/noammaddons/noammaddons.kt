package noammaddons

import kotlinx.coroutines.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.commands.CommandManager.registerCommands
import noammaddons.config.PogObject
import noammaddons.events.PreKeyInputEvent
import noammaddons.events.EventDispatcher
import noammaddons.features.FeatureManager
import noammaddons.features.FeatureManager.registerFeatures
import noammaddons.features.impl.DevOptions
import noammaddons.features.impl.misc.ClientBranding
import noammaddons.features.impl.misc.Cosmetics.CosmeticRendering
import noammaddons.ui.config.ConfigGUI
import noammaddons.utils.*
import noammaddons.utils.WebUtils.fetchJsonWithRetry
import org.apache.logging.log4j.LogManager


@Mod(
    modid = noammaddons.MOD_ID,
    name = noammaddons.MOD_NAME,
    version = noammaddons.MOD_VERSION,
    clientSideOnly = true
)
class noammaddons {
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

        @JvmStatic
        val mc = Minecraft.getMinecraft()

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val hudData = PogObject("hudData", DataClasses.HudElementConfig())
        private val firstLoad = PogObject("firstLoad", true)
        val personalBests = PogObject("PersonalBests", DataClasses.PersonalBestData())

        @JvmField
        val ahData = mutableMapOf<String, Double>()

        @JvmField
        val bzData = mutableMapOf<String, DataClasses.ApiBzItem>()

        @JvmField
        val npcData = mutableMapOf<String, Double>()

        @JvmField
        val itemIdToNameLookup = mutableMapOf<String, String>()
        var mayorData: DataClasses.ApiMayor? = null
    }

    @Mod.EventHandler
    fun onInit(event: FMLInitializationEvent) {
        Logger.info("Initializing NoammAddons")
        loadTime = System.currentTimeMillis()

        ClientBranding.setCustomIcon()
        ClientBranding.setCustomTitle()

        listOf(
            this, EventDispatcher, ThreadUtils,
            TestGround, GuiUtils, ScanUtils,
            LocationUtils, DungeonUtils,
            ActionBarParser, PartyUtils,
            ChatUtils, EspUtils, ActionUtils,
            TablistListener, ServerPlayer,
            ScoreboardUtils, FeatureManager
        ).forEach(MinecraftForge.EVENT_BUS::register)

        mc.renderManager.skinMap.let {
            it["slim"]?.run { addLayer(CosmeticRendering(this)) }
            it["default"]?.run { addLayer(CosmeticRendering(this)) }
        }

        registerFeatures()
        registerCommands()
        this.init()

        Logger.info("Finished Initializing NoammAddons")
        Logger.info("Load Time: ${(System.currentTimeMillis() - loadTime) / 1000.0} seconds")
    }

    @SubscribeEvent
    fun onKey(event: PreKeyInputEvent) {
        if (! firstLoad.getData()) return
        firstLoad.setData(false)
        Utils.playFirstLoadMessage()
        ThreadUtils.setTimeout(11_000) {
            GuiUtils.openScreen(ConfigGUI)
            Utils.openDiscordLink()
        }
    }

    fun init() {
        ThreadUtils.loop(600_000) {
            if (DevOptions.updateChecker) UpdateUtils.update()

            fetchJsonWithRetry<Map<String, Double>>("https://moulberry.codes/lowestbin.json") {
                it ?: return@fetchJsonWithRetry
                ahData.clear()
                ahData.putAll(it)
            }

            WebUtils.get("https://api.hypixel.net/v2/skyblock/bazaar") { obj ->
                obj ?: return@get
                if (obj["success"]?.jsonPrimitive?.booleanOrNull != true) return@get

                val rawBzData = JsonUtils.json.decodeFromJsonElement(
                    MapSerializer(String.serializer(), DataClasses.bzitem.serializer()),
                    obj["products"] !!
                )

                val data = rawBzData.entries.associate { it.value.quick_status.productId to it.value.quick_status }

                bzData.clear()
                bzData.putAll(data)
            }

            WebUtils.get("https://api.hypixel.net/resources/skyblock/items") { obj ->
                obj ?: return@get
                if (obj["success"]?.jsonPrimitive?.booleanOrNull != true) return@get

                val items = JsonUtils.json.decodeFromJsonElement(
                    ListSerializer(DataClasses.APISBItem.serializer()),
                    obj["items"] !!
                )

                val sellPrices = items.filter {
                    it.npcSellPrice != null
                }.associate { it.id to it.npcSellPrice !! }.toMutableMap()

                val idToName = items.associate { it.id to it.name }.toMutableMap()

                npcData.clear()
                npcData.putAll(sellPrices)
                itemIdToNameLookup.clear()
                itemIdToNameLookup.putAll(idToName)
            }

            WebUtils.get("https://api.hypixel.net/v2/resources/skyblock/election") { jsonObject ->
                jsonObject ?: return@get
                if (jsonObject["success"]?.jsonPrimitive?.booleanOrNull != true) return@get
                val dataElement = jsonObject["data"] ?: return@get
                mayorData = JsonUtils.json.decodeFromJsonElement(DataClasses.ApiMayor.serializer(), dataElement)
            }
        }
    }
}