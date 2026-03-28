package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.NoammAddons.priceData
import com.github.noamm9.event.impl.ContainerEvent
import com.github.noamm9.event.impl.ContainerFullyOpenedEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.getValue
import com.github.noamm9.ui.clickgui.components.impl.ToggleSetting
import com.github.noamm9.ui.clickgui.components.provideDelegate
import com.github.noamm9.ui.clickgui.components.section
import com.github.noamm9.utils.ChatUtils.formattedText
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.ColorUtils.withAlpha
import com.github.noamm9.utils.DataDownloader
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.NumbersUtils.romanToDecimal
import com.github.noamm9.utils.Utils.equalsOneOf
import com.github.noamm9.utils.Utils.remove
import com.github.noamm9.utils.Utils.startsWithOneOf
import com.github.noamm9.utils.items.ItemUtils
import com.github.noamm9.utils.items.ItemUtils.lore
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.location.WorldType
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.highlight
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color

object ChestProfit: Feature("Dungeon Chest Profit Calculator") {
    private val hud by ToggleSetting("HUD Display", true).section("General")
    private val includeEssence by ToggleSetting("Includes Essence", true)
    private val croesusChestsProfit by ToggleSetting("Croesus Chests Profit", true).section("Croesus")
    private val croesusChestHighlight by ToggleSetting("Highlight Croesus Chests", true)
    private val hideRedChests by ToggleSetting("Hide Opened Chests", true)
    private val croesusKismetDisplay by ToggleSetting("Highlight Rerolled Chests", true)

    private val blackList by lazy { DataDownloader.loadJson<List<String>>("blacklistDrops.json") }

    private val essenceRegex = Regex("§d(?<type>\\w+) Essence §8x(?<count>\\d+)")
    private val croesusChestRegex = Regex("^(Master )?Catacombs - Flo(or (IV|V?I{0,3}))?\$")

    private val chestsToHighlight = mutableListOf<DungeonChest>()
    private var sortedChestsCache = emptyList<DungeonChest>()

