package com.github.noamm9.features.impl.dev.cosmetics.badges

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.mixin.IFontManager
import com.github.noamm9.mixin.IMinecraft
import com.github.noamm9.utils.equalsOneOf
import com.mojang.blaze3d.font.GlyphProvider
import com.mojang.blaze3d.font.SpaceProvider
import net.minecraft.client.gui.font.FontOption
import net.minecraft.client.gui.font.providers.BitmapProvider
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import java.io.ByteArrayInputStream
import java.util.*
import kotlin.jvm.optionals.getOrNull

object BadgeFontInjector {
    private val FONT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "badges")
    private val ATLAS_ID = Identifier.fromNamespaceAndPath(MOD_ID, "font/badges.png")
    private val ATLAS_TEXTURE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "textures/font/badges.png")

    @Volatile private var current: List<GlyphProvider.Conditional> = emptyList()
    @Volatile private var retained: Pair<ByteArray, List<Char>>? = null

    fun apply(pngBytes: ByteArray, chars: List<Char>) {
        retained = pngBytes to chars
        closeAll(current)
        current = emptyList()

        val fontSet = fontSets()[FONT_ID] ?: return
        val providers = loadProviders(pngBytes, chars) ?: return
        runCatching { fontSet.reload(providers, emptySet()) }.onFailure {
            NoammAddons.logger.error("[BadgeFontInjector] Failed to inject badge providers", it)
            closeAll(providers)
            return
        }

        current = providers
        NoammAddons.logger.info("[BadgeFontInjector] Injected ${chars.size} dynamic badges")
    }

    @JvmStatic
    fun reapply() {
        val (pngBytes, chars) = retained ?: return
        apply(pngBytes, chars)
    }

    @Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
    private fun loadProviders(pngBytes: ByteArray, chars: List<Char>): List<GlyphProvider.Conditional>? {
        val real = NoammAddons.mc.resourceManager

        val overlay = object: ResourceManager by real {
            override fun getResource(id: Identifier): Optional<Resource> {
                if (id.equalsOneOf(ATLAS_ID, ATLAS_TEXTURE_ID)) {
                    val pack = real.listPacks().findFirst().getOrNull() ?: error("Failed to load pack")
                    return Optional.of(Resource(pack) { ByteArrayInputStream(pngBytes) })
                }
                return real.getResource(id)
            }
        }

        val bitmap = runCatching {
            BitmapProvider.Definition(
                ATLAS_ID,
                BadgePack.HEIGHT, BadgePack.ASCENT,
                BadgePack.codepointGrid(chars)
            ).unpack().left().orElseThrow().load(overlay)
        }.getOrElse {
            NoammAddons.logger.error("[BadgeFontInjector] Failed to load badge atlas", it)
            return null
        }

        val space = SpaceProvider(mapOf(BadgeManager.SEPARATOR.code to 1f))

        return listOf(
            GlyphProvider.Conditional(bitmap, FontOption.Filter.ALWAYS_PASS),
            GlyphProvider.Conditional(space, FontOption.Filter.ALWAYS_PASS)
        )
    }

    private fun fontSets() = ((NoammAddons.mc as IMinecraft).fontManager as IFontManager).fontSets
    private fun closeAll(providers: List<GlyphProvider.Conditional>) = providers.forEach { runCatching { it.close() } }
}