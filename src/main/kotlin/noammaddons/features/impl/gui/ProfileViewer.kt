package noammaddons.features.impl.gui

import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import kotlinx.serialization.json.*
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import noammaddons.features.Feature
import noammaddons.features.impl.gui.Menus.drawLore
import noammaddons.features.impl.gui.Menus.getScreenSize
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.debugMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.JsonUtils.getDouble
import noammaddons.utils.JsonUtils.getInt
import noammaddons.utils.JsonUtils.getObj
import noammaddons.utils.JsonUtils.getString
import noammaddons.utils.MouseUtils.isMouseOver
import noammaddons.utils.NumbersUtils.format
import noammaddons.utils.NumbersUtils.toRoman
import noammaddons.utils.RenderHelper.getScaleFactor
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.RenderUtils.drawCenteredText
import noammaddons.utils.RenderUtils.drawFloatingRect
import noammaddons.utils.RenderUtils.drawLine
import noammaddons.utils.RenderUtils.drawText
import java.awt.Color
import java.util.*
import kotlin.math.roundToInt


// yes, it's a mess
object ProfileViewer: Feature() {
    val profileCache = mutableMapOf<String, Pair<PlayerData, Long>>()
    val categories = listOf("&bSkills", "&bDungeons", "&bInventory", "&bPets", "&bMisc")
    var currentCategory = categories[0]
    var tooltipLore = listOf<String>()
    var shouldDrawToolTip = false

    val data = PlayerData()


    class ProfleViewerGUI: GuiScreen() {
        override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
            val scale = 4f / mc.getScaleFactor()
            val (screenWidth, screenHeight) = getScreenSize(scale)
            val (rawMouseX, rawMouseY) = mouseX.toFloat() / scale to mouseY.toFloat() / scale

            val boxWidth = screenWidth * 0.7f
            val boxHeight = boxWidth / 2f
            val x = (screenWidth - boxWidth) / 2f
            val y = (screenHeight - boxHeight) / 2f

            GlStateManager.pushMatrix()
            GlStateManager.scale(scale, scale, scale)

            drawFloatingRect(x - 3, y - 3, boxWidth + 6, boxHeight + 6, Color(27, 27, 27, 190))
            drawLine(Color.WHITE, x, y + 10f, x + boxWidth, y + 10f)

            categories.forEachIndexed { i, str ->
                val sectionWidth = boxWidth / categories.size
                val textWidth = getStringWidth(str, 0.85f)
                val centeredX = x + (i * sectionWidth) + (sectionWidth - textWidth) / 2
                val categoryStr = if (currentCategory == str) "&d${str.removeFormatting()}" else str
                drawText(categoryStr, centeredX, y + 1f, 0.85f)
            }

            drawCenteredText("${data.rank} ${data.name}", x + (boxWidth / 7), y + boxHeight / 8, .85f)

            data.entityOtherPlayerMP?.let {
                val skinX = (x + boxWidth / 7).roundToInt()
                val skinY = (y + boxHeight - boxHeight / 3.5).roundToInt()

                GlStateManager.enableDepth()
                GuiInventory.drawEntityOnScreen(
                    skinX, skinY, 40,
                    - rawMouseX + skinX,
                    - rawMouseY + skinY - 72,
                    it
                )
                GlStateManager.disableDepth()
            }

            val skycryptData = data.skyCryptData ?: return GlStateManager.popMatrix()

            val profileName = skycryptData.getString("cute_name") ?: "Unknown"
            val profileMode = skycryptData.getString("game_mode") ?: "Unknown"
            val joinDate = skycryptData.getObj("data")?.getObj("user_data")?.getObj("first_join")?.getString("text") ?: "Unknown"

            drawText("&6Joined: &b$joinDate&r", x + boxWidth / 25, y + boxHeight / 12 + 12 * 11.4f, 0.75f)
            drawText("&6Profile: &b$profileName&r", x + boxWidth / 25, y + boxHeight / 12 + 12 * 10f, 0.75f)
            drawText("&6Mode: &b${upperCaseFirst(profileMode)}&r", x + boxWidth / 25, y + boxHeight / 12 + 12 * 10.7f, 0.75f)