    override fun init() {
        register<WorldChangeEvent> {
            DungeonChest.entries.forEach { it.reset() }
            chestsToHighlight.clear()
            sortedChestsCache = emptyList()
        }

        register<ContainerFullyOpenedEvent> {
            if (! LocationUtils.world.equalsOneOf(WorldType.DungeonHub, WorldType.Catacombs)) return@register
            val chestName = event.title.unformattedText
            val currentChest = DungeonChest.getFromName(chestName)

            chestsToHighlight.clear()
            sortedChestsCache = emptyList()

            when {
                currentChest != null -> {
                    val rewardItem = event.items[31] ?: return@register

                    val lore = rewardItem.lore.map { it.removeFormatting() }
                    if ("Cost" !in lore) return@register

                    var profit = - getChestCost(lore)

                    event.items.forEach { (slot, stack) ->
                        if (stack.item == Items.GRAY_STAINED_GLASS_PANE) return@forEach
                        val value = getItemValue(stack)
                        profit += value
                    }

                    currentChest.profit = profit
                    currentChest.openedInSequence = true
                }

                chestName.matches(croesusChestRegex) && croesusChestsProfit.value -> {
                    for (i in 10 .. 16) {
                        val stack = event.items[i] ?: continue
                        if (stack.item == Items.GRAY_STAINED_GLASS_PANE) continue
                        val chestType = DungeonChest.getFromName(stack.hoverName.unformattedText) ?: continue
                        val lore = stack.lore
                        if (lore.last() == "§aAlready opened!") continue

                        val contentIndex = lore.indexOfFirst { it.contains("Contents") }.takeUnless { it == - 1 } ?: continue

                        chestType.slot = i
                        var profit = - getChestCost(lore.map { it.removeFormatting() })

                        lore.drop(contentIndex + 1).takeWhile { it.isNotBlank() }.forEach { line ->
                            val value = if (line.contains("Essence")) getEssenceValue(line)
                            else getIdFromName(line)?.let { getPrice(it) } ?: 0

                            profit += value
                        }

                        chestType.profit = profit
                        chestType.openedInSequence = true

                        if (chestsToHighlight.none { it == chestType }) {
                            chestsToHighlight.add(chestType)
                        }
                        else chestsToHighlight.find { it == chestType }?.profit = profit
                    }

                    sortedChestsCache = chestsToHighlight.sortedByDescending { it.profit }
                }
            }
        }

        register<ContainerEvent.Render.Slot.Pre> {
            if (event.screen !is ContainerScreen) return@register
            if (! LocationUtils.world.equalsOneOf(WorldType.DungeonHub, WorldType.Catacombs)) return@register
            val titleName = event.screen.title.unformattedText

            if (event.slot.index == 0) {
                val width = 176f
                val height = 166f

                DungeonChest.getFromName(titleName)?.let {
                    val color = if (it.profit < 0) "§4" else "§a"
                    val text = "Profit: $color${NumbersUtils.format(it.profit)}  "
                    Render2D.drawString(event.context, text, width - text.width(), 6f)
                } ?: run {
                    if (croesusChestRegex.matches(titleName) && croesusChestsProfit.value) {
                        sortedChestsCache.forEachIndexed { index, chest ->
                            val color = if (chest.profit < 0) "§4" else "§a"
                            val text = "${chest.displayText}: $color${NumbersUtils.format(chest.profit)}§r"

                            Render2D.drawString(
                                event.context,
                                text,
                                width * 1.15f,
                                index * 9f + height / 6f,
                                chest.color
                            )
                        }
                    }
                }
            }

            if (titleName.matches(croesusChestRegex) && croesusChestsProfit.value) {
                sortedChestsCache.take(2).forEachIndexed { index, chest ->
                    if (chest.profit < 0) return@forEachIndexed
                    if (chest.slot != event.slot.index) return@forEachIndexed
                    val color = if (index == 0) Color.GREEN else Color.GREEN.darker().darker().darker()
                    event.slot.highlight(event.context, color)
                }
            }
            else if (titleName == "Croesus") {
                val stack = event.slot.item ?: return@register
                if (stack.item != Items.PLAYER_HEAD) return@register

                val name = stack.hoverName.formattedText
                if (! name.equalsOneOf("§aThe Catacombs", "§aMaster Mode The Catacombs")) return@register
                val lore = stack.lore

                if (croesusChestHighlight.value) {
                    var highlightColor: Color? = null

                    for (line in lore) when {
                        line == "§aNo more chests to open!" -> {
                            if (hideRedChests.value) {
                                event.isCanceled = true
                                return@register
                            }
                            highlightColor = Color.RED
                            break
                        }

                        line == "§cNo chests opened yet!" -> {
                            highlightColor = Color.GREEN
                            break
                        }

                        line.startsWith("§7Opened Chest: ") -> {
                            highlightColor = Color.YELLOW
                            break
                        }
                    }

                    highlightColor?.let { event.slot.highlight(event.context, it.withAlpha(100)) }
                }

                if (croesusKismetDisplay.value && lore[lore.size - 4] != "§5 §9Kismet Feather") {
                    val pose = event.context.pose()
                    pose.pushMatrix()
                    pose.scale(0.7f)
                    pose.translate((event.slot.x + 7) / 0.7f, (event.slot.y + 7) / 0.7f)
                    event.context.renderFakeItem(ItemStack(Items.FEATHER), 0, 0)
                    pose.popMatrix()
                }
            }
        }
    }

    private fun getItemValue(stack: ItemStack): Long {
        val itemName = stack.hoverName.formattedText
        val itemId = stack.skyblockId
        var value = 0L

        if (itemId == "ENCHANTED_BOOK") value += getPrice(enchantNameToID(stack.lore.first()))
        value += getEssenceValue(itemName)
        value += getPrice(itemId)
        if (itemName.contains("Shard")) {
            val cleanName = itemName.removeFormatting().uppercase().remove(" SHARD").replace(" ", "_").remove("_X1")
            val shardId = "SHARD_$cleanName"
            val shardPrice = getPrice(shardId)

            value += shardPrice
        }

        return value
    }

