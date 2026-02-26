package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent

abstract class KeyboardEvent() : Event(cancelable = true) {
    class KeyPressed(val keyEvent: KeyEvent) : KeyboardEvent()
    class CharTyped(val charEvent: CharacterEvent) : KeyboardEvent()
}