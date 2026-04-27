package com.github.noamm9.utils

import net.minecraft.util.Util
import java.awt.Color
import java.net.URI

object Utils {
    val favoriteColor = Color(0, 134, 255)

    fun openDiscordLink() {
        val link = "h*#t#t~p*s:/#/*d*is#c~o~r*d.~g~~*g#*/*p~j9*#m*QG~x#*M*#xB~".remove("#", "~", "*")
        Util.getPlatform().openUri(URI(link))
    }
}