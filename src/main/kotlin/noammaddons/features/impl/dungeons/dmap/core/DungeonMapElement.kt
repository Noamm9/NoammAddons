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
import noammaddons.features.impl.dungeons.dmap.utils.MapRenderUtils.colorizeScore
import noammaddons.features.impl.dungeons.dmap.utils.MapRenderUtils.drawPlayerHead
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils
import noammaddons.utils.*
import noammaddons.utils.DungeonUtils.dungeonStarted
import noammaddons.utils.RenderHelper.colorCodeByPresent
import noammaddons.utils.RenderHelper.getStringHeight
import noammaddons.utils.RenderHelper.getStringWidth
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
        GlStateManager.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)
        renderRooms()
        renderCheckmarks()
        renderText()
        GlStateManager.translate(- MapUtils.startCorner.first.toFloat(), - MapUtils.startCorner.second.toFloat(), 0f)
        renderPlayerHeads()
        drawExtraInfo()

        GlStateManager.popMatrix()
    }


    private fun drawMapBackground() {
        when (DungeonMapConfig.mapBackgroundStyle.value) {
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
        }
    }

    private fun renderRooms() {
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
                val tile = (DungeonInfo.dungeonList[y * 11 + x] as? Room)?.takeIf { it.uniqueRoom == null } ?: continue
                if (! DungeonMapConfig.dungeonMapCheater.value && (tile.state == RoomState.UNDISCOVERED)) continue

                val size = MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom
                val xOffset = (x shr 1) * size
                val yOffset = (y shr 1) * size

                drawCheckmark(tile, xOffset.toFloat(), yOffset.toFloat(), checkmarkSize)
            }
        }

        DungeonInfo.uniqueRooms.forEach { unq ->
            val room = unq.mainRoom
            if (! DungeonMapConfig.dungeonMapCheater.value && (room.state == RoomState.UNDISCOVERED || room.state == RoomState.UNOPENED)) return@forEach

            val size = MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom
            val checkPos = unq.getCheckmarkPosition()
            val xOffset = (checkPos.first / 2f) * size
            val yOffset = (checkPos.second / 2f) * size

            drawCheckmark(room, xOffset, yOffset, checkmarkSize)
        }
    }

    private fun renderText() {
        DungeonInfo.uniqueRooms.forEach { unq ->
            val room = unq.mainRoom
            if (! DungeonMapConfig.dungeonMapCheater.value && (room.state == RoomState.UNDISCOVERED || room.state == RoomState.UNOPENED)) return@forEach
            val size = MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom
            val checkPos = unq.getCheckmarkPosition()

            val xOffset = (checkPos.first / 2f) * size
            val yOffset = (checkPos.second / 2f) * size

            val color = when (room.state) {
                RoomState.GREEN -> 0x55ff55
                RoomState.CLEARED -> 0xffffff
                RoomState.FAILED -> 0xff0000
                else -> 0xaaaaaa
            }

            when (DungeonMapConfig.dungeonMapCheckmarkStyle.value) {
                3 -> {
                    val lines = room.data.name.split(" ").takeUnless { "Unknown" in it } ?: return@forEach
                    var textScale = DungeonMapConfig.textScale.value

                    if (DungeonMapConfig.limitRoomNameSize.value) {
                        val scaleToFitWidth = MapUtils.mapRoomSize / (lines.maxOfOrNull { getStringWidth(it) } ?: 0f)
                        val scaleToFitHeight = MapUtils.mapRoomSize / getStringHeight(lines)
                        textScale = minOf(scaleToFitWidth, scaleToFitHeight).coerceAtMost(textScale)
                        if ("Withermancer" in lines) textScale *= 2 // temp fix trust
                    }

                    MapRenderUtils.renderCenteredText(
                        lines,
                        xOffset + HotbarMapColorParser.halfRoom,
                        yOffset + HotbarMapColorParser.halfRoom,
                        if (room.data.type != RoomType.ENTRANCE) color else 0xffffff,
                        textScale
                    )
                }

                2 -> MapRenderUtils.renderCenteredText(
                    listOf("${room.data.secrets}"),
                    xOffset + HotbarMapColorParser.halfRoom,
                    yOffset + 1 + HotbarMapColorParser.halfRoom,
                    color, DungeonMapConfig.textScale.value * 2f
                )
            }
        }
    }

    private fun renderPlayerHeads() {
        if (LocationUtils.inBoss) return

        if (dungeonStarted) {
            DungeonUtils.dungeonTeammatesNoSelf.filterNot { it.isDead }.forEach { p ->
                drawPlayerHead(p.name, p.skin, p.clazz, p.entity, p.mapIcon.mapX, p.mapIcon.mapZ, p.mapIcon.yaw)
            }

            DungeonUtils.thePlayer?.let { p ->
                drawPlayerHead(p.name, p.skin, p.clazz, p.entity)
            }
        }
        else {
            DungeonUtils.runPlayersNames.filterNot { it.key == mc.session.username }.forEach { (name, skin) ->
                mc.theWorld.getPlayerEntityByName(name)?.let { entity ->
                    drawPlayerHead(name, skin, DungeonUtils.Classes.Empty, entity)
                }
            }

            drawPlayerHead(mc.session.username, mc.thePlayer.locationSkin, DungeonUtils.Classes.Empty, mc.thePlayer)
        }
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
        MapRenderUtils.renderCenteredText(textLines, 0f, 3f, 0xffffff, 0.7f)
    }

    private fun drawCheckmark(room: Room, xOffset: Float, yOffset: Float, checkmarkSize: Double) {
        if (DungeonMapConfig.dungeonMapCheckmarkStyle.value != 1 && room.core != 0) return

        val checkmark = when (room.state) {
            RoomState.CLEARED -> CheckMarkWhite
            RoomState.GREEN -> CheckMarkGreen
            RoomState.FAILED -> CheckMarkCross
            RoomState.UNOPENED -> if (DungeonMapConfig.dungeonMapCheater.value) null
            else if (DungeonMapConfig.hideQuestionCheckmarks.value) null
            else CheckMarkQuestion

            else -> return
        }

        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.enableAlpha()
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f)
        RenderHelper.bindColor(Color.WHITE)
        mc.textureManager.bindTexture(checkmark)

        MapRenderUtils.drawTexturedQuad(
            xOffset + (MapUtils.mapRoomSize - checkmarkSize) / 2,
            yOffset + (MapUtils.mapRoomSize - checkmarkSize) / 2,
            checkmarkSize, checkmarkSize
        )

        GlStateManager.disableBlend()
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
}