package com.github.noamm9.features.impl.misc

import com.github.noamm9.features.Feature
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.PlainTextContents

/**
 * @see com.github.noamm9.mixin.MixinItemStack
 */
object OldMasterStar: Feature("Displays master stars the old way (e.g. §6✪✪✪✪✪§c➌§r -> §c✪✪✪§6✪✪).") {
    private val masterStars = listOf("➊", "➋", "➌", "➍", "➎")

    private val cache = object: LinkedHashMap<Component, Component>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Component, Component>) = size > 128
    }

    @JvmStatic
    fun transformName(name: Component): Component {
        if (! enabled) return name
        return cache.getOrPut(name) { buildTransformed(name) }
    }

    private fun buildTransformed(name: Component): Component {
        val l = name.siblings.size
        if (l < 2) return name

        var i = 0
        var normalStarsComp = name.siblings[l - 2]
        var starsComp = name.siblings.last()
        var starsText = (starsComp.contents as? PlainTextContents.LiteralContents)?.text ?: return name

        if (starsText == "✦") {
            if (l < 3) return name
            i = 1
            normalStarsComp = name.siblings[l - 3]
            starsComp = name.siblings[l - 2]
            starsText = (starsComp.contents as? PlainTextContents.LiteralContents)?.text?.trim() ?: return name
        }

        val stars = masterStars.indexOf(starsText) + 1
        if (stars <= 0) return name

        val comp = MutableComponent.create(name.contents).withStyle(name.style)
        comp.siblings.addAll(name.siblings.subList(0, l - 2 - i))
        comp.siblings.add(Component.literal("✪".repeat(stars)).withStyle(starsComp.style))
        if (stars != 5) {
            comp.siblings.add(Component.literal("✪".repeat(5 - stars)).withStyle(normalStarsComp.style))
        }
        if (i > 0) {
            comp.siblings.add(Component.literal(" "))
            comp.siblings.addAll(name.siblings.subList(l - i, l))
        }

        return comp
    }
}
