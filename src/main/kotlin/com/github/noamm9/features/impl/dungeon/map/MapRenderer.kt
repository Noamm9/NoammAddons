package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.utils.ColorUtils.colorCodeByPercent
import com.github.noamm9.utils.ColorUtils.colorizeScore
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.dungeons.map.core.*
import com.github.noamm9.utils.dungeons.map.handlers.DungeonScanner
import com.github.noamm9.utils.dungeons.map.handlers.HotbarMapScanner
import com.github.noamm9.utils.dungeons.map.handlers.ScoreCalculation
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

    override fun draw(ctx: GuiGraphicsExtractor, example: Boolean): Pair<Float, Float> = draw(ctx, example, true)

    fun draw(ctx: GuiGraphicsExtractor, example: Boolean, extraInfo: Boolean): Pair<Float, Float> {
        renderBackground(ctx)
        ctx.pose().translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat())
        applyCheater()
        renderRooms(ctx)
        renderText(ctx)
        ctx.pose().translate(- MapUtils.startCorner.first.toFloat(), - MapUtils.startCorner.second.toFloat())
        renderPlayerHeads(ctx)
        if (extraInfo) renderExtraInfo(ctx)

        return 128f to if (MapConfig.mapExtraInfo.value) 140f else 128f
    }

    private fun renderBackground(ctx: GuiGraphicsExtractor) {
        val width = 128
        val height = if (MapConfig.mapExtraInfo.value) 140f else 128f

        ctx.drawRect(0, 0, width, height, MapConfig.mapBackground.value)
        ctx.drawBorder(0, 0, width, height, MapConfig.mapBorderColor.value, MapConfig.mapBorderWidth.value)
    }

    private fun renderExtraInfo(ctx: GuiGraphicsExtractor) {
        if (! MapConfig.mapExtraInfo.value) return
        if (! MapConfig.dungeonMapCheater.value && ! DungeonListener.dungeonStarted) return

        val secretsStr = "&6Secrets: &b${ScoreCalculation.foundSecrets}&f/&e${DungeonScanner.secretCount}"
        val cryptsStr = colorCodeByPercent(ScoreCalculation.cryptsCount, 6) + "Crypts: ${ScoreCalculation.cryptsCount}"
        val scoreStr = "&eScore: ${colorizeScore(ScoreCalculation.score)}&r"
        val deathsStr = "&cDeaths: ${colorCodeByPercent(ScoreCalculation.deathCount, 4, true)}${ScoreCalculation.deathCount}&r"
        val bonusStr = buildString {
            append(if (ScoreCalculation.mimicKilled) "&aM &f| " else "&cM &f| ")
            append(if (ScoreCalculation.princeKilled) "&aP &f| " else "&cP &f| ")
            append(if (ScoreCalculation.batKilled) "&aB" else "&cB")
        }

        val line1 = "$secretsStr    $cryptsStr"
        val line2 = "$scoreStr   $deathsStr   $bonusStr"

        ctx.pose().translate(width / 2f, 128f)
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

    private fun renderRooms(ctx: GuiGraphicsExtractor) {
        val connectorSize = (HotbarMapScanner.quarterRoom.takeUnless { it == - 1 } ?: 4)

        for (y in 0 .. 10) for (x in 0 .. 10) {
            val tile = DungeonScanner.dungeonList[y * 11 + x].takeUnless { it is Unknown } ?: continue
            if (tile.state == RoomState.UNDISCOVERED && ! MapConfig.dungeonMapCheater.value) continue
            if (tile is DoorTile && getDoorState(tile) == RoomState.UNDISCOVERED && ! MapConfig.dungeonMapCheater.value) continue

            var color = tile.getColor()
            if (MapConfig.dungeonMapCheater.value && tile.state == RoomState.UNDISCOVERED) {
                color = color.darker().darker()
            }

            if (tile is RoomTile && tile.uniqueRoom?.hasMimic == true && MapConfig.highlightMimicRoom.value && NoammAddons.isCheat) {
                color = MathUtils.lerpColor(color, MapConfig.colorMimic.value, 0.2)
            }

            val xOffset = (x shr 1) * (MapUtils.mapRoomSize + connectorSize)
            val yOffset = (y shr 1) * (MapUtils.mapRoomSize + connectorSize)

            val xEven = x and 1 == 0
            val yEven = y and 1 == 0

            when {
                xEven && yEven -> if (tile is RoomTile) {
                    ctx.drawRect(
                        xOffset,
                        yOffset, MapUtils.mapRoomSize,
                        MapUtils.mapRoomSize,
                        color
                    )
                }

                ! xEven && ! yEven -> {
                    ctx.drawRect(
                        xOffset,
                        yOffset, MapUtils.mapRoomSize + connectorSize,
                        MapUtils.mapRoomSize + connectorSize,
                        color
                    )
                }

                else -> drawRoomConnector(
                    ctx, xOffset, yOffset, connectorSize, tile is DoorTile, ! xEven, color
                )
            }

            if (tile is RoomTile && tile.data.isUnknown()) {
                val checkmarkSize = MapConfig.checkmarkSize.value * 10
                drawCheckmark(
                    ctx, tile,
                    xOffset + MapUtils.mapRoomSize / 2 - checkmarkSize / 2,
                    yOffset + MapUtils.mapRoomSize / 2 - checkmarkSize / 2,
                    checkmarkSize,
                )
            }
        }
    }

    private fun renderText(ctx: GuiGraphicsExtractor) {
        val roomSize = MapUtils.mapRoomSize.toFloat()
        val gapSize = HotbarMapScanner.quarterRoom.toFloat()
        val halfRoom = HotbarMapScanner.halfRoom.toFloat()
        val fullCellSize = roomSize + gapSize

        DungeonScanner.uniqueRooms.values.forEach { unq ->
            val roomTile = unq.mainRoom

            if (unq.data.isUnknown()) return@forEach
            if (unq.data.type == RoomType.ENTRANCE) return@forEach
            if (! MapConfig.dungeonMapCheater.value && roomTile.state.equalsOneOf(RoomState.UNDISCOVERED, RoomState.UNOPENED)) return@forEach

            val checkPos = unq.getCheckmarkPosition()
            val cX = (checkPos.first / 2f) * fullCellSize + halfRoom
            val cY = (checkPos.second / 2f) * fullCellSize + halfRoom

            val color = when (roomTile.state) {
                RoomState.GREEN -> Color(85, 255, 85)
                RoomState.FAILED -> Color(255, 0, 0)
                RoomState.CLEARED -> Color(255, 255, 255)
                else -> Color(170, 170, 170)
            }

            when (MapConfig.dungeonMapCheckmarkStyle.value) {
                2, 3 -> {
                    var scale = MapConfig.textScale.value.toFloat()
                    val showSecrets = MapConfig.dungeonMapCheckmarkStyle.value == 3 && roomTile.data.secrets > 0

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
                        ctx.drawCenteredString(line, cX, currentY, color, scale)
                        currentY += totalH / totalLines
                    }

                    if (showSecrets) {
                        val secStr = "${unq.foundSecrets}/${roomTile.data.secrets}"
                        ctx.drawCenteredString(secStr, cX, currentY, color, scale)
                    }
                }

                1 -> ctx.drawCenteredString(
                    if (roomTile.data.secrets == 0) "0" else "${unq.foundSecrets}/${roomTile.data.secrets}",
                    cX,
                    cY - UGraphics.getFontHeight() / 2,
                    color,
                    MapConfig.textScale.value
                )

                0 -> {
                    val checkmarkSize = MapConfig.checkmarkSize.value * 10
                    val halfcheckmarkSize = checkmarkSize / 2
                    drawCheckmark(ctx, unq.mainRoom, cX - halfcheckmarkSize, cY - halfcheckmarkSize, MapConfig.checkmarkSize.value * 10)
                }
            }
        }
    }

    private fun renderPlayerHeads(ctx: GuiGraphicsExtractor) {
        if (LocationUtils.inBoss) return

        DungeonListener.dungeonTeammatesNoSelf.forEach { player ->
            if (player.isDead) return@forEach
            drawPlayerHead(ctx, player)
        }

        drawPlayerHead(ctx, DungeonListener.thePlayer ?: return)
    }


    private fun drawCheckmark(ctx: GuiGraphicsExtractor, tile: Tile, x: Number, y: Number, size: Number) {
        val checkmark = when (tile.state) {
            RoomState.CLEARED -> checkmarkWhite
            RoomState.GREEN -> checkmarkGreen
            RoomState.FAILED -> checkmarkFail
            RoomState.UNOPENED -> if (! MapConfig.hideQuestionCheckmarks.value) checkmarkUnknown else return
            else -> return
        }

        ctx.drawTexture(checkmark, x, y, size, size)
    }

    private fun drawPlayerHead(ctx: GuiGraphicsExtractor, teammate: DungeonPlayer) {
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

        ctx.pose().pushMatrix()
        ctx.pose().translate(x, z)
        val currentYaw = MathUtils.normalizeYaw(yaw)
        val headYaw = Math.toRadians((currentYaw + 180).toDouble()).toFloat()

        ctx.pose().rotate(headYaw)
        ctx.pose().scale(MapConfig.playerHeadScale.value)

        if (MapConfig.mapVanillaMarker.value && teammate == DungeonListener.thePlayer) {
            ctx.drawTexture(ownPlayerMarker, - 6, - 6, 12, 12, MapConfig.mapVanillaMarkerColor.value)
        }
        else {
            ctx.drawBorder(- 7, - 7, 14, 14, borderColor)
            ctx.drawPlayerHead(- 6, - 6, 12, teammate.skin)
        }

        val heldItem = mc.player?.mainHandItem
        val shouldDrawName = MapConfig.playerNames.value == 2 || (MapConfig.playerNames.value == 1
            && (heldItem != null && (heldItem.skyblockId == "SPIRIT_LEAP" || heldItem.skyblockId == "INFINITE_SPIRIT_LEAP"
            || heldItem.skyblockId == "HAUNT_ABILITY")))

        if (shouldDrawName) {
            ctx.pose().rotate(- headYaw)
            ctx.pose().translate(0f, 8f)
            ctx.pose().scale(MapConfig.playerNameScale.value)
            ctx.drawCenteredString(teammate.name, 0, 0, nameColor)
        }

        ctx.pose().popMatrix()
    }

    private fun drawRoomConnector(
        matrices: GuiGraphicsExtractor, x: Int, y: Int, doorWidth: Int, doorway: Boolean, vertical: Boolean, color: Color,
    ) {
        val doorwayOffset = if (MapUtils.mapRoomSize == 16) 5 else 6
        val doorHeight = if (doorway) 6 else MapUtils.mapRoomSize
        var x1 = if (vertical) x + MapUtils.mapRoomSize else x
        var y1 = if (vertical) y else y + MapUtils.mapRoomSize
        if (doorway) if (vertical) y1 += doorwayOffset else x1 += doorwayOffset

        matrices.drawRect(
            x1.toDouble(),
            y1.toDouble(),
            (if (vertical) doorWidth else doorHeight).toDouble(),
            (if (vertical) doorHeight else doorWidth).toDouble(),
            color
        )
    }
}
