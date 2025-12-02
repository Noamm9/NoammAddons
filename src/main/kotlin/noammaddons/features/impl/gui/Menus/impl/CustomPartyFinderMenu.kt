package noammaddons.features.impl.gui.Menus.impl

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemSkull
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.mc
import noammaddons.events.GuiMouseClickEvent
import noammaddons.events.InventoryFullyOpenedEvent
import noammaddons.features.impl.dungeons.PartyFinder
import noammaddons.features.impl.gui.Menus.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.disableNEUInventoryButtons
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.JsonUtils.getDouble
import noammaddons.utils.JsonUtils.getInt
import noammaddons.utils.JsonUtils.getObj
import noammaddons.utils.LocationUtils.WorldType.*
import noammaddons.utils.LocationUtils.world
import noammaddons.utils.NumbersUtils
import noammaddons.utils.NumbersUtils.romanToDecimal
import noammaddons.utils.NumbersUtils.toFixed
import noammaddons.utils.ProfileUtils
import noammaddons.utils.ProfileUtils.getCatacombsLevel
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.RenderUtils.drawTextWithoutColorLeak
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.remove


object CustomPartyFinderMenu {
    private val partyMembersRegex = Regex(" (.+): (.+) \\(..(\\d+)..\\)")
    private val levelRequiredRegex = Regex("Dungeon Level Required: (\\d+)")
    private val selectedClassRegex = Regex("Currently Selected: (.+)")
    private val selectDungeonClassRegex = Regex("§7View and select a dungeon class\\.")
    private val classNames = listOf("&4&lArcher", "&a&lTank", "&6&lBerserk", "&5&lHealer", "&b&lMage")
    val inPartyFinder get() = currentChestName.removeFormatting() == "Party Finder" && PartyFinder.customMenu.value
    var selectedClass: String? = null

    @SubscribeEvent
    fun guiRender(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (! inPartyFinder) return
        if (! world.equalsOneOf(DungeonHub, Hub)) return
        disableNEUInventoryButtons()
        event.isCanceled = true
        val container = mc.thePlayer?.openContainer?.inventorySlots ?: return

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
            if (stack == null) return@forEach
            if (stack.getItemId().equalsOneOf(160, 262)) return@forEach
            if (i !in 10 until 36 && i != 53) return@forEach

            val x = offsetX + (i % 9) * 18
            val y = offsetY + (i / 9) * 18
            val classes = mutableListOf<String>()
            var levelRequired = 0

            stack.lore.forEach { line ->
                when {
                    line.contains("Dungeon Level Required:") -> levelRequired = levelRequiredRegex.find(line.removeFormatting())?.groupValues?.get(1)?.toInt() ?: 0

                    partyMembersRegex.matches(line) -> classes.add(
                        partyMembersRegex.matchEntire(line)?.destructured?.component2()?.removeFormatting() ?: ""
                    )
                }
            }

            val missingClasses = classNames.filter { classes.indexOf(it.removeFormatting()) == - 1 }.map { it.take(5) }

            val missing = listOf(
                missingClasses.take(2).joinToString(""),
                missingClasses.drop(2).take(2).joinToString("")
            ).filter { it.isNotBlank() }

            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            drawText(if (levelRequired == 0) "" else "&c$levelRequired", 15f - getStringWidth("$levelRequired") * 0.6f, 1f, 0.6f)
            drawText(missing.joinToString("\n"), 1.5f, 10f - if (missing.size == 2) 4.5f else 0f, 0.51f)
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

        var floor = 0
        var type = 'F'

        val lore = item.lore.toMutableList().let { lore ->
            if (item.item !is ItemSkull) return@let lore
            val remainingClasses = classNames.map { it.removeFormatting() }.toMutableList()

            lore.forEachIndexed { index, line ->
                if (PartyFinder.customMenuShowStats.value && line.removeFormatting().contains("Dungeon: Master Mode")) type = 'M'
                if (PartyFinder.customMenuShowStats.value && line.contains("§7Floor: §bFloor ")) floor = line.split(" ").last().let { it.toIntOrNull() ?: it.romanToDecimal() }
                partyMembersRegex.matchEntire(line)?.destructured?.let { (pName, cName, cLvl) ->
                    val playerName = pName.removeFormatting()
                    val className = cName.removeFormatting()
                    val level = cLvl.toInt()
                    val color = getColor(level)
                    val stats = getStats(playerName, floor, type)
                    lore[index] = " $pName: §e$className $color$level $stats"
                    remainingClasses.remove(className)
                }
            }

            if (selectedClass?.removeFormatting() in remainingClasses) {
                val idx = remainingClasses.indexOf(selectedClass?.removeFormatting())
                remainingClasses[idx] = "$selectedClass§7"
            }
            lore.add("§cMissing: §7" + remainingClasses.joinToString(", ") { it.addColor() })

            return@let lore
        }

        updateLastSlot(slotIndex)
        drawLore(item.displayName, lore, mx, my, scale, screenSize)
    }

