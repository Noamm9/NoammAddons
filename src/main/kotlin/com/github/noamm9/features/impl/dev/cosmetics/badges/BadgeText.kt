package com.github.noamm9.features.impl.dev.cosmetics.badges

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.features.impl.dev.Cosmetics
import com.github.noamm9.features.impl.dev.cosmetics.CosmeticData
import com.github.noamm9.utils.catch
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import java.util.*

object BadgeText {
    private val font = FontDescription.Resource(Identifier.fromNamespaceAndPath(MOD_ID, "badges"))
    @Volatile private var prefixes = emptyMap<UUID, Component>()

    fun init(people: Map<UUID, CosmeticData>) {
        prefixes = people.mapNotNull { (uuid, data) ->
            if (! data.hasBadge) return@mapNotNull null

            val icons = data.badges.mapNotNull {
                val icon = catch { Badges.valueOf(it) }
                if (icon == null) {
                    NoammAddons.logger.info("[${BadgeText.javaClass.simpleName}] Badge not found: $it for $uuid")
                    return@mapNotNull null
                }

                icon
            }

            if (icons.isEmpty()) return@mapNotNull null

            val prefix = Component.empty()
            icons.forEachIndexed { index, glyph ->
                if (index > 0) prefix.append(Component.literal("\uE004").withStyle { it.withFont(font) })
                prefix.append(Component.literal(glyph.char.toString()).withStyle {
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