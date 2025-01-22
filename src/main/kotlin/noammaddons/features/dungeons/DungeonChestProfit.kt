package noammaddons.features.dungeons

import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemSkull
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.DrawSlotEvent
import noammaddons.events.InventoryFullyOpenedEvent
import noammaddons.events.SlotClickEvent
import noammaddons.features.Feature
import noammaddons.mixins.AccessorGuiContainer
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.GuiUtils.changeTitle
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.getSlotFromIndex
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.JsonUtils.fetchJsonWithRetry
import noammaddons.utils.LocationUtils.WorldName
import noammaddons.utils.LocationUtils.WorldType.Catacombs
import noammaddons.utils.LocationUtils.WorldType.DungeonHub
import noammaddons.utils.NumbersUtils.format
import noammaddons.utils.NumbersUtils.romanToDecimal
import noammaddons.utils.NumbersUtils.toRoman
import noammaddons.utils.PlayerUtils.Player
import noammaddons.utils.RenderHelper.applyAlpha
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.highlight
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import java.awt.Color
import java.lang.Math.random

object DungeonChestProfit: Feature() {
    private val essenceRegex = Regex("§d(?<type>\\w+) Essence §8x(?<count>\\d+)")
    private val croesusChestRegex = Regex("^(Master Mode )?The Catacombs - Flo(or (IV|V?I{0,3}))?$")
    private val chestsToHighlight = mutableListOf<DungeonChest>()
    private val rngList = mutableListOf<String>()
    private val blackList = mutableListOf<String>()
    private var currentChestProfit = 0
    private var currentChestPrice = 0
    private var newName: String? = null

    init {
        fetchJsonWithRetry<List<String>>("https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/DungeonChestProfit/RNG_BLACKLIST.json") {
            it ?: return@fetchJsonWithRetry
            blackList.clear()
            blackList.addAll(it)
        }

        fetchJsonWithRetry<List<String>>("https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/DungeonChestProfit/RNG_List.json") {
            it ?: return@fetchJsonWithRetry
            rngList.clear()
            rngList.addAll(it)
        }
    }

    @SubscribeEvent
    fun onInventory(event: InventoryFullyOpenedEvent) {
        if (! WorldName.equalsOneOf(DungeonHub, Catacombs)) return
        DungeonChest.entries.forEach { it.reset() }
        if (chestsToHighlight.isNotEmpty()) chestsToHighlight.clear()
        newName?.let {
            changeTitle(it)
            newName = null
        }

        when {
            event.title.endsWith(" Chest§r") && config.DungeonChectProfit -> {
                if (event.items[31] == null) return

                val l = event.items[31] !!.lore.map { it.removeFormatting() }
                val i = l.indexOf("Cost")
                if (i == - 1) return

                currentChestPrice = getChestPrice(l)
                currentChestProfit = currentChestPrice * - 1

                for (obj in event.items) {
                    if (obj.value.getItemId() == 160) continue

                    val itemName = obj.value.displayName
                    val itemId = obj.value.SkyblockID
                    val isbook = itemId == "ENCHANTED_BOOK"

                    val bookName = if (isbook) obj.value.lore[0] else null
                    val bookID = if (isbook) enchantNameToID(bookName !!) else null

                    val essance = getEssenceValue(itemName)

                    if (isbook) currentChestProfit += getPrice(bookID !!)
                    if (essance != null) currentChestProfit += essance.toInt()
                    if (! isbook && essance == null) currentChestProfit += getPrice(itemId)
                }
            }

            event.title.removeFormatting().matches(croesusChestRegex) && config.CroesusChestsProfit -> {
                for (i in 10 .. 16) {
                    val item = event.items[i] ?: continue
                    if (item.getItemId() == 160) continue
                    val chestType = DungeonChest.getFromName(item.displayName.removeFormatting()) ?: continue

                    val lore = item.lore
                    val contentIndex = lore.indexOf("§7Contents")
                    if (contentIndex == - 1) continue

                    chestType.slot = i
                    chestType.profit -= getChestPrice(lore).toInt()

                    lore.drop(contentIndex + 1).takeWhile { it != "" }.forEach { drop ->
                        val value = when (drop.contains("Essence")) {
                            true -> getEssenceValue(drop)?.toInt() ?: return@forEach
                            else -> getPrice(getIdFromName(drop) ?: return@forEach)
                        }

                        chestType.profit += value
                    }

                    chestsToHighlight.add(chestType)
                }
            }
        }
    }

