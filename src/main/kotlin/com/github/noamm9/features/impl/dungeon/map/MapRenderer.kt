package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.NoammAddons.MOD_ID
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.ui.hud.HudElement
import com.github.noamm9.utils.ColorUtils.colorCodeByPercent
import com.github.noamm9.utils.ColorUtils.colorizeScore
import com.github.noamm9.utils.MathUtils
import com.github.noamm9.utils.dungeons.DungeonListener
import com.github.noamm9.utils.dungeons.DungeonPlayer
import com.github.noamm9.utils.dungeons.enums.DungeonClass
import com.github.noamm9.utils.dungeons.map.DungeonInfo
import com.github.noamm9.utils.dungeons.map.core.*
import com.github.noamm9.utils.dungeons.map.handlers.HotbarMapColorParser
import com.github.noamm9.utils.dungeons.map.handlers.ScoreCalculation
import com.github.noamm9.utils.dungeons.map.utils.MapUtils
import com.github.noamm9.utils.items.ItemUtils.skyblockId
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import com.github.noamm9.utils.render.RenderHelper.renderVec
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import java.awt.Color
import kotlin.math.max

object MapRenderer: HudElement() {
    override val name get() = "Dungeon Map"
    override val toggle get() = DungeonMap.enabled && MapConfig.mapEnabled.value
    override val shouldDraw get() = LocationUtils.inDungeon && ! LocationUtils.inBoss

    private val checkmarkGreen = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dungeonmap/checkmarks/green_check")
    private val checkmarkWhite = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dungeonmap/checkmarks/white_check")
    private val checkmarkUnknown = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dungeonmap/checkmarks/question")
    private val checkmarkFail = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dungeonmap/checkmarks/cross")
    private val ownPlayerMarker = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dungeonmap/marker_self")
    // private val otherPlayerMarker = ResourceLocation.fromNamespaceAndPath(MOD_ID, "dungeonmap/marker_other")

    override fun draw(ctx: GuiGraphics, example: Boolean): Pair<Float, Float> {

        renderBackground(ctx)
        ctx.pose().translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat())
        applyCheater()
        renderRooms(ctx)
        renderText(ctx)
        ctx.pose().translate(- MapUtils.startCorner.first.toFloat(), - MapUtils.startCorner.second.toFloat())
        renderPlayerHeads(ctx)
        renderExtraInfo(ctx)

