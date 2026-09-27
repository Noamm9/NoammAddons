package com.github.noamm9.features.impl.dev.cosmetics.badges

import com.mojang.blaze3d.platform.NativeImage

object BadgePack {
    private const val CELL = 64
    private const val COLUMNS = 16
    const val ASCENT = 8
    const val HEIGHT = 10

    fun fitCell(source: NativeImage): NativeImage {
        if (source.width == CELL && source.height == CELL) return source
        val target = NativeImage(CELL, CELL, false)
        source.resizeSubRectTo(0, 0, source.width, source.height, target)
        source.close()
        return target
    }

    fun buildAtlas(cells: List<NativeImage>): NativeImage {
        val rows = (cells.size + COLUMNS - 1) / COLUMNS
        val atlas = NativeImage(COLUMNS * CELL, rows * CELL, false)
        atlas.fillRect(0, 0, atlas.width, atlas.height, 0)
        cells.forEachIndexed { index, cell ->
            cell.copyRect(atlas, 0, 0, (index % COLUMNS) * CELL, (index / COLUMNS) * CELL, CELL, CELL, false, false)
            cell.close()
        }
        return atlas
    }

    fun codepointGrid(chars: List<Char>) = chars.chunked(COLUMNS).map { row ->
        IntArray(COLUMNS) { if (it < row.size) row[it].code else 0 }
    }.toTypedArray()
}