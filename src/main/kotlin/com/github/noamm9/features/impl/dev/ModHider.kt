package com.github.noamm9.features.impl.dev

import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.mixin.ILanguage
import com.github.noamm9.mixin.ILanguageManager
import com.github.noamm9.ui.clickgui.components.impl.DropdownSetting
import com.github.noamm9.ui.clickgui.components.impl.MultiCheckboxSetting
import com.github.noamm9.ui.clickgui.components.impl.TextInputSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.PacketLogger
import com.github.noamm9.utils.PayloadUtils
import com.github.noamm9.utils.equalsOneOf
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.ClientBrandRetriever
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.resources.language.ClientLanguage
import net.minecraft.client.resources.language.LanguageInfo
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentContents
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.KeybindContents
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.BrandPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.DiscardedPayload
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.resources.MultiPackResourceManager
import java.util.*
import kotlin.jvm.optionals.getOrNull

@AlwaysActive
object ModHider: Feature("Hides which mods you are running from the server: blocks the Sign/Anvil translation key exploit, spoofs your client brand, strips the mod list and filters outgoing plugin channels", toggled = true) {
    const val MODE_OFF = 0
    const val MODE_VANILLA = 1
    const val MODE_MODDED = 2
    const val MODE_CUSTOM = 3

    private val DEFAULT_ALLOWED_MODS = setOf("minecraft", "fabricloader", "java", "fabric")

    private val DEFAULT_ALLOWED_CHANNELS = listOf(
        "fabric:attachment_sync_v1",
        "fabric:recipe_sync",
        "fabric-screen-handler-api-v1:open_screen",
        "hypixel:ping",
        "hypixel:party_info",
        "hypixel:player_info",
        "hypixel:hello",
        "hypixel:register",
        "hyevent:location"
    )

    private val spoofMode by DropdownSetting("Spoof Mode", MODE_CUSTOM, listOf("Off", "Vanilla", "Modded", "Custom"))
        .section("Client Spoofing")
        .withDescription("Off: report everything honestly. Vanilla: pretend to be an unmodded client. Modded: admit to Fabric but hide the mod list. Custom: pick your own brand and whitelist")

    private val customBrand by TextInputSetting("Custom Brand", "fabric")
        .withDescription("The client brand reported to the server in Custom mode")
        .showIf { spoofMode.value == MODE_CUSTOM }

    private val hideModList by ToggleSetting("Hide Mod List", true)
        .withDescription("Strip your mods out of the known-pack list the server receives")
        .showIf { spoofMode.value == MODE_CUSTOM }

    private val blockPayloads by ToggleSetting("Block Plugin Channels", true)
        .withDescription("Refuse to send custom payloads on channels that are not whitelisted below")
        .showIf { spoofMode.value == MODE_CUSTOM }

    private val allowedMods by MultiCheckboxSetting("Allowed Mods", installedMods())
        .withDescription("Mods that stay visible to the server. Everything unchecked is hidden")
        .showIf { spoofMode.value == MODE_CUSTOM && hideModList.value }

    private val allowedChannels by TextInputSetting("Allowed Channels", DEFAULT_ALLOWED_CHANNELS.joinToString(","))
        .withDescription("Comma separated plugin channels that are allowed through")
        .showIf { spoofMode.value == MODE_CUSTOM && blockPayloads.value }

    private val notifyOnDetection by ToggleSetting("Notify On Detection", true)
        .section("Reporting")
        .withDescription("Warn once per server when it tries to read your mods through a Sign or Anvil")

    private val logBlockedPackets by ToggleSetting("Log Blocked Packets", false)
        .withDescription("Append every blocked payload to config/NoammAddons/logs/blocked_packets.log")

    override fun toggle() = Unit

    private val serverLanguages = IdentityHashMap<ClientPacketListener?, Language?>()
    private val language = ILanguage.invokeLoadDefault()
    private val warnedServers = mutableSetOf<String>()

    private val resending = ThreadLocal.withInitial { false }

    override fun init() {
        register<PacketEvent.Sent> {
            if (resending.get()) return@register
            val packet = event.packet as? ServerboundCustomPayloadPacket ?: return@register

            val mode = spoofMode.value
            if (mode == MODE_OFF) return@register

            val payload = packet.payload
            val id = payload.type().id().toString()

            if (id.equalsOneOf("minecraft:register", "minecraft:unregister")) {
                if (mode != MODE_CUSTOM || ! blockPayloads.value) return@register

                val filtered = PayloadUtils.filterRegisterPayload(payload, allowedChannelSet()) ?: return@register
                event.cancel()
                resend(ServerboundCustomPayloadPacket(filtered))
                return@register
            }

            if (payload is DiscardedPayload || payload is BrandPayload) return@register

            when (mode) {
                MODE_VANILLA -> {
                    event.cancel()
                    reportBlocked("Vanilla", payload, id)
                }

                MODE_CUSTOM -> if (blockPayloads.value && ! isChannelAllowed(id)) {
                    event.cancel()
                    reportBlocked("Custom", payload, id)
                }
            }
        }
    }

