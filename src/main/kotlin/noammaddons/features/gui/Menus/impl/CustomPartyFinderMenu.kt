package noammaddons.features.gui.Menus.impl

import gg.essential.universal.UGraphics.getStringWidth
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemSkull
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.GuiContainerEvent
import noammaddons.features.Feature
import noammaddons.features.gui.Menus.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.LocationUtils.WorldName
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.RenderUtils.drawTextWithoutColorLeak
import noammaddons.utils.RenderUtils.drawWithNoLeak
import noammaddons.utils.ThreadUtils.loop
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.isNull


object CustomPartyFinderMenu: Feature() {
    private val partyMembersRegex = Regex(" (.+): (.+) \\(..(\\d+)..\\)")
    private val levelRequiredRegex = Regex("Dungeon Level Required: (\\d+)")
    private val selectedClassRegex = Regex("Currently Selected: (.+)")
    private val selectDungeonClassRegex = Regex("§7View and select a dungeon class\\.")
    private val classNames = listOf("&4&lArcher", "&a&lTank", "&6&lBerserk", "&5&lHealer", "&b&lMage")
    val inPartyFinder get() = currentChestName.removeFormatting() == "Party Finder" && config.CustomSBMenus
    var selectedClass: String? = null

    @SubscribeEvent
    fun guiRender(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inPartyFinder) return
        if (! WorldName.equalsOneOf("Hub", "Dungeon Hub")) return
        event.isCanceled = true
        val container = Player?.openContainer?.inventorySlots ?: return

        val scale = calculateScale()
        val (mx, my) = getMouseScaledCoordinates(scale)
        val screenSize = getScreenSize(scale)
        val windowSize = container.size - 36

        val (offsetX, offsetY, width, height) = calculateOffsets(screenSize, windowSize)
        val slotPosition = calculateSlotPosition(mx, my, offsetX, offsetY)

        GlStateManager.pushMatrix()
        GlStateManager.scale(scale, scale, scale)

        renderBackground(offsetX, offsetY, width, height, backgroundColor)
        drawTextWithoutColorLeak("&6&l&n[&b&l&nN&d&l&nA&6&l&n]&r &b&lParty Finder", offsetX, offsetY)

        container.forEach { slot ->
            val stack = slot.stack
            val i = slot.slotNumber
            if (i >= windowSize) return@forEach
            if (stack.isNull()) return@forEach
            if (stack.getItemId().equalsOneOf(160, 262)) return@forEach
            if (i !in 10 until 36 && i != 53) return@forEach

            val x = offsetX + (i % 9) * 18
            val y = offsetY + (i / 9) * 18
            val classes = mutableListOf<String>()
            var levelRequired = 0

            stack.lore.forEach { line ->
                when {
                    levelRequiredRegex.matches(line.removeFormatting()) -> levelRequired = levelRequiredRegex.find(
                        line.removeFormatting()
                    )?.groupValues?.get(1)?.toInt() ?: 0

                    partyMembersRegex.matches(line) -> classes.add(
                        partyMembersRegex.matchEntire(line)?.destructured?.component2()?.removeFormatting() ?: ""
                    )
                }
            }

            val missingClasses = classNames
                .filter { name -> classes.indexOf(name.removeFormatting()) == - 1 }
                .map { it.take(5) }

            val missingOffsetY = if (missingClasses.size >= 3) mc.fontRendererObj.FONT_HEIGHT * 2 else mc.fontRendererObj.FONT_HEIGHT
            val missingStr = missingClasses.take(2).joinToString("")
            val p2 = missingClasses.drop(2).take(2).joinToString("")
            val missing = """
			$missingStr
			$p2
			""".trimIndent()
            val TextScale = 0.60f

            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)