            when (currentCategory.removeFormatting()) {
                "Skills" -> {
                    val skillObj = skycryptData.getObj("data")?.getObj("skills")?.getObj("skills")
                    val slayerObj = skycryptData.getObj("data")?.getObj("slayer")

                    skillObj?.entries?.forEachIndexed { i, (name, obj_) ->
                        val obj = obj_.jsonObject
                        val skillname = colorSkill(upperCaseFirst(name))
                        val lvl = colorSkillLVL(obj.getInt("level") ?: 0)
                        val skillStr = "$skillname $lvl&r".addColor()
                        val xp = format(obj.getDouble("xp") ?: 0.0)
                        val rank = formatCommas(obj.getInt("rank") ?: 0)

                        val isHovered = isMouseOver(
                            rawMouseX - x, rawMouseY - y,
                            boxWidth / 3.0, boxHeight / 8.0 + (i * 12),
                            getStringWidth(skillStr, 0.85f), 12
                        )

                        if (isHovered) {
                            shouldDrawToolTip = true
                            tooltipLore = listOf(
                                "&bXP: &d$xp",
                                "&bRank: &f#&d$rank"
                            ).map { it.addColor() }
                        }

                        drawText(skillStr, x + boxWidth / 3, y + boxHeight / 8 + (i * 12), 0.85f)
                    }

                    slayerObj?.getObj("slayers")?.entries?.forEachIndexed { i, (name, obj_) ->
                        val obj = obj_.jsonObject
                        val lvlObj = obj.getObj("level")

                        val slayerName = "&5${upperCaseFirst(name)}".addColor()
                        val slayerLevel = "&3${lvlObj?.getInt("currentLevel")}"
                        val slayerStr = "$slayerName $slayerLevel&r"
                        val xp = lvlObj?.getInt("xp") ?: 0
                        val xpForNext = lvlObj?.getInt("xpForNext") ?: 0
                        val missingXP = xpForNext - xp
                        val expValues = getExpValues()

                        drawText(slayerStr, x + boxWidth - (boxWidth / 3), y + boxHeight / 8 + (i * 12), 0.85f)

                        val isSlayerHoverd = isMouseOver(
                            rawMouseX - x, rawMouseY - y,
                            boxWidth - (boxWidth / 3.0),
                            boxHeight / 8.0 + (i * 12),
                            getStringWidth(slayerStr, 0.85f), 12
                        )

                        if (isSlayerHoverd) {
                            val lore = mutableListOf(
                                "&bSlayer XP: &d${formatCommas(xp)}",
                                "&bNext Level In: ",
                                "  &d${formatCommas(missingXP / expValues[0])} T5 Bosses",
                                "  &d${formatCommas(missingXP / expValues[1])} T4 Bosses",
                                "  &d${formatCommas(missingXP / expValues[2])} T3 Bosses",
                                "  &d${formatCommas(missingXP / expValues[3])} T2 Bosses",
                                "  &d${formatCommas(missingXP / expValues[4])} T1 Bosses",
                            )
                            if (i != 0) lore.remove(lore[2])
                            if (slayerLevel.removeFormatting().toInt() == lvlObj?.getInt("maxLevel")) {
                                val a = lore[0]
                                lore.clear()
                                lore.add(a)
                                lore.add("&b&lMax Level&r")
                            }

                            tooltipLore = lore.map { it.addColor() }
                            shouldDrawToolTip = true
                        }

                    }

                    val totalSlayerXpStr = "&bTotal Slayer XP: &d${format(slayerObj?.getInt("total_slayer_xp") ?: 0)}"
                    val totalCoinsSpentStr = "&bTotal Coins Spent: &6${format(slayerObj?.getInt("total_coins_spent") ?: 0)}"

                    drawText(totalSlayerXpStr, x + boxWidth - (boxWidth / 3), y + boxHeight / 8 + (7 * 12), 0.85f)
                    drawText(totalCoinsSpentStr, x + boxWidth - (boxWidth / 3), y + boxHeight / 8 + (8 * 12), 0.85f)

                    val isCoinsHovered = isMouseOver(
                        rawMouseX, rawMouseY,
                        x + boxWidth - (boxWidth / 3.0),
                        y + boxHeight / 8.0 + (8 * 12),
                        getStringWidth(totalCoinsSpentStr, 0.85f), (9 * .85f).toInt()
                    )

                    if (isCoinsHovered) {
                        shouldDrawToolTip = true
                        val lore = mutableListOf<String>()

                        slayerObj?.getObj("slayers")?.entries?.forEach { (name, obj_) ->
                            val obj = obj_.jsonObject
                            val coinsSpent = obj.getInt("coins_spent") ?: 0
                            val slayerName = "&5${upperCaseFirst(name)}"
                            val coinsSpentStr = "&b: &6${format(coinsSpent)}"
                            lore.add("$slayerName$coinsSpentStr&r".addColor())
                        }
                        tooltipLore = lore
                    }

                    val isSlayersHovered = isMouseOver(
                        rawMouseX, rawMouseY,
                        x + boxWidth - (boxWidth / 3.0),
                        y + boxHeight / 8.0 + (7 * 12),
                        getStringWidth(totalSlayerXpStr, 0.85f), (9 * .85f).toInt()
                    )

                    if (isSlayersHovered) {
                        shouldDrawToolTip = true
                        val lore = mutableListOf<String>()
                        val slayerEntries = slayerObj?.getObj("slayers")?.entries
                        val lastIndex = slayerEntries?.size?.minus(1)

                        slayerEntries?.forEachIndexed { index, (name, obj_) ->
                            val slayerName = "&5${upperCaseFirst(name)}"
                            lore.add("$slayerName Bosses Killed:&r".addColor())

                            obj_.jsonObject.getObj("kills")?.entries?.sortedBy { it.key }?.forEach { (tier_, count_) ->
                                val tier = if (tier_ != "total") tier_.toInt().toRoman() else "Total"
                                val count = count_.jsonPrimitive.int
                                if (tier != "Total") lore.add(" &b$tier: &e${formatCommas(count)}&r".addColor())
                                else lore.add("&6Total: &e${formatCommas(count)}&r".addColor())
                            }

                            if (index != lastIndex) lore.add("")
                        }

                        tooltipLore = lore
                    }

                    val weightStr = "&bLilyWeight Weight: &6${formatCommas(data.lilyWeight?.getObj("summary")?.getDouble("total")?.toInt() ?: 0)}"
                    drawText(weightStr, x + boxWidth - (boxWidth / 3), y + boxHeight / 8 + (9 * 12), 0.85f)

                    val isWeightHovered = isMouseOver(
                        rawMouseX, rawMouseY,
                        x + boxWidth - (boxWidth / 3.0),
                        y + boxHeight / 8.0 + (9 * 12),
                        getStringWidth(weightStr, 0.85f), (9 * .85f).toInt()
                    )

                    if (isWeightHovered) {
                        shouldDrawToolTip = true
                        val lore = mutableListOf<String>()
                        val weightEntries = data.lilyWeight?.getObj("summary")?.entries

                        weightEntries?.forEach { (name, obj_) ->
                            val weightName = "&5${upperCaseFirst(name)}"
                            val num = obj_.jsonPrimitive.double
                            val str = "$weightName&b: &6${formatCommas(num.toInt())}"
                            lore.add(str.addColor())
                        }

                        tooltipLore = lore
                    }
                }

                "Dungeons" -> {}
                "Inventory" -> {}
                "Pets" -> {}
                "Misc" -> {}
            }

