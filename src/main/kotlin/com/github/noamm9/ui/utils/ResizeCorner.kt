package com.github.noamm9.ui.utils

import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.blaze3d.platform.cursor.CursorTypes
import org.lwjgl.sdl.SDLMouse

enum class ResizeCorner(val cursor: CursorType) {
    NONE(CursorTypes.ARROW),
    LEFT(CursorTypes.RESIZE_EW),
    RIGHT(CursorTypes.RESIZE_EW),
    TOP(CursorTypes.RESIZE_NS),
    BOTTOM(CursorTypes.RESIZE_NS),
    TOP_LEFT(CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE, "noammaddons:nwse_resize", CursorTypes.RESIZE_ALL)),
    TOP_RIGHT(CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE, "noammaddons:nesw_resize", CursorTypes.RESIZE_ALL)),
    BOTTOM_LEFT(CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE, "noammaddons:nesw_resize", CursorTypes.RESIZE_ALL)),
    BOTTOM_RIGHT(CursorType.createStandardCursor(SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE, "noammaddons:nwse_resize", CursorTypes.RESIZE_ALL));
}