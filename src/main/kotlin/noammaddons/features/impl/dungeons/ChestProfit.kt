package noammaddons.features.impl.dungeons

import gg.essential.elementa.utils.withAlpha
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemSkull
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.*
import noammaddons.features.Feature
import noammaddons.mixins.AccessorGuiContainer
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.ui.config.core.impl.SeperatorSetting
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.GuiUtils.changeTitle
import noammaddons.utils.GuiUtils.currentChestName
import noammaddons.utils.GuiUtils.getSlotFromIndex
import noammaddons.utils.ItemUtils.SkyblockID
import noammaddons.utils.ItemUtils.enchantNameToID
import noammaddons.utils.ItemUtils.getEssenceValue
import noammaddons.utils.ItemUtils.getIdFromName
import noammaddons.utils.ItemUtils.getItemId
import noammaddons.utils.ItemUtils.idToEnchantName
import noammaddons.utils.ItemUtils.lore
import noammaddons.utils.LocationUtils.WorldType.*
import noammaddons.utils.LocationUtils.world
import noammaddons.utils.NumbersUtils.format
import noammaddons.utils.NumbersUtils.romanToDecimal
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderHelper.highlight
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawText
import noammaddons.utils.ThreadUtils.setTimeout
import noammaddons.utils.Utils.equalsOneOf
import noammaddons.utils.Utils.remove
import noammaddons.utils.WebUtils
import java.awt.Color
import java.lang.Math.*

object ChestProfit: Feature("Dungeon Chest Profit Calculator and Croesus Overlay") {
    private val croesusChestRegex = Regex("^(Master Mode )?The Catacombs - Flo(or (IV|V?I{0,3}))?$")
    private val chestsToHighlight = mutableListOf<DungeonChest>()
    private val rngList = mutableListOf<String>()
    private val blackList = mutableListOf<String>()
    private var currentChestProfit = 0
    private var currentChestPrice = 0
    private var newName: String? = null

    private val sortByProfit = ToggleSetting("Sort by Profit")
    val includeEssence = ToggleSetting("Includes Essence")
    private val rng = ToggleSetting("RNG Announcer")

    private val croesusChestsProfit = ToggleSetting("Croesus Chests Profit")
    private val croesusChestHighlight = ToggleSetting("Highlight Croesus Chests")
    private val hideRedChests = ToggleSetting("Hide Opened Chests").addDependency(croesusChestHighlight)
    override fun init() {
        addSettings(
            sortByProfit, includeEssence, rng,
            SeperatorSetting("Croesus"),
            croesusChestsProfit,
            croesusChestHighlight,
            hideRedChests
        )
    }


    init {
        WebUtils.fetchJsonWithRetry<List<String>>("https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/DungeonChestProfit/RNG_BLACKLIST.json") {
            it ?: return@fetchJsonWithRetry
            blackList.clear()
            blackList.addAll(it)
        }

        WebUtils.fetchJsonWithRetry<List<String>>("https://raw.githubusercontent.com/Noamm9/NoammAddons/refs/heads/data/DungeonChestProfit/RNG_List.json") {
            it ?: return@fetchJsonWithRetry
            rngList.clear()
            rngList.addAll(it)
        }
    }