            GlStateManager.popMatrix()

            if (shouldDrawToolTip) drawLore("", tooltipLore, rawMouseX, rawMouseY, scale, Pair(screenWidth, screenHeight))
            tooltipLore = listOf()
            shouldDrawToolTip = false
        }

        override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
            val scale = 4 / ProfileViewer.mc.getScaleFactor().toFloat()
            val (screenWidth, screenHeight) = getScreenSize(scale)
            val (rawMouseX, rawMouseY) = mouseX / scale to mouseY / scale

            val boxWidth = screenWidth * 0.7f
            val boxHeight = boxWidth / 2f
            val x = (screenWidth - boxWidth) / 2f
            val y = (screenHeight - boxHeight) / 2f

            if (mouseButton == 0) {
                categories.forEachIndexed { i, str ->
                    if (str == currentCategory) return@forEachIndexed
                    val sectionWidth = boxWidth / categories.size
                    if (isMouseOver(rawMouseX, rawMouseY, x + (i * sectionWidth), y, sectionWidth, 10)) {
                        currentCategory = str
                        SoundUtils.click()
                        debugMessage("Selected category: $str")
                    }
                }
            }
        }

        override fun doesGuiPauseGame(): Boolean {
            return false
        }

        override fun onGuiClosed() {
            tooltipLore = emptyList()
            shouldDrawToolTip = false
            data.name = ""
            data.rank = ""
            data.entityOtherPlayerMP = null
            data.skyCryptData = null
            data.lilyWeight = null
            currentCategory = categories[0]
        }

        override fun initGui() {
            tooltipLore = emptyList()
            shouldDrawToolTip = false
            currentCategory = categories[0]
        }
    }


    fun getExpValues(): List<Int> {
        mayorData.let {
            if ((it.mayor.perks + it.minister.perk).any { perk -> perk.name == "Slayer XP Buff" }) {
                return listOf(1850, 625, 125, 37, 7)
            }
        }

        return listOf(1500, 500, 100, 25, 5)
    }

    fun formatCommas(number: Number): String = number.toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")

    fun upperCaseFirst(str: String) = str[0].uppercase() + str.substring(1)

    fun colorSkillLVL(lvl: Int): String = when (lvl) {
        in 0 .. 29 -> "&a$lvl&r"
        in 30 .. 39 -> "&e$lvl&r"
        in 40 .. 49 -> "&c$lvl&r"
        in 50 .. 60 -> "&c&l$lvl&r"
        else -> "&f$lvl&r"
    }

    fun colorSkill(skill: String): String = when (skill) {
        "Taming" -> "&d$skill"
        "Fishing" -> "&b$skill"
        "Farming" -> "&e$skill"
        "Combat" -> "&c$skill"
        "Mining" -> "&7$skill"
        "Alchemy" -> "&9$skill"
        "Foraging" -> "&2$skill"
        "Enchanting" -> "&a$skill"
        "Runecrafting" -> "&5$skill"
        else -> "&f$skill"
    }

    fun createFakePlayer(name: String): EntityOtherPlayerMP {
        val uuid = UUID.fromString((ProfileUtils.getUUID(name)))
        val gameProfile = mc.sessionService.fillProfileProperties(GameProfile(uuid, name), true)

        var skinLocation: ResourceLocation? = null
        var skinType: String? = null

        val fakeEntity = object: EntityOtherPlayerMP(mc.theWorld, gameProfile) {
            override fun getLocationSkin(): ResourceLocation {
                return skinLocation ?: ResourceLocation("textures/entity/steve.png")
            }

            override fun getSkinType(): String {
                return skinType ?: "default"
            }
        }

        mc.skinManager.loadProfileTextures(
            fakeEntity.gameProfile, { type, location, profileTexture ->
                if (type != MinecraftProfileTexture.Type.SKIN) return@loadProfileTextures
                skinLocation = location
                skinType = profileTexture.getMetadata("model")
            },
            false
        )

        return fakeEntity
    }
}