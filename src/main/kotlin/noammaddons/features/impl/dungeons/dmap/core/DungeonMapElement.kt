package noammaddons.features.impl.dungeons.dmap.core


import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
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
import noammaddons.features.impl.dungeons.dmap.utils.MapUtils
import noammaddons.utils.*
import noammaddons.utils.DungeonUtils.dungeonStarted
import noammaddons.utils.RenderHelper.colorCodeByPresent
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.max


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

        mc.mcProfiler.startSection("dungeonmap")

        mc.mcProfiler.startSection("drawMapBackground")
        drawMapBackground()
        mc.mcProfiler.endSection()
        GlStateManager.translate(MapUtils.startCorner.first.toFloat(), MapUtils.startCorner.second.toFloat(), 0f)
        mc.mcProfiler.startSection("renderRooms")
        renderRooms()
        mc.mcProfiler.endSection()
        mc.mcProfiler.startSection("renderCheckmarks")
        renderCheckmarks()
        mc.mcProfiler.endSection()
        mc.mcProfiler.startSection("renderText")
        renderText()
        mc.mcProfiler.endSection()
        GlStateManager.translate(- MapUtils.startCorner.first.toFloat(), - MapUtils.startCorner.second.toFloat(), 0f)
        mc.mcProfiler.startSection("renderPlayerHeads")
        renderPlayerHeads()
        mc.mcProfiler.endSection()
        mc.mcProfiler.startSection("drawExtraInfo")
        drawExtraInfo()
        mc.mcProfiler.endSection()

        mc.mcProfiler.endSection()
        GlStateManager.popMatrix()
    }


    private fun drawMapBackground() {
        when (DungeonMapConfig.mapBackgroundStyle.value) {
            0 -> {
                val tessellator = Tessellator.getInstance()
                val worldRenderer = tessellator.worldRenderer

                GlStateManager.pushMatrix()
                GlStateManager.enableBlend()
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
                GlStateManager.disableTexture2D()

                worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR)

                MapRenderUtils.addRectToBatch(
                    0.0, 0.0, width.toDouble(), height.toDouble(),
                    DungeonMapConfig.mapBackground.value
                )

                MapRenderUtils.addRectBorderToBatch(
                    0.0, 0.0, width.toDouble(), height.toDouble(),
                    DungeonMapConfig.mapBorderWidth.value,
                    DungeonMapConfig.mapBorderColor.value
                )

                tessellator.draw()

                GlStateManager.enableTexture2D()
                GlStateManager.disableBlend()
                GlStateManager.popMatrix()
            }

            1 -> RenderUtils.drawTexture(RES_MAP_BACKGROUND, - 2.5, - 2.5, width + 5, height + 5)
        }
    }

    private fun renderRooms() {
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.disableTexture2D()

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR)

        for (y in 0 .. 10) {
            for (x in 0 .. 10) {
                val tile = DungeonInfo.dungeonList[y * 11 + x]
                if (! DungeonMapConfig.dungeonMapCheater.value && (tile is Unknown || tile.state == RoomState.UNDISCOVERED)) continue
                val color = (if (tile.state == RoomState.UNDISCOVERED) tile.color.darker().darker() else tile.color).let { base ->
                    base.takeIf { DungeonMapConfig.highlightMimicRoom.value && (tile as? Room)?.uniqueRoom?.hasMimic == true }?.let {
                        MathUtils.lerpColor(it, DungeonMapConfig.colorMimic.value, 0.2)
                    } ?: base
                }

                val xOffset = (x shr 1) * (MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom)
                val yOffset = (y shr 1) * (MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom)

                val xEven = x and 1 == 0
                val yEven = y and 1 == 0

                when {
                    xEven && yEven -> if (tile is Room) {
                        MapRenderUtils.addRectToBatch(
                            xOffset.toDouble(),
                            yOffset.toDouble(),
                            MapUtils.mapRoomSize.toDouble(),
                            MapUtils.mapRoomSize.toDouble(),
                            color
                        )
                    }

                    ! xEven && ! yEven -> {
                        MapRenderUtils.addRectToBatch(
                            xOffset.toDouble(),
                            yOffset.toDouble(),
                            (MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom).toDouble(),
                            (MapUtils.mapRoomSize + HotbarMapColorParser.quarterRoom).toDouble(),
                            color
                        )
                    }

                    else -> {
                        val doorway = tile is Door
                        val vertical = ! xEven
                        val doorWidth = HotbarMapColorParser.quarterRoom

                        val doorwayOffset = if (MapUtils.mapRoomSize == 16) 5 else 6
                        val width = if (doorway) 6 else MapUtils.mapRoomSize
                        var x1 = if (vertical) xOffset + MapUtils.mapRoomSize else xOffset
                        var y1 = if (vertical) yOffset else yOffset + MapUtils.mapRoomSize
                        if (doorway) if (vertical) y1 += doorwayOffset else x1 += doorwayOffset

                        val finalW = if (vertical) doorWidth else width
                        val finalH = if (vertical) width else doorWidth

                        MapRenderUtils.addRectToBatch(x1.toDouble(), y1.toDouble(), finalW.toDouble(), finalH.toDouble(), color)
                    }
                }
            }
        }

        tessellator.draw()

        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
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
        val mc = Minecraft.getMinecraft()
        val fr = mc.fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.enableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.color(1f, 1f, 1f, 1f)

        val roomSize = MapUtils.mapRoomSize.toFloat()
        val gapSize = HotbarMapColorParser.quarterRoom.toFloat()
        val halfRoom = HotbarMapColorParser.halfRoom.toFloat()
        val fullCellSize = roomSize + gapSize

        DungeonInfo.uniqueRooms.forEach { unq ->
            val room = unq.mainRoom

            if (unq.name == "Unknown") return@forEach
            if (room.data.type == RoomType.ENTRANCE) return@forEach
            if (! DungeonMapConfig.dungeonMapCheater.value && (room.state == RoomState.UNDISCOVERED || room.state == RoomState.UNOPENED)) return@forEach

            val checkPos = unq.getCheckmarkPosition()
            val cX = (checkPos.first / 2f) * fullCellSize + halfRoom
            val cY = (checkPos.second / 2f) * fullCellSize + halfRoom

            val color = when (room.state) {
                RoomState.GREEN -> 0x55ff55
                RoomState.CLEARED -> 0xffffff
                RoomState.FAILED -> 0xff0000
                else -> 0xaaaaaa
            }


            GlStateManager.pushMatrix()
            GlStateManager.translate(cX, cY, 0f)

            when (DungeonMapConfig.dungeonMapCheckmarkStyle.value) {
                3, 4 -> {
                    var scale = DungeonMapConfig.textScale.value

                    if (DungeonMapConfig.limitRoomNameSize.value) {
                        var maxWidth = roomSize
                        var maxHeight = roomSize

                        if (room.data.shape != "L" || room.data.shape != "1x1" || room.data.shape != "2x2") {
                            var minX = Int.MAX_VALUE;
                            var maxX = Int.MIN_VALUE
                            var minZ = Int.MAX_VALUE;
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

                        var maxLineW = 0
                        for (line in unq.cacheSplitName) {
                            val w = fr.getStringWidth(line)
                            if (w > maxLineW) maxLineW = w
                        }

                        var totalH = unq.cacheSplitName.size * fr.FONT_HEIGHT

                        if (DungeonMapConfig.dungeonMapCheckmarkStyle.value == 4 && room.data.secrets > 0) {
                            val w = fr.getStringWidth("${unq.foundSecrets}/${room.data.secrets}")
                            if (w > maxLineW) maxLineW = w
                            totalH += fr.FONT_HEIGHT
                        }

                        if (maxLineW > 0 && totalH > 0) {
                            val sW = maxWidth / maxLineW
                            val sH = maxHeight / totalH
                            scale = sW.coerceAtMost(sH).coerceIn(0.4f, DungeonMapConfig.textScale.value)
                        }
                    }

                    GlStateManager.scale(scale, scale, 1f)

                    val showSecrets = DungeonMapConfig.dungeonMapCheckmarkStyle.value == 4 && room.data.secrets > 0
                    val totalLines = unq.cacheSplitName.size + (if (showSecrets) 1 else 0)
                    val totalH = totalLines * fr.FONT_HEIGHT

                    var currentY = - (totalH / 2f)

                    for (line in unq.cacheSplitName) {
                        fr.drawString(line, - fr.getStringWidth(line) / 2f, currentY, color, true)
                        currentY += fr.FONT_HEIGHT
                    }

                    if (showSecrets) {
                        val secStr = "${unq.foundSecrets}/${room.data.secrets}"
                        fr.drawString(secStr, - fr.getStringWidth(secStr) / 2f, currentY, color, true)
                    }
                }

                2 -> {
                    val str = if (room.data.secrets == 0) "0" else "${unq.foundSecrets}/${room.data.secrets}"
                    GlStateManager.scale(DungeonMapConfig.textScale.value, DungeonMapConfig.textScale.value, 1f)
                    fr.drawString(str, - fr.getStringWidth(str) / 2f, - (fr.FONT_HEIGHT / 2f) + 1, color, true)
                }
            }

            GlStateManager.popMatrix()
        }

        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    private fun renderPlayerHeads() {
        if (LocationUtils.inBoss) return

        if (dungeonStarted) {
            DungeonUtils.dungeonTeammatesNoSelf.forEach { p ->
                if (p.isDead) return@forEach
                MapRenderUtils.drawPlayerHead(p.name, p.skin, p.clazz, p.entity, p.mapIcon.mapX, p.mapIcon.mapZ, p.mapIcon.yaw)
            }

            DungeonUtils.thePlayer?.let { p ->
                MapRenderUtils.drawPlayerHead(p.name, p.skin, p.clazz, p.entity)
            }
        }
        else {
            DungeonUtils.runPlayersNames.forEach { (name, skin) ->
                if (name == mc.session.username) return@forEach
                mc.theWorld.getPlayerEntityByName(name)?.let { entity ->
                    MapRenderUtils.drawPlayerHead(name, skin, DungeonUtils.Classes.Empty, entity)
                }
            }

            MapRenderUtils.drawPlayerHead(mc.session.username, mc.thePlayer.locationSkin, DungeonUtils.Classes.Empty, mc.thePlayer)
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
}