    private fun reportBlocked(source: String, payload: CustomPacketPayload, id: String) {
        if (logBlockedPackets.value) PacketLogger.logBlocked("ModHider ($source)", payload)
        NotificationManager.push("Mod Hider", "Blocked payload: &b$id")
    }

    private fun resend(packet: ServerboundCustomPayloadPacket) {
        resending.set(true)
        try {
            mc.connection?.send(packet)
        }
        finally {
            resending.set(false)
        }
    }

    private fun installedMods(): MutableMap<String, Boolean> {
        return FabricLoader.getInstance().allMods
            .map { it.metadata.id }
            .sorted()
            .associateWithTo(linkedMapOf()) { it in DEFAULT_ALLOWED_MODS }
    }

    private fun allowedChannelSet() = allowedChannels.value
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toSet()

    private fun isChannelAllowed(id: String) = allowedChannelSet().any { id.startsWith(it, ignoreCase = true) }

    /** Whether the mod list should be stripped out of what the server can see. */
    @JvmStatic
    fun shouldHideMods() = when (spoofMode.value) {
        MODE_VANILLA, MODE_MODDED -> true
        MODE_CUSTOM -> hideModList.value
        else -> false
    }

    /** The client brand to report, or null to report honestly. */
    @JvmStatic
    fun spoofedBrand(): String? = when (spoofMode.value) {
        MODE_VANILLA -> ClientBrandRetriever.VANILLA_NAME
        MODE_MODDED -> "fabric"
        MODE_CUSTOM -> customBrand.value.ifBlank { "fabric" }
        else -> null
    }

    /** Whether a resource pack belonging to [packId] may stay visible to the server. */
    @JvmStatic
    fun isModAllowed(packId: String): Boolean = when (spoofMode.value) {
        MODE_VANILLA -> false
        MODE_MODDED -> DEFAULT_ALLOWED_MODS.any { packId.startsWith(it, ignoreCase = true) }
        MODE_CUSTOM ->
            if (! hideModList.value) true
            else allowedMods.value.any { (id, allowed) -> allowed && packId.startsWith(id, ignoreCase = true) }

        else -> true
    }

    /** Warns once per server that it tried to read the mod list through a Sign or Anvil. */
    @JvmStatic
    fun notifyDetectionAttempt() {
        if (! notifyOnDetection.value) return

        val ip = mc.currentServer?.ip
        if (ip != null && ! warnedServers.add(ip)) return

        NotificationManager.push("Mod Hider", "This server tried to read your mod list.")
    }

    @JvmStatic
    fun getString(component: Component): String {
        if (component !is MutableComponent) return component.string
        val stringBuilder = StringBuilder()
        visit(component.contents)?.let { stringBuilder.append(it) }
        for (sibling in component.siblings) stringBuilder.append(getString(sibling))
        return stringBuilder.toString()
    }

    /** [getString], but also warns the user when the result differs from what vanilla would show. */
    @JvmStatic
    fun getStringAndReport(component: Component): String {
        val processed = getString(component)
        if (processed != component.string) notifyDetectionAttempt()
        return processed
    }

    private fun visit(contents: ComponentContents) = when (contents) {
        is KeybindContents if ! canTranslate(contents.name) -> contents.name
        is TranslatableContents if ! canTranslate(contents.key) -> contents.fallback ?: contents.key
        else -> contents.visit(Optional<String>::of).getOrNull()
    }

    private fun canTranslate(key: String): Boolean {
        if (mc.currentServer?.resourcePackStatus == ServerData.ServerPackStatus.ENABLED) {
            if (! serverLanguages.containsKey(mc.connection)) {
                if (! serverLanguages.isEmpty()) serverLanguages.clear()
                serverLanguages[mc.connection] = createServerLanguage()
            }

            return serverLanguages[mc.connection] !!.has(key)
        }

        return language.has(key)
    }

    private fun createServerLanguage(): Language {
        val allPackResources = mc.resourceManager.listPacks().toList()
        val packResources = mutableListOf(allPackResources.first())

        for (i in 1 ..< allPackResources.size) {
            val packResource = allPackResources[i]
            val source = packResource.location().source()
            if (
                ! shouldHideMods() ||
                isModAllowed(packResource.packId()) ||
                source.equalsOneOf(PackSource.FEATURE, PackSource.WORLD, PackSource.SERVER)
            ) packResources.add(packResource)
        }

        val resourceManager = MultiPackResourceManager(PackType.CLIENT_RESOURCES, packResources)
        val currentLanguageCode = mc.languageManager.selected

        var languageInfo: LanguageInfo? = null
        val languages = ILanguageManager.invokeExtractLanguages(resourceManager.listPacks())
        val list = mutableListOf("en_us")
        var bidirectional = ILanguageManager.getDefaultLanguage().bidirectional()
        if (currentLanguageCode != "en_us" && (languages[currentLanguageCode].also { languageInfo = it }) != null) {
            list.add(currentLanguageCode)
            bidirectional = languageInfo !!.bidirectional()
        }

        return ClientLanguage.loadFrom(resourceManager, list, bidirectional)
    }
}