    @SubscribeEvent
    fun drawProfit(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (event.gui !is GuiChest) return
        if (! WorldName.equalsOneOf(DungeonHub, Catacombs)) return

        val gui = (event.gui as AccessorGuiContainer)
        val (x, y) = gui.guiLeft.toFloat() to gui.guiTop.toFloat()
        val (width, height) = gui.width.toFloat() to gui.height.toFloat()

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 100f)

        when {
            currentChestName.removeFormatting().matches(croesusChestRegex) -> {
                // sry for horrible code but if it works, don't touch it lol @Noamm9
                val sortedNumbers = chestsToHighlight.sortedByDescending { it.profit }.toMutableList()
                val sortedNumbersV2 = sortedNumbers
                val toHighlight = sortedNumbers.take(2).toMutableList()

                val toRemove = sortedNumbers.filter { dungeonChest ->
                    getSlotFromIndex(dungeonChest.slot)
                        ?.stack?.lore?.any {
                            it.contains("§aAlready opened")
                        } ?: false
                }

                sortedNumbers.removeAll(toRemove)
                toHighlight.removeAll(toRemove)

                (if (config.CroesusChestsProfitSortByProfit) sortedNumbers else chestsToHighlight.filterNot { it in toRemove })
                    .forEachIndexed { index, it ->
                        val str = it.displayText + ": " + (if (it.profit < 0) "§4" else "§a") + format(it.profit) + "§r"

                        drawText(
                            str,
                            width * 1.15f,
                            index * 9f + height / 6f,
                            1f, it.color
                        )
                    }

                if (toRemove.isNotEmpty()) {
                    if (sortedNumbersV2.any { getSlotFromIndex(it.slot)?.stack?.lore?.contains("§cCan't open another chest!") == true }) {
                        changeTitle("", event.gui)
                        drawCenteredText("&4&l&nCan't open another chest!", width / 2f, height / 40)
                    }

                    toRemove.forEach {
                        getSlotFromIndex(it.slot)?.highlight(Color.RED)
                    }
                }

                toHighlight.forEachIndexed { i, chest ->
                    if (chest.profit < 0) return@forEachIndexed
                    getSlotFromIndex(chest.slot)?.highlight(
                        if (i == 0) Color.GREEN
                        else Color.GREEN.darker().darker().darker()
                    )
                }
            }

            currentChestName.endsWith(" Chest§r") && config.DungeonChectProfit && currentChestProfit != 0 -> {
                val profit = "${if (currentChestProfit < 0) "§4" else "§a"}${format(currentChestProfit)}"
                val str = "Profit: $profit  " // added space for padding, yes I am lazy @Noamm9

                drawText(str, width - getStringWidth(str), + 6f)
            }
        }

        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onDrawSlot(event: DrawSlotEvent) {
        if (! config.CroesusChestHighlight) return
        if (! WorldName.equalsOneOf(DungeonHub, Catacombs)) return
        if (event.gui !is GuiChest) return
        if (event.slot.inventory == mc.thePlayer.inventory) return
        val stack = event.slot.stack ?: return
        if (stack.item !is ItemSkull) return
        val name = stack.displayName
        if (! name.equalsOneOf("§cThe Catacombs", "§cMaster Mode The Catacombs")) return
        val lore = stack.lore

        event.slot.highlight(
            when {
                lore.any { line -> line == "§aNo more Chests to open!" } -> {
                    if (config.CroesusChestHighlightHideRedChests) {
                        event.isCanceled = true
                        return
                    }
                    else Color.RED
                }

                lore.any { line -> line == "§8No Chests Opened!" } -> Color.GREEN
                lore.any { line -> line.startsWith("§8Opened Chest: ") } -> Color.YELLOW
                else -> return
            }.applyAlpha(150)
        )
    }

    @SubscribeEvent
    fun onGuiClick(event: SlotClickEvent) {
        if (! config.CroesusChestsProfit) return
        if (event.gui !is GuiChest) return
        if (event.slot?.inventory == mc.thePlayer.inventory) return
        val stack = event.slot?.stack ?: return
        if (stack.item !is ItemSkull) return
        val name = stack.displayName
        if (! name.equalsOneOf("§cThe Catacombs", "§cMaster Mode The Catacombs")) return

        val floor = stack.lore[0].removeFormatting().substringAfterLast(" ").romanToDecimal()
        val title = if (name.startsWith("§cMaster")) "§c§lMaster Mode" else "§a§lFloor"

        newName = "$title §e§l$floor"
    }

    fun getEssenceValue(text: String): Double? {
        //    if (!Skytils.config.dungeonChestProfitIncludesEssence) return null
        val groups = essenceRegex.matchEntire(text)?.groups ?: return null
        val type = groups["type"]?.value?.uppercase() ?: return null
        val count = groups["count"]?.value?.toInt() ?: return null
        return (bzData["ESSENCE_$type"]?.price ?: .0) * count
    }

