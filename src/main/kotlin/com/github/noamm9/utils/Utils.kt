package com.github.noamm9.utils

import com.github.noamm9.NoammAddons.mc
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerGamePacketListener
import gg.essential.universal.UDesktop
import java.awt.Color
import java.net.URI

object Utils {
    val favoriteColor = Color(0, 134, 255)

    fun openDiscordLink() = UDesktop.browse(URI("https://discord.gg/pj9mQGxMxB"))

    fun Packet<ServerGamePacketListener>.send() = mc.connection?.send(this)
}