    private fun getChestCost(cleanLore: List<String>): Long {
        return cleanLore.firstNotNullOfOrNull { line ->
            if (line.contains("FREE")) 0L
            else if (line.contains(" Coins")) line.substringBefore(" ").replace(",", "").toLongOrNull()
            else null
        } ?: 0L
    }

    private fun getEssenceValue(text: String): Long {
        if (! includeEssence.value) return 0L
        val match = essenceRegex.find(text) ?: return 0L
        val type = match.groups["type"]?.value?.uppercase() ?: return 0L
        val count = match.groups["count"]?.value?.toLongOrNull() ?: 0L
        return (priceData["ESSENCE_$type"] ?: 0L) * count
    }

    private fun getIdFromName(name: String): String? {
        val cleanName = name.removeFormatting()
        if (cleanName.startsWith("Enchanted Book (")) return enchantNameToID(name.substringAfter("(").substringBefore(")"))
        if (cleanName.contains("Shard")) return "SHARD_${cleanName.removeFormatting().uppercase().remove(" SHARD").replace(" ", "_").remove("_X1")}"
        return ItemUtils.getIdByName(cleanName.remove("Shiny "))
    }

    private fun enchantNameToID(enchant: String): String {
        val enchantName = enchant.substringBeforeLast(" ")
        val name = enchantName.removeFormatting().uppercase().replace(" ", "_")

        val isUltimate = enchantName.startsWithOneOf("§9§d§l", "§d§l", "§7§l")
        val enchantId = if (isUltimate && ! name.contains("ULTIMATE_")) "ULTIMATE_$name" else name

        val levelStr = enchant.substringAfterLast(" ").removeFormatting()
        val level = levelStr.toIntOrNull() ?: levelStr.romanToDecimal()
        return "ENCHANTMENT_${enchantId}_$level"
    }

    private fun getPrice(id: String): Long {
        if (id in blackList) return 0L
        return priceData[id] ?: 0L
    }

    init {
        hudElement(
            name = "Chest Profit",
            enabled = { hud.value },
            shouldDraw = { LocationUtils.world.equalsOneOf(WorldType.DungeonHub, WorldType.Catacombs) }
        ) { ctx, example ->
            val text = if (example) DungeonChest.example
            else {
                val chests = sortedChestsCache.takeIf { it.isNotEmpty() } ?: DungeonChest.entries.filter { it.openedInSequence }.sortedByDescending { it.profit }
                if (chests.isEmpty()) return@hudElement 0f to 0f

                chests.map { chest ->
                    val colorCode = if (chest.profit < 0) "§4" else "§a"
                    val profit = NumbersUtils.format(chest.profit)
                    chest to "${chest.displayText}: $colorCode$profit"
                }
            }

            var maxWidth = 0f
            text.forEachIndexed { i, (chest, str) ->
                Render2D.drawString(ctx, str, 0f, i * 9f, chest.color)
                maxWidth = maxOf(maxWidth, str.width().toFloat())
            }

            maxWidth to text.size * 9f
        }
    }

    enum class DungeonChest(val displayText: String, val color: Color) {
        WOOD("Wood Chest", Color(100, 64, 1)),
        GOLD("Gold Chest", Color.YELLOW),
        DIAMOND("Diamond Chest", Color.CYAN),
        EMERALD("Emerald Chest", Color(0, 128, 0)),
        OBSIDIAN("Obsidian Chest", Color(128, 0, 128)),
        BEDROCK("Bedrock Chest", Color.DARK_GRAY);

        var slot = 0
        var profit = 0L
        var openedInSequence: Boolean = false

        fun reset() {
            slot = 0
            profit = 0
            openedInSequence = false
        }

        companion object {
            fun getFromName(name: String?): DungeonChest? {
                if (name.isNullOrBlank()) return null
                return entries.find {
                    it.displayText.remove(" Chest") == name.remove(" Chest")
                }
            }

            internal val example = listOf(
                WOOD to "Wood Chest: §a75k",
                GOLD to "Gold Chest: §4-62k",
                DIAMOND to "Diamond Chest: §a24k",
                EMERALD to "Emerald Chest: §4-442k",
                OBSIDIAN to "Obsidian Chest: §4-624k",
                BEDROCK to "Bedrock Chest: §a5m"
            )
        }
    }
}