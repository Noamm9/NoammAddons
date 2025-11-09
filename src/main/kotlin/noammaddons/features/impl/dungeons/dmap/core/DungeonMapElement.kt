package noammaddons.features.impl.dungeons.dmap.core

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import noammaddons.NoammAddons.Companion.MOD_ID
import noammaddons.NoammAddons.Companion.hudData
import noammaddons.NoammAddons.Companion.mc
import noammaddons.config.EditGui.GuiElement
import noammaddons.features.impl.dungeons.MimicDetector
import noammaddons.features.impl.dungeons.dmap.core.map.*
import noammaddons.features.impl.dungeons.dmap.handlers.*
import noammaddons.features.impl.dungeons.dmap.utils.MapRenderUtils
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils
import noammaddons.utils.*
import noammaddons.utils.DungeonUtils.dungeonStarted
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.RenderHelper.colorCodeByPresent
import noammaddons.utils.RenderHelper.getStringHeight
import noammaddons.utils.RenderHelper.getStringWidth
import noammaddons.utils.Utils.equalsOneOf
import org.lwjgl.opengl.GL11
import java.awt.Color


object DungeonMapElement: GuiElement(hudData.getData().dungeonMap) {
    private val RES_MAP_BACKGROUND = ResourceLocation("textures/map/map_background.png")
    private val CheckMarkGreen = ResourceLocation(MOD_ID, "dungeonmap/checkmarks/green_check.png")
    private val CheckMarkWhite = ResourceLocation(MOD_ID, "dungeonmap/checkmarks/white_check.png")
    private val CheckMarkCross = ResourceLocation(MOD_ID, "dungeonmap/checkmarks/cross.png")
    private val CheckMarkQuestion = ResourceLocation(MOD_ID, "dungeonmap/checkmarks/question.png")
    val playerMarker = ResourceLocation(MOD_ID, "dungeonmap/marker.png")
    private val checkmarkSize get() = 10.0 * DungeonMapConfig.checkmarkSize.value

    override val enabled: Boolean get() = DungeonMapConfig.mapEnabled.value
    override val width: Float get() = 128f
    override val height: Float get() = if (DungeonMapConfig.mapExtraInfo.value) 140f else 128f

    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(getX(), getY(), 1f)
        GlStateManager.scale(getScale(), getScale(), getScale())

        drawMapBackground()
        renderRooms()
        renderCheckmarks()
        renderText()
        renderPlayerHeads()
        drawExtraInfo()