            drawWithNoLeak {
                drawText(
                    if (levelRequired == 0) "" else "&c$levelRequired",
                    15f - getStringWidth("$levelRequired") * TextScale,
                    1f, TextScale
                )

                drawText(
                    missing,
                    1f,
                    13 - (missingOffsetY * TextScale) - mc.fontRendererObj.FONT_HEIGHT * TextScale / 2f,
                    TextScale
                )
            }
            GlStateManager.popMatrix()
        }

        renderHeads(container, windowSize, offsetX, offsetY, slotPosition, 0)
        renderItems(container, windowSize, offsetX, offsetY, 0)

        GlStateManager.popMatrix()

        if (! isValidSlot(slotPosition.first, slotPosition.second)) return
        val slotIndex = getSlotIndex(slotPosition.first, slotPosition.second)
        if (slotIndex >= windowSize) return

        val item = container[slotIndex]?.stack ?: return
        if (item.getItemId() == 160 && item.metadata == 15) return
        val lore = toolTipHandler(item) ?: item.lore

        updateLastSlot(slotIndex)
        drawLore(item.displayName, lore, mx, my, scale, screenSize)
    }

    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.GuiMouseClickEvent) {
        if (! inPartyFinder) return
        if (! event.button.equalsOneOf(0, 1, 2)) return
        val container = Player?.openContainer?.inventorySlots ?: return
        event.isCanceled = true

        val scale = calculateScale()
        val (x, y) = getMouseScaledCoordinates(scale)
        val screenSize = getScreenSize(scale)
        val windowSize = container.size - 36

        val (offsetX, offsetY, _, _) = calculateOffsets(screenSize, windowSize)
        val slotPosition = calculateSlotPosition(x, y, offsetX, offsetY)

        if (! isValidSlot(slotPosition.first, slotPosition.second)) return
        val slot = getSlotIndex(slotPosition.first, slotPosition.second)

        if (slot >= windowSize) return
        container[slot].run {
            if (stack.isNull()) return
            if (stack.getItemId() == 160 && stack.metadata == 15) return
        }

        handleSlotClick(event.button, slot)
    }

    init {
        loop(100) {
            if (! config.CustomSBMenus) return@loop
            if (currentChestName.removeFormatting() != "Catacombs Gate") return@loop
            val lore = Player?.openContainer?.inventorySlots?.get(45)?.stack?.lore ?: return@loop
            if (lore.size <= 3) return@loop
            if (! lore[0].matches(selectDungeonClassRegex)) return@loop

            selectedClassRegex.matchEntire(lore[2].removeFormatting())?.destructured?.run {
                selectedClass = classNames[classNames.map { it.removeFormatting() }.indexOf(component1())]
            }

        }
    }

    private fun toolTipHandler(item: ItemStack): List<String>? {
        val classNames = classNames.map { it.removeFormatting() }.toMutableList()
        val toolTip = item.lore.toMutableList()

        toolTip.forEachIndexed { index, line ->
            if (! line.matches(partyMembersRegex)) return@forEachIndexed

            partyMembersRegex.matchEntire(line) !!.destructured.run {
                val playerName = component1()
                val className = component2().removeFormatting()
                val level = component3().toInt()
                val color = getColor(level)
                toolTip[index] = " $playerName: §e$className $color$level"
                classNames.remove(className)
            }
        }

        if (item.item !is ItemSkull) return null

        if (selectedClass.removeFormatting() in classNames) classNames[classNames.indexOf(selectedClass.removeFormatting())] = "$selectedClass§7"
        toolTip.add("")
        toolTip.add("§cMissing: §7" + classNames.joinToString(", ") { it.addColor() })

        return toolTip
    }

    private fun getColor(level: Int): String = when {
        level >= 50 -> "§c§l"
        level >= 45 -> "§c"
        level >= 40 -> "§6"
        level >= 35 -> "§d"
        level >= 30 -> "§9"
        level >= 25 -> "§b"
        level >= 20 -> "§2"
        level >= 15 -> "§a"
        level >= 10 -> "§e"
        level >= 5 -> "§f"
        else -> "§7"
    }
}