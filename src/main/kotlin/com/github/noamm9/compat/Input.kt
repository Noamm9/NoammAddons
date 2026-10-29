package com.github.noamm9.compat

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.input.KeyEvent

//~ if <26.3 'InputConstants.Type.KEYBOARD' -> 'InputConstants.Type.KEYSYM'
val KEYBOARD_TYPE: InputConstants.Type = InputConstants.Type.KEYBOARD

//~ if <26.3 'keycode' -> 'scancode'
val KeyEvent.scanCodeCompat get() = keycode

//~ if <26.3 'sdl.SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE' -> 'glfw.GLFW.GLFW_RESIZE_NWSE_CURSOR'
const val CURSOR_RESIZE_NWSE = org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_NWSE_RESIZE

//~ if <26.3 'sdl.SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE' -> 'glfw.GLFW.GLFW_RESIZE_NESW_CURSOR'
const val CURSOR_RESIZE_NESW = org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_NESW_RESIZE

//~ if <26.3 '(key)' -> '(com.github.noamm9.NoammAddons.mc.window, key)'
fun isKeyDown(key: Int) = InputConstants.isKeyDown(key)

fun isMouseDown(button: Int) = run {
    //? if <26.3 {
    /*org.lwjgl.glfw.GLFW.glfwGetMouseButton(com.github.noamm9.NoammAddons.mc.window.handle(), button) == org.lwjgl.glfw.GLFW.GLFW_PRESS
    *///? } else {
    org.lwjgl.sdl.SDLMouse.SDL_GetMouseState(null, null) and (1 shl (button - 1)) != 0
    //? }
}
