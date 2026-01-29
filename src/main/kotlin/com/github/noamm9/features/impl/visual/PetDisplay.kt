package com.github.noamm9.features.impl.visual

import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.InventoryFullyOpenedEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.getValue
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.componnents.provideDelegate
import com.github.noamm9.ui.clickgui.componnents.section
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.Utils.remove
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.height
import com.github.noamm9.utils.render.Render2D.width
import java.awt.Color

object PetDisplay: Feature("Pet Features") {
    private val petDisplay by ToggleSetting("Pet Display")
    private val autoPetTitles by ToggleSetting("Auto Pet Title")
    private val activePetHighlight by ToggleSetting("Highlight Active pet").section("Pets Menu")

    private val chatPetRuleRegex = Regex("§cAutopet §eequipped your §7\\[Lvl .*] (?<pet>.*)§e! §a§lVIEW RULE")
    private val chatSpawnRegex = Regex("§aYou summoned your (?<pet>.*)§a!")
    private val chatDespawnRegex = Regex("§aYou despawned your .*§a!")

    private var render = -1

    val petDisplayHud = hudElement("PetDisplay", { LocationUtils.inSkyblock && cacheData.getData()["pet"] != null }) { context, example ->
        val text = if (example) "&6Golden Dragon" else cacheData.getData()["pet"].toString()
        if(petDisplay.value) Render2D.drawString(context, text, 0, 0)
        return@hudElement text.width().toFloat() to text.height().toFloat()
    }

    override fun init() {
        register<ChatMessageEvent> {
            if (! LocationUtils.inSkyblock) return@register
            event.formattedText.let { it ->
                if (chatDespawnRegex.matches(it)) {
                    render = -1
                    cacheData.getData().remove("pet")
                    return@register
                }

                val match1 = chatSpawnRegex.find(it)?.destructured?.component1()
                val match3 = chatPetRuleRegex.find(it)?.destructured?.component1()
                if(match3 != null) ChatUtils.showTitle(match3)
                cacheData.getData()["pet"] = match1 ?: match3 ?: return@let
            }
        }

        register<InventoryFullyOpenedEvent> {
            if(!activePetHighlight.value || event.title.unformattedText != "Pets") return@register
            for (item in event.items) {
                for (line in item.value.lore) {
                    if(line.removeFormatting() == "Click to despawn!") {
                        render = item.key
                        cacheData.getData()["pet"] = item.value.hoverName.formattedText.remove(Regex("\\[Lvl .*]"))
                        return@register
                    }
                }
            }
        }

        register<ContainerEvent.Render.Slot.Pre> {
            if(!activePetHighlight.value || event.screen.title.unformattedText != "Pets") return@register
            if(event.slot.index == render) Render2D.drawRect(event.context, event.slot.x, event.slot.y, 16, 16, Color.GREEN)
        }
    }
}