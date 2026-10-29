package com.github.noamm9.ui.utils

import com.github.noamm9.compat.CURSOR_RESIZE_NESW
import com.github.noamm9.compat.CURSOR_RESIZE_NWSE
import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.blaze3d.platform.cursor.CursorTypes

enum class ResizeCorner(val cursor: CursorType) {
    NONE(CursorTypes.ARROW),
    LEFT(CursorTypes.RESIZE_EW),
    RIGHT(CursorTypes.RESIZE_EW),
    TOP(CursorTypes.RESIZE_NS),
    BOTTOM(CursorTypes.RESIZE_NS),
    TOP_LEFT(CursorType.createStandardCursor(CURSOR_RESIZE_NWSE, "noammaddons:nwse_resize", CursorTypes.RESIZE_ALL)),
    TOP_RIGHT(CursorType.createStandardCursor(CURSOR_RESIZE_NESW, "noammaddons:nesw_resize", CursorTypes.RESIZE_ALL)),
    BOTTOM_LEFT(CursorType.createStandardCursor(CURSOR_RESIZE_NESW, "noammaddons:nesw_resize", CursorTypes.RESIZE_ALL)),
    BOTTOM_RIGHT(CursorType.createStandardCursor(CURSOR_RESIZE_NWSE, "noammaddons:nwse_resize", CursorTypes.RESIZE_ALL));
}