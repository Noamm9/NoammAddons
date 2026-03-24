package com.github.noamm9.utils.location

import com.github.noamm9.event.EventBus
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.WorldChangeEvent
import com.google.gson.JsonParser

object LocrawListener {
    var server = ""
    var gameType = ""
    var location = ""
    var map = ""

    fun init() {
        EventBus.register<ChatMessageEvent> {
            if (! LocationUtils.onHypixel) return@register
            parseLocRaw(event.unformattedText)
        }

        EventBus.register<WorldChangeEvent> { reset() }
    }

    private fun parseLocRaw(message: String) {
        if (! message.startsWith("{\"server\":") || ! message.endsWith("}")) return
        val locRaw = JsonParser.parseString(message).getAsJsonObject()
        if (locRaw.has("server")) server = locRaw.get("server").asString
        if (locRaw.has("gametype")) gameType = locRaw.get("gametype").asString
        if (locRaw.has("mode")) location = locRaw.get("mode").asString
        if (locRaw.has("map")) map = locRaw.get("map").asString
    }

    private fun reset() {
        server = ""
        gameType = ""
        location = ""
        map = ""
    }
}