    fun getIdFromName(name: String): String? {
        return if (name.startsWith("§aEnchanted Book (")) {
            val enchant = name.substring(name.indexOf("(") + 1, name.indexOf(")"))
            return enchantNameToID(enchant)
        }
        else {
            val unformatted = name.removeFormatting().replace("Shiny ", "")
            itemIdToNameLookup.entries.find {
                it.value == unformatted && ! it.key.contains("STARRED")
            }?.key
        }
    }

    fun getChestPrice(lore: List<String>): Int {
        lore.forEach {
            val line = it.removeFormatting()
            if (line.contains("FREE")) {
                return 0
            }
            if (line.contains(" Coins")) {
                return line.substring(0, line.indexOf(" ")).replace(",", "").toInt()
            }
        }
        return 0
    }

    fun enchantNameToID(enchant: String): String {
        val enchantName = enchant.substringBeforeLast(" ")
        val name = enchantName.removeFormatting().uppercase().replace(" ", "_")
        val enchantId = if (enchantName.startsWith("§9§d§l") || enchantName.startsWith("§d§l")) {
            if (! name.contains("ULTIMATE_")) "ULTIMATE_$name"
            else name
        }
        else name

        val level = enchant.substringAfterLast(" ").removeFormatting().run {
            toIntOrNull() ?: romanToDecimal()
        }
        return "ENCHANTMENT_${enchantId}_$level"
    }

    fun idToEnchantName(enchantId: String): String {
        val parts = enchantId.split("_")
        if (parts.size < 3 || parts[0] != "ENCHANTMENT") throw IllegalArgumentException("Invalid enchantment ID format")

        val isUltimate = parts[1] == "ULTIMATE"
        val enchantNameParts = if (isUltimate) parts.drop(2).dropLast(1) else parts.drop(1).dropLast(1)
        val enchantLevel = parts.last().toIntOrNull() ?: throw IllegalArgumentException("Invalid level in enchantment ID")

        val enchantName = enchantNameParts.joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
        }

        val romanLevel = enchantLevel.toRoman()
        val formattedName = if (isUltimate) "§d§l$enchantName $romanLevel§a" else "$enchantName $romanLevel§a"

        return formattedName
    }

    fun getPrice(id: String): Int {
        if (id in blackList) return 0

        val price = when {
            bzData.containsKey(id) -> bzData[id] !!.price.toInt()
            ahData.containsKey(id) -> ahData[id] !!.toInt()
            else -> 0
        }

        setTimeout(200) { rng(id) }

        return price
    }

    fun rng(id: String) {
        if (! currentChestName.endsWith(" Chest§r")) return
        if (id !in rngList) return

        val profit = "${if (currentChestProfit < 0) "§4" else "§a"}${format(currentChestProfit)}"
        val str = "&6${itemIdToNameLookup[id] ?: idToEnchantName(id)}&f: $profit"

        if (config.RNGDropAnnouncer) sendPartyMessage(str)
        modMessage(str)

        Player.playSound("note.pling", 50f, 1.22f)
        setTimeout(120) { Player.playSound("note.pling", 50f, 1.13f) }
        setTimeout(240) { Player.playSound("note.pling", 50f, 1.29f) }
        setTimeout(400) { Player.playSound("note.pling", 50f, 1.60f) }


        repeat(70) {
            val multiX = if (random() <= 0.5) - 2 else 2
            val multiZ = if (random() <= 0.5) - 2 else 2
            mc.theWorld.spawnParticle(
                EnumParticleTypes.HEART,
                Player.posX + random() * multiX,
                Player.posY + 0.5 + random(),
                Player.posZ + random() * multiZ,
                0.0, 1.0, 0.0
            )
        }
    }

    private enum class DungeonChest(var displayText: String, var color: Color) {
        WOOD("Wood Chest", Color(100, 64, 1)),
        GOLD("Gold Chest", Color.YELLOW),
        DIAMOND("Diamond Chest", Color.CYAN),
        EMERALD("Emerald Chest", Color(0, 128, 0)),
        OBSIDIAN("Obsidian Chest", Color(128, 0, 128)),
        BEDROCK("Bedrock Chest", Color.DARK_GRAY);

        var slot = 0
        var profit = 0

        fun reset() {
            slot = 0
            profit = 0
        }

        companion object {
            fun getFromName(name: String?): DungeonChest? {
                if (name.isNullOrBlank()) return null
                return entries.find {
                    it.displayText == name
                }
            }
        }
    }
}