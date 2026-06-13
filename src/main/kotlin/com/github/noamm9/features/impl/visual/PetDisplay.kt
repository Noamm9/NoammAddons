package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.ui.clickgui.components.impl.ColorSetting
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.remove
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.height
import com.github.noamm9.utils.render.Render2D.highlight
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import java.awt.Color

@AlwaysActive
object PetDisplay: Feature("Pet Features") {
    private val petDisplay by ToggleSetting("Pet Display").withDescription("Draws the current active pet on screen.").section("HUD")
    private val autoPetTitles by ToggleSetting("Auto Pet Title").withDescription("Shows a title on screen when you swap pets via autopet rules.")

    private val activePetHighlight by ToggleSetting("Highlight Active pet").withDescription("highlights the active pet inside the pet menu").section("Pets Menu")
    private val petHighlightColor by ColorSetting("Highlight color", Color.CYAN).showIf { activePetHighlight.value }

    private val chatPetRuleRegex = Regex("§cAutopet §eequipped your §7\\[Lvl .*] (?<pet>.*)§e! §a§lVIEW RULE")
    private val chatSpawnRegex = Regex("§aYou summoned your (?<pet>.*)§a!")
    private val chatDespawnRegex = Regex("§aYou despawned your .*§a!")

    private val petMenuRegex = Regex("^\\(\\d/\\d\\) Pets$")
    private val petLevelRegex = Regex(".+\\[Lvl .*]")
    private var selectedPetSlot = - 1

    override fun init() {
        hudElement("PetDisplay",
            enabled = { petDisplay.value },
            shouldDraw = { LocationUtils.inSkyblock && cacheData.get()["pet"] != null }) { context, example ->
            val text = if (example) "&6Golden Dragon" else cacheData.get()["pet"].toString()
            Render2D.drawString(context, text, 0, 0)
            return@hudElement text.width().toFloat() to text.height().toFloat()
        }

        register<ChatMessageEvent> {
            if (! LocationUtils.inSkyblock) return@register
            event.formattedText.let {
                if (chatDespawnRegex.matches(it)) {
                    cacheData.get().remove("pet")
                    selectedPetSlot = - 1
                    return@register
                }

                val match1 = chatSpawnRegex.find(it)?.destructured?.component1()
                val match2 = chatPetRuleRegex.find(it)?.destructured?.component1()
                if (match2 != null && autoPetTitles.value && enabled) ChatUtils.showTitle(match2)
                cacheData.get()["pet"] = match1 ?: match2 ?: return@let
            }
        }

        register<ContainerFullyOpenedEvent> {
            selectedPetSlot = - 1
            if (! event.title.unformattedText.matches(petMenuRegex)) return@register
            for ((i, stack) in event.items) {
                val lore = stack.lore
                if (lore.getOrNull(lore.lastIndex - 2)?.removeFormatting() != "Click to despawn!") continue
                cacheData.get()["pet"] = stack.hoverName.formattedText.remove(petLevelRegex).trim()
                selectedPetSlot = i
                return@register
            }
        }

        register<ContainerEvent.Render.Slot.Pre> {
            if (! enabled) return@register
            if (! activePetHighlight.value) return@register
            if (event.slot.index != selectedPetSlot) return@register
            event.slot.highlight(event.context, petHighlightColor.value)
        }

        register<PacketEvent.Sent> {
            if (! enabled) return@register
            if (! activePetHighlight.value) return@register
            if (event.packet !is ServerboundContainerClosePacket) return@register
            selectedPetSlot = - 1
        }

        register<MainThreadPacketReceivedEvent.Pre> {
            if (! enabled) return@register
            if (! activePetHighlight.value) return@register
            if (event.packet !is ClientboundContainerClosePacket) return@register
            selectedPetSlot = - 1
        }
    }
}