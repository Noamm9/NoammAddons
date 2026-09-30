package com.github.noamm9.ui.utils

import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.blaze3d.platform.cursor.CursorTypes
//~ if <26.3 'org.lwjgl.sdl.SDLMouse' -> 'org.lwjgl.glfw.GLFW'
import org.lwjgl.glfw.GLFW

enum class ResizeCorner(val cursor: CursorType) {
    NONE(CursorTypes.ARROW),
    LEFT(CursorTypes.RESIZE_EW),
    RIGHT(CursorTypes.RESIZE_EW),
    TOP(CursorTypes.RESIZE_NS),
    BOTTOM(CursorTypes.RESIZE_NS),

    //~ if <26.3 'SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE' -> 'GLFW.GLFW_RESIZE_NWSE_CURSOR' {
    TOP_LEFT(CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR, "noammaddons:nwse_resize", CursorTypes.RESIZE_ALL)),
    BOTTOM_RIGHT(CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR, "noammaddons:nwse_resize", CursorTypes.RESIZE_ALL)),
    //~}

    //~ if <26.3 'SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE' -> 'GLFW.GLFW_RESIZE_NESW_CURSOR' {
    TOP_RIGHT(CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR, "noammaddons:nesw_resize", CursorTypes.RESIZE_ALL)),
    BOTTOM_LEFT(CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR, "noammaddons:nesw_resize", CursorTypes.RESIZE_ALL));
    //~}
}