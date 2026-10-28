package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.utils.ColorUtils.colorCodeByPercent
import com.github.noamm9.utils.ColorUtils.colorizeScore
import com.github.noamm9.utils.ColorUtils.lerp
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.dungeons.map.core.*
import com.github.noamm9.utils.dungeons.map.handlers.*
import com.github.noamm9.utils.dungeons.map.utils.MapUtils
import com.github.noamm9.utils.equalsOneOf
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawBorder
import com.github.noamm9.utils.render.Render2D.drawCenteredString
import com.github.noamm9.utils.render.Render2D.drawPlayerHead
import com.github.noamm9.utils.render.Render2D.drawRect
import com.github.noamm9.utils.render.Render2D.drawTexture
import com.github.noamm9.utils.render.RenderHelper.renderVec
import gg.essential.universal.UGraphics
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import java.awt.Color

object MapRenderer: HudElement() {
    override val name = "Dungeon Map"
    override val toggle get() = DungeonMap.enabled && MapConfig.mapEnabled.value
    override val shouldDraw get() = LocationUtils.inDungeon && (! LocationUtils.inBoss || ! MapConfig.mapHideInBoss.value)

    private val checkmarkGreen = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/dungeonmap/checkmarks/green_check.png")
    private val checkmarkWhite = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/dungeonmap/checkmarks/white_check.png")
    private val checkmarkUnknown = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/dungeonmap/checkmarks/question.png")
    private val checkmarkFail = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/dungeonmap/checkmarks/cross.png")
    private val ownPlayerMarker = Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/dungeonmap/marker.png")