        GlStateManager.popMatrix()
    }


    private fun drawMapBackground() = when (DungeonMapConfig.mapBackgroundStyle.value) {
        0 -> {
            MapRenderUtils.renderRect(
                0.0, 0.0,
                width.toDouble(), height.toDouble(),
                DungeonMapConfig.mapBackground.value
            )

            MapRenderUtils.renderRectBorder(
                0.0, 0.0,
                width.toDouble(), height.toDouble(),
                DungeonMapConfig.mapBorderWidth.value,
                DungeonMapConfig.mapBorderColor.value
            )
        }

        1 -> RenderUtils.drawTexture(RES_MAP_BACKGROUND, - 2.5, - 2.5, width + 5, height + 5)
        2 -> {}
        else -> {}
    }

    private fun renderRooms() {
        GlStateManager.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)

        if (DungeonMapConfig.dungeonMapCheater.value) {
            DungeonInfo.dungeonList.forEach {
                if (it.state == RoomState.UNOPENED) {
                    it.state = RoomState.UNDISCOVERED
                }
            }
        }

        for (y in 0 .. 10) {
            for (x in 0 .. 10) {
                val tile = DungeonInfo.dungeonList[y * 11 + x]
                if (! DungeonMapConfig.dungeonMapCheater.value && (tile is Unknown || tile.state == RoomState.UNDISCOVERED)) continue
                val color = if (tile.state == RoomState.UNDISCOVERED) tile.color.darker().darker() else tile.color

                val xOffset = (x shr 1) * (MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom)
                val yOffset = (y shr 1) * (MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom)

                val xEven = x and 1 == 0
                val yEven = y and 1 == 0

                when {
                    xEven && yEven -> if (tile is Room) {
                        MapRenderUtils.renderRect(
                            xOffset.toDouble(),
                            yOffset.toDouble(),
                            MapUtils.mapRoomSize.toDouble(),
                            MapUtils.mapRoomSize.toDouble(),
                            color
                        )
                    }

                    ! xEven && ! yEven -> {
                        MapRenderUtils.renderRect(
                            xOffset.toDouble(),
                            yOffset.toDouble(),
                            (MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom).toDouble(),
                            (MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom).toDouble(),
                            color
                        )
                    }

                    else -> drawRoomConnector(
                        xOffset, yOffset, HotbarMapColorParser.quarterRoom, tile is Door, ! xEven, color
                    )
                }
            }
        }
    }

    private fun renderCheckmarks() {
        for (y in 0 .. 10) {
            for (x in 0 .. 10) {
                val tile = DungeonInfo.dungeonList[y * 11 + x]
                if (! DungeonMapConfig.dungeonMapCheater.value && (tile is Unknown || tile.state == RoomState.UNDISCOVERED)) continue
                if (tile !is Room) continue

                val xOffset = (x shr 1) * (MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom)
                val yOffset = (y shr 1) * (MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom)
                drawCheckmark(tile, xOffset.toFloat(), yOffset.toFloat(), checkmarkSize)
            }
        }
    }

    private fun renderText() {
        DungeonInfo.uniqueRooms.forEach { unq ->
            val room = unq.mainRoom
            if (! DungeonMapConfig.dungeonMapCheater.value && (room.state == RoomState.UNDISCOVERED || room.state == RoomState.UNOPENED)) return@forEach
            val size = MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom
            val checkPos = unq.getCheckmarkPosition()

            val xOffsetName = (checkPos.first / 2f) * size
            val yOffsetName = (checkPos.second / 2f) * size

            val secretCount = room.data.secrets
            val roomType = room.data.type

            val color = when (room.state) {
                RoomState.GREEN -> 0x55ff55
                RoomState.CLEARED -> 0xffffff
                RoomState.FAILED -> 0xff0000
                else -> 0xaaaaaa
            }

            if ((DungeonMapConfig.mapRoomNames.value != 0 && roomType == RoomType.PUZZLE) ||
                (DungeonMapConfig.mapRoomNames.value >= 2 && roomType.equalsOneOf(RoomType.TRAP, RoomType.CHAMPION, RoomType.FAIRY)) ||
                (DungeonMapConfig.mapRoomNames.value == 3 && roomType.equalsOneOf(RoomType.NORMAL, RoomType.RARE) || DungeonMapConfig.dungeonMapCheckmarkStyle.value == 3)
            ) {
                val lines = room.data.name.split(" ")
                if ("Unknown" in lines) return@forEach
                var textScale = DungeonMapConfig.textScale.value

                if (DungeonMapConfig.limitRoomNameSize.value) {
                    while (lines.maxOf { getStringWidth(it, textScale) } > MapUtils.mapRoomSize) textScale *= 0.99f
                    while (getStringHeight(lines, textScale) > MapUtils.mapRoomSize) textScale *= 0.99f
                }

                MapRenderUtils.renderCenteredText(
                    lines,
                    (xOffsetName + HotbarMapColorParser.halfRoom).toInt(),
                    (yOffsetName + HotbarMapColorParser.halfRoom).toInt(),
                    if (room.data.type != RoomType.ENTRANCE) color else 0xffffff,
                    textScale
                )
            }

            if (room.data.type == RoomType.NORMAL && DungeonMapConfig.dungeonMapCheckmarkStyle.value == 2/* && secretCount > 0*/)
                MapRenderUtils.renderCenteredText(
                    listOf("$secretCount"),
                    (xOffsetName + HotbarMapColorParser.halfRoom).toInt(),
                    (yOffsetName + 1 + HotbarMapColorParser.halfRoom).toInt(),
                    color, DungeonMapConfig.textScale.value * 2f
                )

        }

        GlStateManager.translate(- MapUtils.startCorner.first.toFloat(), - MapUtils.startCorner.second.toFloat(), 0f)
    }


    private fun getCheckmark(state: RoomState) = when (state) {
        RoomState.CLEARED -> CheckMarkWhite
        RoomState.GREEN -> CheckMarkGreen
        RoomState.FAILED -> CheckMarkCross
        RoomState.UNOPENED -> if (DungeonMapConfig.dungeonMapCheater.value) null
        else if (DungeonMapConfig.hideQuestionCheckmarks.value) null
        else CheckMarkQuestion

        else -> null
    }

    private fun drawCheckmark(tile: Tile, xOffset: Float, yOffset: Float, checkmarkSize: Double) {
        if (DungeonMapConfig.dungeonMapCheckmarkStyle.value != 1) return
        val room = tile as? Room ?: return

        when (DungeonMapConfig.mapRoomNames.value) {
            0 -> {}
            1 -> if (room.data.type == RoomType.PUZZLE && room.data.name != "Unknown") return
            2 -> if (room.data.type.equalsOneOf(RoomType.PUZZLE, RoomType.TRAP, RoomType.CHAMPION, RoomType.FAIRY) && room.data.name != "Unknown") return
            3 -> if (room.data.name != "Unknown") return
        }

        getCheckmark(tile.state)?.let {
            GlStateManager.enableBlend()
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GlStateManager.enableAlpha()
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f)

            GlStateManager.color(1f, 1f, 1f, 1f)
            mc.textureManager.bindTexture(it)

            MapRenderUtils.drawTexturedQuad(
                xOffset + (MapUtils.mapRoomSize - checkmarkSize) / 2,
                yOffset + (MapUtils.mapRoomSize - checkmarkSize) / 2,
                checkmarkSize, checkmarkSize
            )

            GlStateManager.disableBlend()
        }

    }

    private fun renderPlayerHeads() {
        if (LocationUtils.inBoss) return
        DungeonUtils.dungeonTeammatesNoSelf.filterNot { it.isDead }.map { it.mapIcon }.forEach(MapRenderUtils::drawPlayerHead)

        if (DungeonMapConfig.dungeonMapCheater.value && ! dungeonStarted) MapRenderUtils.drawPlayerHead(mc.session.username, mc.thePlayer.locationSkin, mc.thePlayer)
        else thePlayer?.mapIcon?.let(MapRenderUtils::drawPlayerHead)

        if (DungeonMapConfig.dungeonMapCheater.value && ! dungeonStarted) {
            DungeonUtils.runPlayersNames.filterNot { (name, _) -> name == mc.session.username }
                .forEach { (name, skin) ->
                    mc.theWorld.getPlayerEntityByName(name)?.let { entity ->
                        MapRenderUtils.drawPlayerHead(name, skin, entity)
                    }
                }
        }
    }

    private fun drawRoomConnector(x: Int, y: Int, doorWidth: Int, doorway: Boolean, vertical: Boolean, color: Color) {
        val doorwayOffset = if (MapUtils.mapRoomSize == 16) 5 else 6
        val width = if (doorway) 6 else MapUtils.mapRoomSize
        var x1 = if (vertical) x + MapUtils.mapRoomSize else x
        var y1 = if (vertical) y else y + MapUtils.mapRoomSize
        if (doorway) if (vertical) y1 += doorwayOffset else x1 += doorwayOffset

        MapRenderUtils.renderRect(
            x1.toDouble(), y1.toDouble(),
            (if (vertical) doorWidth else width).toDouble(),
            (if (vertical) width else doorWidth).toDouble(),
            color
        )
    }

    private fun drawExtraInfo() {
        if (! DungeonMapConfig.mapExtraInfo.value) return
        if (! DungeonMapConfig.dungeonMapCheater.value && ! dungeonStarted) return

        val secretsStr = "&6Secrets: &b${ScoreCalculation.foundSecrets}&f/&e${DungeonInfo.secretCount}"
        val cryptsStr = colorCodeByPresent(ScoreCalculation.cryptsCount.get(), 5) + "Crypts: ${ScoreCalculation.cryptsCount.get()}"
        val scoreStr = "&eScore: ${colorizeScore(ScoreCalculation.score)}&r"
        val deathsStr = "&cDeaths: ${colorCodeByPresent(ScoreCalculation.deathCount, 4, true)}${ScoreCalculation.deathCount}&r"
        val mimicStr = "&cM: ${if (MimicDetector.mimicKilled.get()) "&a&l✔&r" else "&c&l✖&r"}"
        val princeStr = "&eP: ${if (MimicDetector.princeKilled.get()) "&a&l✔&r" else "&c&l✖&r"}"

        val line1 = "$secretsStr    $cryptsStr"
        val line2 = "$scoreStr   $deathsStr   $mimicStr $princeStr"
        val textLines = listOf(line1, line2)

        GlStateManager.translate(width / 2f, 128f, 1f)
        MapRenderUtils.renderCenteredText(textLines, 0, 3, 0xffffff, 0.7f)
    }

    fun colorizeScore(score: Int): String {
        return when {
            score < 270 -> "§c${score}"
            score < 300 -> "§e${score}"
            else -> "§a${score}"
        }
    }
}