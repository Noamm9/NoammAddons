package com.github.noamm9.event.impl

import com.github.noamm9.event.Event

sealed class NoammDebugFlagEvent(val flag: String): Event(false) {
    class Add(flag: String): NoammDebugFlagEvent(flag)
    class Remove(flag: String): NoammDebugFlagEvent(flag)
}