package noammaddons.features.dungeons.dmap.core

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import noammaddons.config.EditGui.GuiElement
import noammaddons.features.dungeons.MimicDetector
import noammaddons.features.dungeons.dmap.core.map.*
import noammaddons.features.dungeons.dmap.handlers.DungeonInfo
import noammaddons.features.dungeons.dmap.handlers.DungeonMapColorParser
import noammaddons.features.dungeons.dmap.utils.MapRenderUtils
import noammaddons.features.dungeons.dmap.utils.MapUtils
import noammaddons.noammaddons.Companion.MOD_ID
import noammaddons.noammaddons.Companion.hudData
import noammaddons.noammaddons.Companion.mc
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
    private val CheckMarkGreen = ResourceLocation(MOD_ID, "dungeonmap/checkmarks/green_check.png")
    private val CheckMarkWhite = ResourceLocation(MOD_ID, "dungeonmap/checkmarks/white_check.png")
    private val CheckMarkCross = ResourceLocation(MOD_ID, "dungeonmap/checkmarks/cross.png")
    private val CheckMarkQuestion = ResourceLocation(MOD_ID, "dungeonmap/checkmarks/question.png")
    val playerMarker = ResourceLocation(MOD_ID, "dungeonmap/marker.png")
    val checkmarkSize get() = 10.0 * DungeonMapConfig.checkmarkSize

    override val enabled: Boolean get() = DungeonMapConfig.mapEnabled
    override val width: Float get() = 128f
    override val height: Float get() = if (DungeonMapConfig.mapExtraInfo) 140f else 128f

    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(getX(), getY(), 1f)
        GlStateManager.scale(getScale(), getScale(), getScale())

        MapRenderUtils.renderRect(
            0.0, 0.0,
            width.toDouble(), height.toDouble(),
            DungeonMapConfig.mapBackground
        )

        MapRenderUtils.renderRectBorder(
            0.0, 0.0,
            width.toDouble(), height.toDouble(),
            DungeonMapConfig.mapBorderWidth.toDouble(),
            DungeonMapConfig.mapBorderColor
        )

        renderRooms()
        renderText()
        renderPlayerHeads()
        drawExtraInfo()

        GlStateManager.popMatrix()
    }

    private fun renderRooms() {
        if (DungeonMapConfig.dungeonMapCheater) {
            DungeonInfo.dungeonList.forEach {
                if (it.state.equalsOneOf(RoomState.UNOPENED)) {
                    it.state = RoomState.UNDISCOVERED
                }
            }
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)
        val connectorSize = DungeonMapColorParser.quarterRoom

        for (y in 0 .. 10) {
            for (x in 0 .. 10) {
                val tile = DungeonInfo.dungeonList[y * 11 + x]
                if (! DungeonMapConfig.dungeonMapCheater && (tile is Unknown || tile.state == RoomState.UNDISCOVERED)) continue
                val color = if (tile.state == RoomState.UNDISCOVERED) tile.color.darker().darker() else tile.color

                val xOffset = (x shr 1) * (MapUtils.mapRoomSize + connectorSize)
                val yOffset = (y shr 1) * (MapUtils.mapRoomSize + connectorSize)

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
                            (MapUtils.mapRoomSize + connectorSize).toDouble(),
                            (MapUtils.mapRoomSize + connectorSize).toDouble(),
                            color
                        )
                    }

                    else -> drawRoomConnector(
                        xOffset, yOffset, connectorSize, tile is Door, ! xEven, color
                    )
                }

                if (tile !is Room) continue
                if (tile.state != RoomState.UNOPENED) continue
                drawCheckmark(tile, xOffset.toFloat(), yOffset.toFloat(), checkmarkSize)
            }
        }
        GlStateManager.popMatrix()
    }

    private fun renderText() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)

        DungeonInfo.uniqueRooms.forEach { unq ->
            val room = unq.mainRoom
            if (! DungeonMapConfig.dungeonMapCheater && (room.state == RoomState.UNDISCOVERED || room.state == RoomState.UNOPENED)) return@forEach
            val size = MapUtils.mapRoomSize + DungeonMapColorParser.quarterRoom
            val checkPos = unq.getCheckmarkPosition()
            val xOffsetCheck = (checkPos.first / 2f) * size
            val yOffsetCheck = (checkPos.second / 2f) * size
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

            drawCheckmark(room, xOffsetCheck, yOffsetCheck, checkmarkSize)

            if ((DungeonMapConfig.mapRoomNames != 0 && roomType == RoomType.PUZZLE) ||
                (DungeonMapConfig.mapRoomNames >= 2 && roomType.equalsOneOf(RoomType.TRAP, RoomType.CHAMPION, RoomType.FAIRY)) ||
                (DungeonMapConfig.mapRoomNames == 3 && roomType.equalsOneOf(RoomType.NORMAL, RoomType.RARE) || DungeonMapConfig.dungeonMapCheckmarkStyle == 3)
            ) {
                val lines = room.data.name.split(" ")
                var textScale = DungeonMapConfig.textScale

                if (DungeonMapConfig.limitRoomNameSize) {
                    while (lines.maxOf { getStringWidth(it, textScale) } > MapUtils.mapRoomSize) textScale *= 0.99f
                    while (getStringHeight(lines, textScale) > MapUtils.mapRoomSize) textScale *= 0.99f
                }

                MapRenderUtils.renderCenteredText(
                    lines,
                    (xOffsetName + DungeonMapColorParser.halfRoom).toInt(),
                    (yOffsetName + DungeonMapColorParser.halfRoom).toInt(),
                    if (room.data.type != RoomType.ENTRANCE) color else 0xffffff,
                    textScale
                )
            }

            if (room.data.type == RoomType.NORMAL && DungeonMapConfig.dungeonMapCheckmarkStyle == 2/* && secretCount > 0*/)
                MapRenderUtils.renderCenteredText(
                    listOf("$secretCount"),
                    (xOffsetName + DungeonMapColorParser.halfRoom).toInt(),
                    (yOffsetName + 2 + DungeonMapColorParser.halfRoom).toInt(),
                    color, DungeonMapConfig.textScale * 2f
                )

        }
        GlStateManager.popMatrix()
    }

    private fun getCheckmark(state: RoomState) = when (state) {
        RoomState.CLEARED -> CheckMarkWhite
        RoomState.GREEN -> CheckMarkGreen
        RoomState.FAILED -> CheckMarkCross
        RoomState.UNOPENED -> if (! DungeonMapConfig.hideQuestionCheckmarks) CheckMarkQuestion
        else null

        else -> null
    }

    private fun drawCheckmark(tile: Tile, xOffset: Float, yOffset: Float, checkmarkSize: Double) {
        if (DungeonMapConfig.dungeonMapCheckmarkStyle != 1) return

        val room = tile as? Room ?: return
        when (DungeonMapConfig.mapRoomNames) {
            0 -> {}
            1 -> if (room.data.type == RoomType.PUZZLE) return
            2 -> if (room.data.type.equalsOneOf(RoomType.PUZZLE, RoomType.TRAP, RoomType.CHAMPION, RoomType.FAIRY)) return
            3 -> return
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
                checkmarkSize,
                checkmarkSize
            )

            GlStateManager.disableBlend()
        }

    }

    private fun renderPlayerHeads() {
        if (LocationUtils.inBoss) return
        if (DungeonMapConfig.dungeonMapCheater && ! dungeonStarted) {
            DungeonUtils.runPlayersNames.entries.forEach { (name, skin) ->
                if (name == mc.session.username) return@forEach
                val entity = mc.theWorld.getPlayerEntityByName(name) ?: return@forEach

                MapRenderUtils.drawOwnPlayerHead(
                    DungeonUtils.DungeonPlayer(
                        name = name,
                        clazz = DungeonUtils.Classes.Empty,
                        clazzLvl = 50,
                        entity = entity,
                        locationSkin = skin
                    )
                )
            }
        }

        DungeonInfo.playerIcons.values.filterNot { it.teammate.isDead }.forEach { entry ->
            MapRenderUtils.drawPlayerHead(entry)
        }

        if (DungeonMapConfig.dungeonMapCheater) {
            val me = thePlayer ?: DungeonMapPlayer.dummy
            MapRenderUtils.drawPlayerHead(DungeonMapPlayer(me, me.locationSkin))
        }
        else thePlayer?.run {
            if (isDead) return@run
            MapRenderUtils.drawOwnPlayerHead(this)
        }
    }

    private fun drawRoomConnector(x: Int, y: Int, doorWidth: Int, doorway: Boolean, vertical: Boolean, color: Color) {
        val doorwayOffset = if (MapUtils.mapRoomSize == 16) 5 else 6
        val width = if (doorway) 6 else MapUtils.mapRoomSize
        var x1 = if (vertical) x + MapUtils.mapRoomSize else x
        var y1 = if (vertical) y else y + MapUtils.mapRoomSize
        if (doorway) {
            if (vertical) y1 += doorwayOffset else x1 += doorwayOffset
        }
        MapRenderUtils.renderRect(
            x1.toDouble(),
            y1.toDouble(),
            (if (vertical) doorWidth else width).toDouble(),
            (if (vertical) width else doorWidth).toDouble(),
            color
        )
    }

    private fun drawExtraInfo() {
        if (! DungeonMapConfig.mapExtraInfo) return
        if (! DungeonMapConfig.dungeonMapCheater && ! dungeonStarted) return

        GlStateManager.pushMatrix()
        GlStateManager.translate(128f / 2f, 128f, 1f)

        val foundSecrets = TablistListener.secretsFound
        val totalSecrets = maxOf(DungeonInfo.secretCount, TablistListener.secretTotal)
        val crypts = TablistListener.cryptsCount
        val deaths = TablistListener.deathCount
        val deathPenalty = - TablistListener.deathPenalty
        val mimicStatus = if (MimicDetector.mimicKilled.get()) "&a&l✔&r" else "&c&l✖&r"

        val line1 = "&6Secrets: &b$foundSecrets&f/&e$totalSecrets    ${colorCodeByPresent(crypts, 5)}Crypts: $crypts"
        val line2 = "&cDeaths: ${colorCodeByPresent(deaths, 4, true)}$deaths: $deathPenalty&r    &cMimic: $mimicStatus"
        val text = listOf(line1, line2)

        MapRenderUtils.renderCenteredText(text, 0, 3, 0xffffff, 0.7f)
        GlStateManager.popMatrix()
    }
}