        return 128f to if (MapConfig.mapExtraInfo.value) 140f else 128f
    }

    private fun renderBackground(ctx: GuiGraphics) {
        val width = 128
        val height = if (MapConfig.mapExtraInfo.value) 140f else 128f

        Render2D.drawRect(ctx, 0, 0, width, height, MapConfig.mapBackground.value)
        Render2D.drawBorder(ctx, 0, 0, width, height, MapConfig.mapBorderColor.value, MapConfig.mapBorderWidth.value)
    }

    private fun renderExtraInfo(ctx: GuiGraphics) {
        if (! MapConfig.mapExtraInfo.value) return
        if (! MapConfig.dungeonMapCheater.value && ! DungeonListener.dungeonStarted) return

        val secretsStr = "&6Secrets: &b${ScoreCalculation.foundSecrets}&f/&e${DungeonInfo.secretCount}"
        val cryptsStr = colorCodeByPercent(ScoreCalculation.cryptsCount, 6) + "Crypts: ${ScoreCalculation.cryptsCount}"
        val scoreStr = "&eScore: ${colorizeScore(ScoreCalculation.score)}&r"
        val deathsStr = "&cDeaths: ${colorCodeByPercent(ScoreCalculation.deathCount, 4, true)}${ScoreCalculation.deathCount}&r"
        val mimicStr = "&cM: ${if (ScoreCalculation.mimicKilled) "&a&l✔&r" else "&c&l✖&r"}"
        val princeStr = "&eP: ${if (ScoreCalculation.princeKilled) "&a&l✔&r" else "&c&l✖&r"}"

        val line1 = "$secretsStr    $cryptsStr"
        val line2 = "$scoreStr   $deathsStr   $mimicStr $princeStr"

        ctx.pose().translate(width / 2f, 128f)
        Render2D.drawCenteredString(ctx, line1, 0f, - 4f, scale = 0.7f)
        Render2D.drawCenteredString(ctx, line2, 0f, 2f, scale = 0.7f)
    }

    private fun applyCheater() {
        if (! MapConfig.dungeonMapCheater.value) return
        DungeonInfo.dungeonList.forEach { tile ->
            if (tile.state == RoomState.UNOPENED) tile.state = RoomState.UNDISCOVERED
        }
    }

    private fun renderRooms(ctx: GuiGraphics) {
        val connectorSize = (HotbarMapColorParser.quarterRoom.takeUnless { it == - 1 } ?: 4)

        for (y in 0 .. 10) for (x in 0 .. 10) {
            val tile = DungeonInfo.dungeonList[y * 11 + x]
            if (tile is Unknown) continue
            if (tile.state == RoomState.UNDISCOVERED && ! MapConfig.dungeonMapCheater.value) continue
            if (tile is Door && getDoorState(y, x) == RoomState.UNDISCOVERED && ! MapConfig.dungeonMapCheater.value) continue

            var color = tile.color

            if (tile is Room && tile.uniqueRoom?.hasMimic == true && MapConfig.highlightMimicRoom.value) {
                color = MathUtils.lerpColor(color, MapConfig.colorMimic.value, 0.2)
            }

            val xOffset = (x shr 1) * (MapUtils.mapRoomSize + connectorSize)
            val yOffset = (y shr 1) * (MapUtils.mapRoomSize + connectorSize)

            val xEven = x and 1 == 0
            val yEven = y and 1 == 0

            when {
                xEven && yEven -> if (tile is Room) {
                    Render2D.drawRect(
                        ctx,
                        xOffset, yOffset,
                        MapUtils.mapRoomSize,
                        MapUtils.mapRoomSize,
                        color
                    )
                }

                ! xEven && ! yEven -> {
                    Render2D.drawRect(
                        ctx,
                        xOffset, yOffset,
                        MapUtils.mapRoomSize + connectorSize,
                        MapUtils.mapRoomSize + connectorSize,
                        color
                    )
                }

                else -> drawRoomConnector(
                    ctx, xOffset, yOffset, connectorSize, tile is Door, ! xEven, color
                )
            }

            if (tile is Room && tile.core == 0) {
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

    private fun getDoorState(row: Int, column: Int): RoomState {
        val rooms = getConnectingDoorRooms(row, column) ?: return RoomState.UNDISCOVERED
        if (rooms.toList().any { it.state == RoomState.UNDISCOVERED }) return RoomState.UNDISCOVERED
        return RoomState.UNOPENED
    }

    private fun getConnectingDoorRooms(row: Int, column: Int): Pair<Room, Room>? {
        val vertical = column % 2 == 0
        val connectingTiles = runCatching {
            if (vertical) DungeonInfo.dungeonList[(row - 1) * 11 + column] to DungeonInfo.dungeonList[(row + 1) * 11 + column]
            else DungeonInfo.dungeonList[row * 11 + column - 1] to DungeonInfo.dungeonList[row * 11 + column + 1]
        }.getOrNull() ?: return null
        return (connectingTiles.first as? Room ?: return null) to (connectingTiles.second as? Room ?: return null)
    }

    private fun renderText(ctx: GuiGraphics) {
        val roomSize = MapUtils.mapRoomSize.toFloat()
        val gapSize = HotbarMapColorParser.quarterRoom.toFloat()
        val halfRoom = HotbarMapColorParser.halfRoom.toFloat()
        val fullCellSize = roomSize + gapSize

        DungeonInfo.uniqueRooms.forEach { (name, unq) ->
            val room = unq.mainRoom

            if (name == "Unknown") return@forEach
            if (room.data.type == RoomType.ENTRANCE) return@forEach
            if (! MapConfig.dungeonMapCheater.value && (room.state == RoomState.UNDISCOVERED || room.state == RoomState.UNOPENED)) return@forEach

            val checkPos = unq.getCheckmarkPosition()
            val cX = (checkPos.first / 2f) * fullCellSize + halfRoom
            val cY = (checkPos.second / 2f) * fullCellSize + halfRoom

            val color = when (room.state) {
                RoomState.GREEN -> Color(0x55ff55)
                RoomState.CLEARED -> Color(0xffffff)
                RoomState.FAILED -> Color(0xff0000)
                else -> Color(0xaaaaaa)
            }

            when (MapConfig.dungeonMapCheckmarkStyle.value) {
                2, 3 -> {
                    var scale = MapConfig.textScale.value.toFloat()
                    val showSecrets = MapConfig.dungeonMapCheckmarkStyle.value == 3 && room.data.secrets > 0

                    if (MapConfig.limitRoomNameSize.value) {
                        var maxWidth = roomSize
                        var maxHeight = roomSize

                        if (room.data.shape != "L" || room.data.shape != "1x1" || room.data.shape != "2x2") {
                            var minX = Int.MAX_VALUE
                            var maxX = Int.MIN_VALUE
                            var minZ = Int.MAX_VALUE
                            var maxZ = Int.MIN_VALUE

                            for (tile in unq.tiles) {
                                if (tile.x < minX) minX = tile.x
                                if (tile.x > maxX) maxX = tile.x
                                if (tile.z < minZ) minZ = tile.z
                                if (tile.z > maxZ) maxZ = tile.z
                            }

                            val tilesWide = maxX - minX + 1
                            val tilesTall = maxZ - minZ + 1

                            maxWidth = (tilesWide * roomSize) + (max(0, tilesWide - 1) * gapSize)
                            maxHeight = (tilesTall * roomSize) + (max(0, tilesTall - 1) * gapSize)
                        }

                        if (room.data.shape == "L") maxWidth = roomSize * 2
                        else if (room.data.shape == "2x2") {
                            maxWidth = roomSize * 2
                            maxHeight = roomSize * 2
                        }

                        var maxLineW = 0f
                        for (line in unq.cacheSplitName) {
                            val w = line.width() * scale
                            if (w > maxLineW) maxLineW = w
                        }

                        var totalH = unq.cacheSplitName.size * mc.font.lineHeight * scale

                        if (showSecrets) {
                            val w = "${unq.foundSecrets}/${room.data.secrets}".width() * scale
                            if (w > maxLineW) maxLineW = w
                            totalH += mc.font.lineHeight
                        }

                        if (maxLineW > 0 && totalH > 0) {
                            val sW = maxWidth / maxLineW
                            val sH = maxHeight / totalH
                            scale = sW.coerceAtMost(sH).coerceIn(0.39f, MapConfig.textScale.value.toFloat())
                        }
                    }

                    val totalLines = unq.cacheSplitName.size + (if (showSecrets) 1 else 0)
                    val totalH = totalLines * mc.font.lineHeight * scale

                    var currentY = cY - totalH / 2

                    for (line in unq.cacheSplitName) {
                        Render2D.drawCenteredString(ctx, line, cX, currentY, color, scale)
                        currentY += totalH / totalLines
                    }

                    if (showSecrets) {
                        val secStr = "${unq.foundSecrets}/${room.data.secrets}"
                        Render2D.drawCenteredString(ctx, secStr, cX, currentY, color, scale)
                    }
                }

                1 -> Render2D.drawCenteredString(
                    ctx,
                    if (room.data.secrets == 0) "0" else "${unq.foundSecrets}/${room.data.secrets}",
                    cX,
                    cY - mc.font.lineHeight / 2,
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

    private fun renderPlayerHeads(ctx: GuiGraphics) {
        if (LocationUtils.inBoss) return

        DungeonListener.dungeonTeammatesNoSelf.forEach { player ->
            if (player.isDead) return@forEach
            drawPlayerHead(ctx, player)
        }

        drawPlayerHead(ctx, DungeonListener.thePlayer ?: return)
    }


    private fun drawCheckmark(ctx: GuiGraphics, tile: Tile, x: Number, y: Number, size: Number) {
        val checkmark = when (tile.state) {
            RoomState.CLEARED -> checkmarkWhite
            RoomState.GREEN -> checkmarkGreen
            RoomState.FAILED -> checkmarkFail
            RoomState.UNOPENED -> if (! MapConfig.hideQuestionCheckmarks.value) checkmarkUnknown else return
            else -> return
        }

        Render2D.drawTexture(ctx, checkmark, x, y, size, size)
    }

    private fun drawPlayerHead(ctx: GuiGraphics, teammate: DungeonPlayer) {
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
            Render2D.drawTexture(ctx, ownPlayerMarker, - 6, - 6, 12, 12)
        }
        else {
            Render2D.drawBorder(ctx, - 7, - 7, 14, 14, borderColor)
            Render2D.drawPlayerHead(ctx, - 6, - 6, 12, teammate.skin)
        }

        val heldItem = mc.player?.mainHandItem
        val shouldDrawName = MapConfig.playerNames.value == 2 || (MapConfig.playerNames.value == 1
            && (heldItem != null && (heldItem.skyblockId == "SPIRIT_LEAP" || heldItem.skyblockId == "INFINITE_SPIRIT_LEAP"
            || heldItem.skyblockId == "HAUNT_ABILITY")))

        if (shouldDrawName) {
            ctx.pose().rotate(- headYaw)
            ctx.pose().translate(0f, 8f)
            ctx.pose().scale(MapConfig.playerNameScale.value)
            Render2D.drawCenteredString(ctx, teammate.name, 0, 0, nameColor)
        }

        ctx.pose().popMatrix()
    }

    private fun drawRoomConnector(
        matrices: GuiGraphics, x: Int, y: Int, doorWidth: Int, doorway: Boolean, vertical: Boolean, color: Color,
    ) {
        val doorwayOffset = if (MapUtils.mapRoomSize == 16) 5 else 6
        val width = if (doorway) 6 else MapUtils.mapRoomSize
        var x1 = if (vertical) x + MapUtils.mapRoomSize else x
        var y1 = if (vertical) y else y + MapUtils.mapRoomSize
        if (doorway) if (vertical) y1 += doorwayOffset else x1 += doorwayOffset

        Render2D.drawRect(
            matrices,
            x1.toDouble(),
            y1.toDouble(),
            (if (vertical) doorWidth else width).toDouble(),
            (if (vertical) width else doorWidth).toDouble(),
            color
        )
    }
}