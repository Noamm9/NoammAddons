package com.github.noamm9.features.impl.dev

import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.mixin.ILanguage
import com.github.noamm9.mixin.ILanguageManager
import com.github.noamm9.utils.equalsOneOf
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
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.resources.MultiPackResourceManager
import java.util.*
import kotlin.jvm.optionals.getOrNull


// stops the exploit where the server can send a translation key inside a Sign or Anvil to gets the mods you are using
// mods without language keys are safe from this
object ModHider {
    private val language = ILanguage.invokeLoadDefault()
    private val serverLanguages = IdentityHashMap<ClientPacketListener?, Language?>()

    /*
    private val serverCheckedMods = ObjectArraySet<String>()

    EventBus.register<ServerEvent.Connect> {
        ThreadUtils.scheduledTask(20) {
            if (serverCheckedMods.isEmpty()) return@scheduledTask
            val ip = mc.currentServer?.ip ?: return@scheduledTask
            NotificationManager.push("ModHider", "Blocked $ip from reading mods: ${serverCheckedMods.joinToString(", ")}", 5000)
            serverCheckedMods.clear()
        }
    }

    EventBus.register<ServerEvent.Disconnect> {
        serverCheckedMods.clear()
    }

    @JvmStatic
    fun addMod(key: String) = serverCheckedMods.add(key.substringAfter(".").substringBefore("."))

    */
    @JvmStatic
    fun getString(component: Component): String {
        if (component !is MutableComponent) return component.string
        val stringBuilder = StringBuilder()
        visit(component.contents)?.let { stringBuilder.append(it) }
        for (sibling in component.siblings) stringBuilder.append(getString(sibling))
        return stringBuilder.toString()
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
        val packResources = ArrayList<PackResources?>().apply { add(allPackResources.first()) }

        for (i in 1 ..< allPackResources.size) {
            val packResource = allPackResources[i]
            val source = packResource.location().source()
            if (source.equalsOneOf(PackSource.FEATURE, PackSource.WORLD, PackSource.SERVER)) {
                packResources.add(packResource)
            }
        }

        val resourceManager = MultiPackResourceManager(PackType.CLIENT_RESOURCES, packResources)
        val currentLanguageCode = mc.languageManager.selected

        var languageInfo: LanguageInfo? = null
        val languages = ILanguageManager.invokeExtractLanguages(resourceManager.listPacks())
        val list = ArrayList<String?>(2).apply { add("en_us") }
        var bidirectional = ILanguageManager.getDefaultLanguage().bidirectional()
        if (currentLanguageCode != "en_us" && (languages[currentLanguageCode].also { languageInfo = it }) != null) {
            list.add(currentLanguageCode)
            bidirectional = languageInfo !!.bidirectional()
        }

        return ClientLanguage.loadFrom(resourceManager, list, bidirectional)
    }
}