    @SubscribeEvent
    fun onInventory(event: InventoryFullyOpenedEvent) {
        if (! world.equalsOneOf(DungeonHub, Catacombs)) return
        DungeonChest.entries.forEach { it.reset() }
        if (chestsToHighlight.isNotEmpty()) chestsToHighlight.clear()
        newName?.let {
            changeTitle(it)
            newName = null
        }

        when {
            event.title.endsWith(" Chest§r") -> {
                if (event.items[31] == null) return

                val l = event.items[31] !!.lore.map { it.removeFormatting() }
                val i = l.indexOf("Cost")
                if (i == - 1) return

                currentChestPrice = getChestPrice(l)
                currentChestProfit = currentChestPrice * - 1

                for (obj in event.items) {
                    if (obj.value?.getItemId() == 160) continue

                    val itemId = obj.value.SkyblockID ?: continue
                    val itemName = obj.value?.displayName ?: continue
                    val isbook = itemId == "ENCHANTED_BOOK"

                    val bookName = if (isbook) obj.value !!.lore[0] else null
                    val bookID = if (isbook) enchantNameToID(bookName !!) else null

                    val essance = getEssenceValue(itemName)

                    if (isbook) currentChestProfit += getPrice(bookID !!)
                    if (essance != .0) currentChestProfit += essance.toInt()
                    if (! isbook && essance == .0) currentChestProfit += getPrice(itemId)
                }
            }

            event.title.removeFormatting().matches(croesusChestRegex) -> {
                for (i in 10 .. 16) {
                    val item = event.items[i] ?: continue
                    if (item.getItemId() == 160) continue
                    val chestType = DungeonChest.getFromName(item.displayName.removeFormatting()) ?: continue

                    val lore = item.lore
                    val contentIndex = lore.indexOf("§7Contents")
                    if (contentIndex == - 1) continue

                    chestType.slot = i
                    chestType.profit -= getChestPrice(lore)

                    lore.drop(contentIndex + 1).takeWhile { it != "" }.forEach { drop ->
                        val value = when (drop.contains("Essence")) {
                            true -> getEssenceValue(drop).toInt()
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
        if (! world.equalsOneOf(DungeonHub, Catacombs)) return

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

                sortedNumbers.removeAll(toRemove.toSet())
                toHighlight.removeAll(toRemove.toSet())

                (if (sortByProfit.value) sortedNumbers else chestsToHighlight.filterNot { it in toRemove })
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

            currentChestName.endsWith(" Chest§r") && currentChestProfit != 0 -> {
                val profit = "${if (currentChestProfit < 0) "§4" else "§a"}${format(currentChestProfit)}"
                val str = "Profit: $profit  " // added space for padding, yes I am lazy @Noamm9

                drawText(str, width - getStringWidth(str), + 6f)
            }
        }

        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onDrawSlot(event: DrawSlotEvent) {
        if (! croesusChestHighlight.value) return
        if (! world.equalsOneOf(DungeonHub, Catacombs)) return
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
                    if (! hideRedChests.value) Color.RED
                    else {
                        event.isCanceled = true
                        return
                    }


                }

                lore.any { line -> line == "§8No Chests Opened!" } -> Color.GREEN
                lore.any { line -> line.startsWith("§8Opened Chest: ") } -> Color.YELLOW
                else -> return
            }.withAlpha(100)
        )
    }

    @SubscribeEvent
    fun onGuiClick(event: SlotClickEvent) {
        if (! croesusChestsProfit.value) return
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

    private fun getChestPrice(lore: List<String>): Int {
        lore.forEach {
            val line = it.removeFormatting()
            if (line.contains("FREE")) {
                return 0
            }
            if (line.contains(" Coins")) {
                return line.substring(0, line.indexOf(" ")).remove(",").toInt()
            }
        }
        return 0
    }

    private fun getPrice(id: String): Int {
        if (id in blackList) return 0

        val price = when {
            bzData.containsKey(id) -> bzData[id] !!.sellPrice.toInt()
            ahData.containsKey(id) -> ahData[id] !!.toInt()
            else -> 0
        }

        setTimeout(200) { rng(id) }

        return price
    }

    private fun rng(id: String) {
        if (! currentChestName.endsWith(" Chest§r")) return
        if (id !in rngList) return

        val profit = "${if (currentChestProfit < 0) "§4" else "§a"}${format(currentChestProfit)}"
        val str = "&6${itemIdToNameLookup[id] ?: idToEnchantName(id)}&f: $profit"

        if (rng.value) sendPartyMessage("$CHAT_PREFIX $str")
        modMessage(str)

        mc.thePlayer.playSound("note.pling", 50f, 1.22f)
        setTimeout(120) { mc.thePlayer.playSound("note.pling", 50f, 1.13f) }
        setTimeout(240) { mc.thePlayer.playSound("note.pling", 50f, 1.29f) }
        setTimeout(400) { mc.thePlayer.playSound("note.pling", 50f, 1.60f) }


        repeat(70) {
            val multiX = if (random() <= 0.5) - 2 else 2
            val multiZ = if (random() <= 0.5) - 2 else 2
            mc.theWorld.spawnParticle(
                EnumParticleTypes.HEART,
                mc.thePlayer.posX + random() * multiX,
                mc.thePlayer.posY + 0.5 + random(),
                mc.thePlayer.posZ + random() * multiZ,
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
