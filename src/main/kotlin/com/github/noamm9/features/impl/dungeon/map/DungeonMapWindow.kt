package com.github.noamm9.features.impl.dungeon.map

import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.components.FeatureConfigWindow
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D.drawRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.min

class DungeonMapWindow(feature: Feature): FeatureConfigWindow(feature) {
    override val previewWidth = 240f
    override val defaultHeight = 300f
    override val minHeight = 200f
    override val useSidebar = true

    override fun drawPreview(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float) {
        context.pose().pushMatrix()
        context.pose().translate(mapX, mapY)
        context.pose().scale(mapScale)
        MapRenderer.draw(context, example = ! LocationUtils.inDungeon, extraInfo = true)
        context.pose().popMatrix()
    }

    override fun drawBackground(context: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float, color: Color) {
        val mapRight = mapX + 128f * mapScale
        val mapBottom = mapY + mapHeight * mapScale

        context.drawRect(x, y, width, mapY - y, color)
        context.drawRect(x, mapBottom, width, y + height - mapBottom, color)
        context.drawRect(x, mapY, mapX - x, mapBottom - mapY, color)
        context.drawRect(mapRight, mapY, x + width - mapRight, mapBottom - mapY, color)
    }

    private val mapHeight get() = if (MapConfig.mapExtraInfo.value) 140f else 128f
    private val mapScale get() = min((previewWidth - 12f) / 128f, (previewHeight - 12f) / mapHeight)
    private val mapX get() = previewX + (previewWidth - 128f * mapScale) / 2f
    private val mapY get() = previewY + (previewHeight - mapHeight * mapScale) / 2f
}