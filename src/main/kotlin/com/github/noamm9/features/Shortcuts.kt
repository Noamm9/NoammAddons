package com.github.noamm9.features

import com.github.noamm9.NoammAddons

interface Shortcuts {
    val Shortcuts.mc get() = NoammAddons.mc
    val Shortcuts.scope get() = NoammAddons.scope
    val Shortcuts.player get() = requireNotNull(mc.player) { "Tried to access the player before it was loaded" }
    val Shortcuts.level get() = requireNotNull(mc.level) { "Tried to access the level before the world was loaded" }
    val Shortcuts.gameMode get() = requireNotNull(mc.gameMode) { "mc.gameMode is null" }
}