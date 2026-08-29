package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.ColorSetting
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.features.annotations.AlwaysActive
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.remove
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.Render2D.highlight
import com.github.noamm9.utils.render.RenderHelper.height
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.inventory.ContainerInput
import java.awt.Color

@AlwaysActive
object PetDisplay: Feature("Pet Features") {
    private val petDisplay by ToggleSetting("Pet Display").withDescription("Draws the current active pet on screen.").section("HUD")
    private val autoPetTitles by ToggleSetting("Auto Pet Title").withDescription("Shows a title on screen when you swap pets via autopet rules.")
    private val autoPetTitlesDungeonOnly by ToggleSetting("Dungeons Only").withDescription("Only shows autopet titles while in a dungeon.").showIf { autoPetTitles.value }

    private val activePetHighlight by ToggleSetting("Highlight Active pet").withDescription("highlights the active pet inside the pet menu").section("Pets Menu")
    private val petHighlightColor by ColorSetting("Highlight color", Color.CYAN).showIf { activePetHighlight.value }

    private val chatPetRuleRegex = Regex("§cAutopet §eequipped your §7\\[Lvl .*] (?<pet>.*)§e! §a§lVIEW RULE")
    private val chatSpawnRegex = Regex("§aYou summoned your (?<pet>.*)§a!")
    private val chatDespawnRegex = Regex("§aYou despawned your .*§a!")
    private val loadoutsPetRegex = Regex("\\[Lvl (\\d+)] (.+)$")
    private val loadoutSlots = setOf(14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43)

    private val petMenuRegex = Regex("^(\\(\\d/\\d\\) )?Pets$")
    private val petLevelRegex = Regex(".+\\[Lvl .*]")
    private var selectedPetSlot = - 1
    private var autoPetTitle = ""
    private var autoPetTitleTicks = 0

    override fun init() {
        hudElement(
            "PetDisplay",
            enabled = { petDisplay.value },
            shouldDraw = { LocationUtils.inSkyblock && cacheData.get()["pet"] != null }) { context, example ->
            val text = if (example) "&6Golden Dragon" else cacheData.get()["pet"].toString()
            context.drawString(text, 0, 0)
            return@hudElement text.width().toFloat() to text.height().toFloat()
        }

        hudElement(
            "Auto Pet Title",
            enabled = { autoPetTitles.value },
            shouldDraw = { ticker.isActive },
            centered = true
        ) { context, example ->
            val text = if (example) "&6Golden Dragon" else autoPetTitle
            context.drawCenteredString(text, 0, 0)
            return@hudElement text.width().toFloat() to text.height().toFloat()
        }.apply {
            scale = 2.5f
        }

        register<ChatMessageEvent> {
            val msg = event.formattedText
            if (chatDespawnRegex.matches(msg)) {
                cacheData.get().remove("pet")
                selectedPetSlot = - 1
                return@register
            }

            val match1 = chatSpawnRegex.find(msg)?.destructured?.component1()
            val match2 = chatPetRuleRegex.find(msg)?.destructured?.component1()

            if (match2 != null && autoPetTitles.value && enabled && (! autoPetTitlesDungeonOnly.value || LocationUtils.inDungeon)) {
                autoPetTitle = match2
                autoPetTitleTicks = 40
                ticker.register()
            }

            cacheData.get()["pet"] = match1 ?: match2 ?: return@register
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

        register<ContainerEvent.SlotClick> {
            if (event.button != 0) return@register
            if (event.clickType != ContainerInput.PICKUP) return@register
            if (event.slotId !in loadoutSlots) return@register
            if (! event.screen.title.unformattedText.endsWith(") Loadouts")) return@register
            player.containerMenu.items[event.slotId].lore.find { it.startsWith("§7Pet: ") }?.let {
                cacheData.get()["pet"] = loadoutsPetRegex.find(it)?.destructured?.component2() ?: return@let
            }
        }

        register<ContainerEvent.Render.Slot.Pre> {
            if (! enabled) return@register
            if (! activePetHighlight.value) return@register
            if (event.slot.index != selectedPetSlot) return@register
            event.slot.highlight(event.context, petHighlightColor.value, 1)
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

    private val ticker = EventBus.listener<TickEvent.Start> {
        autoPetTitleTicks --
        if (autoPetTitleTicks <= 0) listener.unregister()
    }
}