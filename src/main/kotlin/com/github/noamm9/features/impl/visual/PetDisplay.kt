package com.github.noamm9.features.impl.visual

import com.github.noamm9.config.types.*
import com.github.noamm9.event.impl.*
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.PetUtils
import com.github.noamm9.utils.items.ItemRarity
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.ItemRenderer
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawString
import com.github.noamm9.utils.render.Render2D.highlight
import com.github.noamm9.utils.render.RenderHelper.height
import com.github.noamm9.utils.render.RenderHelper.width
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import java.awt.Color

object PetDisplay: Feature("Pet Features") {
    private val petDisplay by ToggleSetting("Pet Display").withDescription("Draws the current active pet on screen.").section("HUD")
    private val petInfo by MultiCheckboxSetting("Display Options", mutableMapOf(
        "Pet Name" to true,
        "Pet Level" to true,
        "Pet Item" to true,
        "Pet Skin" to true,
    ))

    private val autoPetTitles by ToggleSetting("Auto Pet Title").withDescription("Shows a title on screen when you swap pets via autopet rules.")
    private val autoPetTitlesDungeonOnly by ToggleSetting("Dungeons Only").withDescription("Only shows autopet titles while in a dungeon.").showIf { autoPetTitles.value }

    private val activePetHighlight by ToggleSetting("Highlight Active pet").withDescription("highlights the active pet inside the pet menu").section("Pets Menu")
    private val petHighlightColor by ColorSetting("Highlight color", Color.CYAN).showIf { activePetHighlight.value }

    private val petMenuRegex = Regex("^(\\(\\d/\\d\\) )?Pets$")
    private var selectedPetSlot = - 1
    private var autoPetTitle = ""
    private var autoPetTitleUntil = 0L

    override fun init() {
        hudElement(
            enabled = { petDisplay.value },
            shouldDraw = { LocationUtils.inSkyblock && PetUtils.currentPet != null }
        ) { context, example ->
            val pet = if (example) examplePet else PetUtils.currentPet ?: return@hudElement 0 to 0

            if (petInfo["Pet Skin"]) {
                ItemRenderer.drawBatchedItemStack(context, pet.head(), 0, 0, 1.3f)
                ItemRenderer.endItemRendererBatch(context)
            }

            val lines = listOfNotNull(
                if (petInfo["Pet Name"]) pet.formattedName(withLevel = petInfo["Pet Level"]) else null,
                if (petInfo["Pet Item"]) pet.heldItem?.let { "&6Item: &5$it" } else null
            )

            val x = if (petInfo["Pet Skin"]) 16 else 0
            val padding = if (petInfo["Pet Skin"]) 3.5f else 0f

            var width = x + padding
            var height = if (petInfo["Pet Skin"] && lines.size == 1) 5f else 0f

            for (line in lines) {
                context.drawString(line, x + padding, height)
                width = maxOf(width, line.width() + x + padding)
                height += 9
            }

            height = maxOf(height, x + padding)

            return@hudElement width to height
        }

        hudElement(
            "Auto Pet Title",
            enabled = { autoPetTitles.value },
            shouldDraw = { autoPetTitle.isNotBlank() && System.currentTimeMillis() < autoPetTitleUntil },
            centered = { true }
        ) { context, example ->
            val text = if (example) "&6Golden Dragon" else autoPetTitle
            context.drawCenteredString(text, 0, 0)
            return@hudElement text.width() to text.height()
        } defaults {
            scale = 2.5f
        }

        register<PetEvent.Change> {
            if (event.cause != PetEvent.Cause.AUTOPET) return@register
            val pet = event.pet ?: return@register

            if (autoPetTitles.value && (! autoPetTitlesDungeonOnly.value || LocationUtils.inDungeon)) {
                autoPetTitle = buildString {
                    append(pet.rarity?.baseColor?.toString() ?: "§f")
                    append(pet.name)
                }
                autoPetTitleUntil = System.currentTimeMillis() + 2000L
            }
        }

        register<ContainerFullyOpenedEvent> {
            selectedPetSlot = - 1
            if (! event.title.unformattedText.matches(petMenuRegex)) return@register
            for ((i, stack) in event.items) {
                val lore = stack.lore
                if (lore.getOrNull(lore.lastIndex - 2)?.removeFormatting() != "Click to despawn!") continue
                selectedPetSlot = i
                return@register
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

    private val examplePet by lazy {
        PetUtils.Pet(
            name = "Golden Dragon",
            level = 200,
            rarity = ItemRarity.LEGENDARY,
            heldItem = "HEPHAESTUS_REMEDIES",
            headSkin = "ewogICJ0aW1lc3RhbXAiIDogMTYyMDM1MDA5ODgyNiwKICAicHJvZmlsZUlkIiA6ICJiNWRkZTVmODJlYjM0OTkzYmMwN2Q0MGFiNWY2ODYyMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJsdXhlbWFuIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJlOWY5YjFmYzAxNDE2NmNiNDZhMDkzZTUzNDliMmJmNmVkZDIwMWI2ODBkNjJlNDhkYmYzYWY5YjA0NTkxMTYiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ",
        )
    }
}