    override fun draw(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Float, Float> = draw(ctx, example, extraInfo = true)

    fun draw(ctx: GuiGraphicsExtractor, example: Boolean, extraInfo: Boolean): Pair<Float, Float> {
        val startX = MapUtils.startCorner.first.toFloat().let { if (it == 5f) it + 1f else it }
        val startY = MapUtils.startCorner.second.toFloat().let { if (it == 5f) it + 1f else it }

        renderBackground(ctx)
        ctx.pose().translate(startX, startY)

        if (example) {
            ctx.renderRooms(MapPreview.tiles, MapPreview.CONNECTOR_SIZE, true)
            ctx.renderText(MapPreview.rooms, MapPreview.CONNECTOR_SIZE, MapUtils.mapRoomSize / 2, true)
        }
        else {
            applyCheater()
            ctx.renderRooms(DungeonScanner.dungeonList, HotbarMapScanner.quarterRoom.takeUnless { it == - 1 } ?: 4, false)
            ctx.renderText(DungeonScanner.uniqueRooms.values, HotbarMapScanner.quarterRoom, HotbarMapScanner.halfRoom, false)
        }

        ctx.pose().translate(- startX, - startY)
        if (example) MapPreview.players.forEachIndexed { i, player -> ctx.drawPlayerHead(player, isSelf = i == 0) }
        else ctx.renderPlayerHeads()
        if (extraInfo) renderExtraInfo(ctx, example)

        return 128f to if (MapConfig.mapExtraInfo.value) 140f else 128f
    }

    private fun renderBackground(ctx: GuiGraphicsExtractor) {
        val width = 128
        val height = if (MapConfig.mapExtraInfo.value) 140f else 128f

        ctx.drawRect(0, 0, width, height, MapConfig.mapBackground.value)
        ctx.drawBorder(0, 0, width, height, MapConfig.mapBorderColor.value, MapConfig.mapBorderWidth.value)
    }

    private fun renderExtraInfo(ctx: GuiGraphicsExtractor, example: Boolean) {
        if (! MapConfig.mapExtraInfo.value) return
        if (! example && ! MapConfig.dungeonMapCheater.value && ! DungeonListener.dungeonStarted) return

        val secretsStr = "&6Secrets: &b${ScoreTracker.foundSecrets}&f/&b${ScoreTracker.secretsUntil300}&f/&e${DungeonScanner.secretCount}"
        val cryptsStr = colorCodeByPercent(ScoreTracker.cryptsCount, 6) + "C${ScoreTracker.cryptsCount}"
        val batsStr = (if (ScoreTracker.batCount > 0) "&aB" else "&cB") + ScoreTracker.batCount
        val scoreStr = "&eScore: ${colorizeScore(ScoreTracker.score)}&r"
        val deathsStr = "&cDeaths: ${colorCodeByPercent(ScoreTracker.deathCount, 4, true)}${ScoreTracker.deathCount}&r"
        val bonusStr = buildString {
            append(if (ScoreTracker.mimicKilled) "&aM &f| " else "&cM &f| ")
            append(if (ScoreTracker.princeKilled) "&aP" else "&cP")
        }

        val line1 = "$secretsStr    $cryptsStr &f| $batsStr"
        val line2 = "$scoreStr   $deathsStr   $bonusStr"

        ctx.pose().translate(64f, 128f)
        ctx.drawCenteredString(line1, 0f, - 4f, scale = 0.7f)
        ctx.drawCenteredString(line2, 0f, 2f, scale = 0.7f)
    }

    private fun applyCheater() {
        if (! MapConfig.dungeonMapCheater.value) return
        DungeonScanner.dungeonList.forEach { tile ->
            if (tile.state == RoomState.UNOPENED) tile.state = RoomState.UNDISCOVERED
        }
    }

    private fun getDoorState(door: DoorTile): RoomState {
        if (door.roomTileIndices.size != 2) return RoomState.UNDISCOVERED
        if (door.roomTiles.any { it.state == RoomState.UNDISCOVERED }) return RoomState.UNDISCOVERED
        return RoomState.UNOPENED
    }

    private fun GuiGraphicsExtractor.renderRooms(tiles: Array<Tile>, connectorSize: Int, preview: Boolean) {
        for (y in 0 .. 10) for (x in 0 .. 10) {
            val tile = tiles[y * 11 + x].takeUnless { it is Unknown } ?: continue
            if (tile.state == RoomState.UNDISCOVERED && ! MapConfig.dungeonMapCheater.value) continue
            if (tile is DoorTile && getDoorState(tile) == RoomState.UNDISCOVERED && ! MapConfig.dungeonMapCheater.value) continue

            var color = tile.getColor()
            if (MapConfig.dungeonMapCheater.value && tile.state == RoomState.UNDISCOVERED) {
                color = color.lerp(MapConfig.colorUndiscovered.value, MapConfig.undiscoveredTint.value / 100f)
            }

            //#if CHEAT
            if (MapConfig.dungeonMapCheater.value && MapConfig.highlightMimicRoom.value && tile is RoomTile && tile.uniqueRoom?.hasMimic == true) {
                color = color.lerp(MapConfig.colorMimic.value, 0.2)
            }
            //#endif

            val xOffset = (x shr 1) * (MapUtils.mapRoomSize + connectorSize)
            val yOffset = (y shr 1) * (MapUtils.mapRoomSize + connectorSize)

            val xEven = x and 1 == 0
            val yEven = y and 1 == 0
            val predicted = getPredictedTypes(tile, y * 11 + x, preview)

            when {
                xEven && yEven -> if (predicted != null) drawPredictedRoom(xOffset, yOffset, predicted)
                else if (tile is RoomTile) drawRect(xOffset, yOffset, MapUtils.mapRoomSize, MapUtils.mapRoomSize, color)

                ! xEven && ! yEven -> drawRect(xOffset, yOffset, MapUtils.mapRoomSize + connectorSize, MapUtils.mapRoomSize + connectorSize, color)

                else -> drawRoomConnector(xOffset, yOffset, connectorSize, tile is DoorTile, ! xEven, color)
            }

            if (tile is RoomTile && tile.data.isUnknown() && predicted == null) {
                drawRoomCheckmark(tile, xOffset, yOffset)
            }
        }
    }

    private fun getPredictedTypes(tile: Tile, index: Int, preview: Boolean): List<RoomType>? {
        if (! MapConfig.predictRooms.value || MapConfig.dungeonMapCheater.value) return null
        if (tile !is RoomTile || tile.state != RoomState.UNOPENED) return null
        if (preview) return MapPreview.predictions[index]
        return SpecialColumn.predict(index)
    }

    private fun GuiGraphicsExtractor.drawPredictedRoom(x: Int, y: Int, types: List<RoomType>) {
        val roomSize = MapUtils.mapRoomSize
        val colors = types.map {
            when (it) {
                RoomType.CHAMPION -> MapConfig.colorMiniboss
                RoomType.TRAP -> MapConfig.colorTrap
                else -> MapConfig.colorPuzzle
            }.value
        }.let { colors ->
            colors.map { if (colors.size > 1) it.darker().darker() else it }
        }

        pose().pushMatrix()
        pose().translate(x.toFloat(), y.toFloat())

        when (colors.size) {
            1 -> drawRect(0, 0, roomSize, roomSize, colors[0])
            2 -> {
                drawRect(0, 0, roomSize, roomSize / 2, colors[0])
                drawRect(0, roomSize / 2, roomSize, roomSize - roomSize / 2, colors[1])
            }

            3 -> {
                val outer = roomSize * 5 / 16
                val center = roomSize - outer
                val inner = center - 1

                drawRect(0, 0, roomSize, outer, colors[0])
                drawRect(0, 0, outer, inner, colors[0])
                drawRect(inner, outer, roomSize - inner, roomSize - outer, colors[1])
                drawRect(0, inner, roomSize, roomSize - inner, colors[1])
                drawRect(outer, outer, center - outer, center - outer, colors[2])
            }
        }

        pose().popMatrix()
    }

    private fun GuiGraphicsExtractor.renderText(rooms: Collection<UniqueRoom>, connectorSize: Int, halfRoomSize: Int, preview: Boolean) {
        val roomSize = MapUtils.mapRoomSize.toFloat()
        val gapSize = connectorSize.toFloat()
        val halfRoom = halfRoomSize.toFloat()
        val fullCellSize = roomSize + gapSize

        rooms.forEach { unq ->
            if (unq.data.isUnknown()) return@forEach
            if (MapConfig.hideBloodroomOvelay.value && unq.data.type == RoomType.BLOOD) return@forEach
            val unopenedTile = unq.tiles.find { ! it.isSeparator && it.state == RoomState.UNOPENED }
            val roomTile = unq.mainRoom

            if (! MapConfig.dungeonMapCheater.value && unopenedTile != null) {
                val (row, column) = unopenedTile.getGridPos()
                if (getPredictedTypes(unopenedTile, row * 11 + column, preview) != null) return@forEach

                val xOffset = (column shr 1) * (MapUtils.mapRoomSize + connectorSize)
                val yOffset = (row shr 1) * (MapUtils.mapRoomSize + connectorSize)
                drawRoomCheckmark(unopenedTile, xOffset, yOffset)
                return@forEach
            }

            if (unq.data.type.equalsOneOf(RoomType.ENTRANCE, RoomType.FAIRY)) return@forEach
            if (! MapConfig.dungeonMapCheater.value && roomTile.state == RoomState.UNDISCOVERED) return@forEach

            val checkPos = unq.getCheckmarkPosition()
            val cX = (checkPos.first / 2f) * fullCellSize + halfRoom
            val cY = (checkPos.second / 2f) * fullCellSize + halfRoom

            val color = when (roomTile.state) {
                RoomState.GREEN -> Color(85, 255, 85)
                RoomState.FAILED -> Color(255, 0, 0)
                RoomState.CLEARED -> Color(255, 255, 255)
                else -> Color(170, 170, 170)
            }

            val checkmarkStyle = when (MapConfig.dungeonMapCheckmarkStyle.value) {
                4 -> if (roomTile.data.type == RoomType.PUZZLE || roomTile.data.secrets == 0) 2 else 1
                else -> MapConfig.dungeonMapCheckmarkStyle.value
            }

            when (checkmarkStyle) {
                2, 3 -> {
                    var scale = MapConfig.textScale.value.toFloat()
                    val showSecrets = checkmarkStyle == 3 && roomTile.data.secrets > 0

                    if (MapConfig.limitRoomNameSize.value) {
                        unq.updateBounds(roomSize, gapSize)
                        val secretsText = if (showSecrets) "${unq.foundSecrets}/${roomTile.data.secrets}" else ""
                        val maxLineW = unq.updateTextScale(1f, showSecrets, secretsText)
                        var totalH = unq.cacheSplitName.size * UGraphics.getFontHeight().toFloat()
                        if (showSecrets) totalH += UGraphics.getFontHeight()

                        if (maxLineW > 0 && totalH > 0) {
                            val sW = unq.cachedMaxWidth / maxLineW
                            val sH = unq.cachedMaxHeight / totalH
                            scale = sW.coerceAtMost(sH).coerceIn(0.39f, MapConfig.textScale.value.toFloat())
                        }
                    }

                    val totalLines = unq.cacheSplitName.size + (if (showSecrets) 1 else 0)
                    val totalH = totalLines * UGraphics.getFontHeight() * scale

                    var currentY = cY - totalH / 2

                    for (line in unq.cacheSplitName) {
                        drawCenteredString(line, cX, currentY, color, scale)
                        currentY += totalH / totalLines
                    }

                    if (showSecrets) {
                        val secStr = "${unq.foundSecrets}/${roomTile.data.secrets}"
                        drawCenteredString(secStr, cX, currentY, color, scale)
                    }
                }

                1 -> drawCenteredString(
                    if (roomTile.data.secrets == 0) "0" else "${unq.foundSecrets}/${roomTile.data.secrets}",
                    cX,
                    cY - UGraphics.getFontHeight() / 2,
                    color,
                    MapConfig.textScale.value
                )

                0 -> {
                    val checkmarkSize = MapConfig.checkmarkSize.value * 10
                    drawCheckmark(roomTile, cX - checkmarkSize / 2, cY - checkmarkSize / 2, checkmarkSize)
                }
            }
        }
    }

    private fun GuiGraphicsExtractor.renderPlayerHeads() {
        if (LocationUtils.inBoss) return

        DungeonListener.dungeonTeammatesNoSelf.forEach { player ->
            if (player.isDead) return@forEach
            drawPlayerHead(player)
        }

        drawPlayerHead(DungeonListener.thePlayer ?: return)
    }

    private fun GuiGraphicsExtractor.drawCheckmark(tile: Tile, x: Number, y: Number, size: Number) {
        val checkmark = when (tile.state) {
            RoomState.CLEARED -> checkmarkWhite
            RoomState.GREEN -> checkmarkGreen
            RoomState.FAILED -> checkmarkFail
            RoomState.UNOPENED if ! MapConfig.hideQuestionCheckmarks.value -> checkmarkUnknown
            else -> return
        }

        drawTexture(checkmark, x, y, size, size)
    }

    private fun GuiGraphicsExtractor.drawRoomCheckmark(tile: Tile, xOffset: Int, yOffset: Int) {
        val checkmarkSize = MapConfig.checkmarkSize.value * 10
        drawCheckmark(
            tile,
            xOffset + MapUtils.mapRoomSize / 2 - checkmarkSize / 2,
            yOffset + MapUtils.mapRoomSize / 2 - checkmarkSize / 2,
            checkmarkSize
        )
    }

    private fun GuiGraphicsExtractor.drawPlayerHead(teammate: DungeonPlayer, isSelf: Boolean = teammate == DungeonListener.thePlayer) {
        val entity = teammate.entity

        val (x, z, yaw) = if (entity == null || ! entity.isAlive) {
            Triple(teammate.mapX, teammate.mapZ, teammate.yaw)
        }
        else {
            val (mx, mz) = MapUtils.coordsToMap(entity.renderVec)
            Triple(mx, mz, entity.yRot)
        }

        val borderColor = if (MapConfig.mapPlayerHeadColorClassBased.value) teammate.clazz.color
        else MapConfig.mapPlayerHeadColor.value

        val nameColor = if (MapConfig.mapPlayerNameClassColorBased.value && teammate.clazz != DungeonClass.Empty) teammate.clazz.color
        else Color.WHITE

        pose().pushMatrix()
        pose().translate(x, z)
        val currentYaw = MathUtils.normalizeYaw(yaw)
        val headYaw = Math.toRadians((currentYaw + 180).toDouble()).toFloat()

        pose().rotate(headYaw)
        pose().scale(MapConfig.playerHeadScale.value)

        if (MapConfig.mapVanillaMarker.value && isSelf) {
            drawTexture(ownPlayerMarker, - 6, - 6, 12, 12, MapConfig.mapVanillaMarkerColor.value)
        }
        else {
            drawBorder(- 7, - 7, 14, 14, borderColor)
            drawPlayerHead(- 6, - 6, 12, teammate.skin)
        }

        val heldItem = mc.player?.mainHandItem
        val shouldDrawName = MapConfig.playerNames.value == 2 || (MapConfig.playerNames.value == 1
            && (heldItem != null && (heldItem.skyblockId == "SPIRIT_LEAP" || heldItem.skyblockId == "INFINITE_SPIRIT_LEAP"
            || heldItem.skyblockId == "HAUNT_ABILITY")))

        if (shouldDrawName) {
            pose().rotate(- headYaw)
            pose().translate(0f, 8f)
            pose().scale(MapConfig.playerNameScale.value)
            drawCenteredString(teammate.name, 0, 0, nameColor)
        }

        pose().popMatrix()
    }

    private fun GuiGraphicsExtractor.drawRoomConnector(x: Int, y: Int, doorWidth: Int, doorway: Boolean, vertical: Boolean, color: Color) {
        val doorHeightSize = MapConfig.doorHeightSize.value
        val doorwayOffset = (MapUtils.mapRoomSize - doorHeightSize) / 2
        val doorHeight = if (doorway) doorHeightSize else MapUtils.mapRoomSize
        var x1 = if (vertical) x + MapUtils.mapRoomSize else x
        var y1 = if (vertical) y else y + MapUtils.mapRoomSize
        if (doorway) if (vertical) y1 += doorwayOffset else x1 += doorwayOffset

        drawRect(
            x1.toDouble(),
            y1.toDouble(),
            (if (vertical) doorWidth else doorHeight).toDouble(),
            (if (vertical) doorHeight else doorWidth).toDouble(),
            color
        )
    }
}