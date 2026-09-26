package com.github.noamm9.features.impl.dev.cosmetics

import com.github.noamm9.features.impl.dev.Cosmetics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import java.util.UUID

object BadgeText {
    private val font = FontDescription.Resource(Identifier.fromNamespaceAndPath("noammaddons", "badges"))
    private val glyphs = mapOf("owner" to "\uE000", "dev" to "\uE001", "amogus" to "\uE002", "amgus" to "\uE002", "wheelchair" to "\uE003")
    @Volatile private var prefixes = emptyMap<UUID, Component>()

    fun init(people: Map<UUID, CosmeticData>) {
        prefixes = people.mapNotNull { (uuid, data) ->
            val icons = data.badges.mapNotNull { glyphs[it.lowercase()] }
            if (icons.isEmpty()) return@mapNotNull null

            val prefix = Component.empty()
            icons.forEachIndexed { index, glyph ->
                if (index > 0) prefix.append(Component.literal("\uE004").withStyle { it.withFont(font) })
                prefix.append(Component.literal(glyph).withStyle {
                    it.withFont(font).withColor(0xFFFFFF).withBold(false).withItalic(false).withShadowColor(0)
                })
            }
            prefix.append(Component.literal(" "))
            uuid to prefix
        }.toMap()
    }

    @JvmStatic fun decorate(name: Component, uuid: UUID): Component {
        if (!Cosmetics.enabled || !Cosmetics.showBadges.value) return name
        val prefix = prefixes[uuid] ?: return name
        return Component.empty().append(prefix).append(name)
    }
}