    @SubscribeEvent
    fun onClick(event: GuiMouseClickEvent) {
        if (! inPartyFinder) return
        if (! world.equalsOneOf(DungeonHub, Hub)) return
        if (! event.button.equalsOneOf(0, 1, 2)) return
        val container = mc.thePlayer?.openContainer?.inventorySlots ?: return

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
            if (stack == null) return
            if (stack.getItemId() == 160 && stack.metadata == 15) return
        }

        handleSlotClick(event.button, slot)
    }

    @SubscribeEvent
    fun invFullOpenEvent(event: InventoryFullyOpenedEvent) {
        if (! PartyFinder.customMenu.value || ! PartyFinder.enabled) return
        if (currentChestName.removeFormatting() != "Catacombs Gate") return
        event.items[45]?.lore?.takeIf { it.size > 3 }.takeIf {
            it?.get(0)?.matches(selectDungeonClassRegex) == true
        }?.run {
            selectedClassRegex.matchEntire(get(2).removeFormatting())?.destructured?.run {
                selectedClass = classNames[classNames.map { it.removeFormatting() }.indexOf(component1())]
            }
        }
    }

    private fun getStats(name: String, floor: Int, type: Char): String {
        if (! PartyFinder.customMenuShowStats.value) return ""
        val key = name.removeFormatting().uppercase()
        if (! ProfileUtils.profileCache.containsKey(key)) {
            Thread { ProfileUtils.getDungeonStats(name) }.start()
            return ""
        }

        val data = ProfileUtils.profileCache[key] ?: return ""
        val dungeons = data.getObj("dungeons")
        val catacombs = dungeons?.getObj("catacombs")
        val master_catacombs = dungeons?.getObj("master_catacombs")

        val totalSecrets = dungeons?.getInt("secrets")?.toDouble() ?: .0
        val totalRuns = dungeons?.getInt("total_runs")?.toDouble() ?: .0
        val secretAvg = (totalSecrets / totalRuns).toFixed(2)

        val cataLvl = dungeons?.getDouble("catacombs_experience")?.let { getCatacombsLevel(it) } ?: "?"
        val pb = ((if (type == 'F') catacombs else master_catacombs)?.getObj("fastest_time_s_plus")
            ?.getInt("$floor")?.let {
                val str = NumbersUtils.formatTime(it)
                str.split(" ").joinToString(":") {
                    if (it.length == 2 && it.contains("s")) ("0" + it.remove("s"))
                    else it.remove("m", "s")
                }
            } ?: "N/A")

        // format style taken from SBD
        return buildString {
            val showSecrets = totalSecrets != .0
            val secrets = if (showSecrets) totalSecrets.toInt() else "?"
            val avg = if (showSecrets) secretAvg else "?"

            append("§b(§6$cataLvl§b)§r")
            append(" §8[§a$secrets§8/§b$avg§8]§r")
            append(" §8[§9$pb§8]§r")
        }
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