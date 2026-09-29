package com.github.noamm9.ui.utils

//? if <26.2 {
/*import org.lwjgl.glfw.GLFW*/
//? } else if <26.3 {
/*import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.blaze3d.platform.cursor.CursorTypes
import org.lwjgl.glfw.GLFW*/
//? } else {
import com.mojang.blaze3d.platform.cursor.CursorType
import com.mojang.blaze3d.platform.cursor.CursorTypes
import org.lwjgl.sdl.SDLMouse
//? }

//? if <26.2 {
/*enum class ResizeCorner(val cursor: Int) {
    NONE(GLFW.GLFW_ARROW_CURSOR),
    LEFT(GLFW.GLFW_HRESIZE_CURSOR),
    RIGHT(GLFW.GLFW_HRESIZE_CURSOR),
    TOP(GLFW.GLFW_VRESIZE_CURSOR),
    BOTTOM(GLFW.GLFW_VRESIZE_CURSOR),
    TOP_LEFT(GLFW.GLFW_RESIZE_NWSE_CURSOR),
    TOP_RIGHT(GLFW.GLFW_RESIZE_NESW_CURSOR),
    BOTTOM_LEFT(GLFW.GLFW_RESIZE_NESW_CURSOR),
    BOTTOM_RIGHT(GLFW.GLFW_RESIZE_NWSE_CURSOR);*/
//? } else if <26.3 {
/*enum class ResizeCorner(val cursor: CursorType) {
    NONE(CursorTypes.ARROW),
    LEFT(CursorTypes.RESIZE_EW),
    RIGHT(CursorTypes.RESIZE_EW),
    TOP(CursorTypes.RESIZE_NS),
    BOTTOM(CursorTypes.RESIZE_NS),
    TOP_LEFT(CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR, "noammaddons:nwse_resize", CursorTypes.RESIZE_ALL)),
    TOP_RIGHT(CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR, "noammaddons:nesw_resize", CursorTypes.RESIZE_ALL)),
    BOTTOM_LEFT(CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR, "noammaddons:nesw_resize", CursorTypes.RESIZE_ALL)),
    BOTTOM_RIGHT(CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR, "noammaddons:nwse_resize", CursorTypes.RESIZE_ALL));*/
//? } else {
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
//? }
}