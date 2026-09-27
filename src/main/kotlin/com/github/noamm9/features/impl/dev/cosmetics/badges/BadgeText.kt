package com.github.noamm9.features.impl.dev.cosmetics.badges

import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.features.impl.dev.Cosmetics
import com.github.noamm9.features.impl.dev.cosmetics.CosmeticData
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import java.util.*

object BadgeText {
    private val font = FontDescription.Resource(Identifier.fromNamespaceAndPath(MOD_ID, "badges"))
    @Volatile private var prefixes = emptyMap<UUID, Component>()
    @Volatile private var lastPeople = emptyMap<UUID, CosmeticData>()

    fun init(people: Map<UUID, CosmeticData>) {
        lastPeople = people
        rebuild()
    }

    fun rebuild() {
        prefixes = lastPeople.mapNotNull { (uuid, data) ->
            val glyphs = data.badges.mapNotNull { BadgeManager.charOf(it) }.ifEmpty { return@mapNotNull null }

            val prefix = Component.empty()
            glyphs.forEachIndexed { index, glyph ->
                if (index > 0) prefix.append(Component.literal(BadgeManager.SEPARATOR.toString()).withStyle { it.withFont(font) })
                prefix.append(Component.literal(glyph.toString()).withStyle {
                    it.withFont(font).withColor(0xFFFFFF).withBold(false).withItalic(false).withShadowColor(0)
                })
            }
            prefix.append(Component.literal(" "))

            uuid to prefix
        }.toMap()
    }

    @JvmStatic
    fun decorate(name: Component, uuid: UUID): Component {
        if (! Cosmetics.enabled || ! Cosmetics.showBadges.value) return name
        val prefix = prefixes[uuid] ?: return name
        return Component.empty().append(prefix).append(name)
    }
}