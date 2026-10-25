package com.github.noamm9.event.impl

import com.github.noamm9.event.Event

class MessageSentEvent(var message: String, val isCommand: Boolean): Event(cancelable = true)