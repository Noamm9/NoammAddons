package noammaddons

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
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
        const val MOD_VERSION = "3.5.4"
        const val CHAT_PREFIX = "§6§l[§b§lN§d§lA§6§l]§r"
        const val FULL_PREFIX = "§d§l§nNo§lamm§b§l§nAddons"
        const val DEBUG_PREFIX = "§8[§b§lN§d§lA §7DEBUG§8]"

        @JvmField
        val mc: Minecraft = Minecraft.getMinecraft()

        @OptIn(DelicateCoroutinesApi::class)
        val scope = GlobalScope

        val config = Config
        val hudData = PogObject("hudData", HudElementConfig())
        val firstLoad = PogObject("firstLoad", true)
        val SlotBindingData = PogObject("SlotBinding", mutableMapOf<String, Double?>())

        val ahData = mutableMapOf<String, Double>()
        val bzData = mutableMapOf<String, ItemUtils.bzitem>()
        val npcData = mutableMapOf<String, Double>()
        val itemIdToNameLookup = mutableMapOf<String, String>()
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
            it["slim"]?.run { addLayer(CosmeticRendering(this)) }
            it["default"]?.run { addLayer(CosmeticRendering(this)) }
        }

        registerFeatures()
        registerCommands()
    }

    @SubscribeEvent
    fun onKey(event: PreKeyInputEvent) {
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

    init {
        ThreadUtils.loop(600_000) {
            UpdateUtils.update()

            // todo: use https://api.hypixel.net/skyblock/auctions instead
            JsonUtils.fetchJsonWithRetry<Map<String, Double>>("https://moulberry.codes/lowestbin.json") {
                it ?: return@fetchJsonWithRetry
                ahData.clear()
                ahData.putAll(it)
            }

            JsonUtils.fetchJsonWithRetry<Map<String, ItemUtils.bzitem>>("https://sky.shiiyu.moe/api/v2/bazaar") {
                it ?: return@fetchJsonWithRetry
                bzData.clear()
                bzData.putAll(it)
            }

            JsonUtils.get("https://api.hypixel.net/resources/skyblock/items") { obj ->
                if (obj["success"]?.jsonPrimitive?.booleanOrNull != true) return@get

                val items = JsonUtils.json.decodeFromJsonElement(
                    ListSerializer(ItemUtils.APISBItem.serializer()),
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
        }
    }
}