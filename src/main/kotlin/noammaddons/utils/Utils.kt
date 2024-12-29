package noammaddons.utils

import gg.essential.universal.UChat
import net.minecraft.network.Packet
import noammaddons.noammaddons.Companion.mc
import noammaddons.utils.ChatUtils.getCenteredText
import noammaddons.utils.ChatUtils.getChatBreak
import noammaddons.utils.SoundUtils.chipiChapa
import noammaddons.utils.ThreadUtils.setTimeout


object Utils {
    fun Any?.isNull(): Boolean = this == null

    fun <T> splitArray(array: List<T>, size: Int): List<List<T>> {
        return array.chunked(size)
    }

    fun playFirstLoadMessage() {
        chipiChapa.start()
        listOf(
            "&b&m${getChatBreak()?.substring(1)}",
            "§b§lThanks for installing NoammAddons§r §6§lForge!",
            "",
            "§dUse §b§l/no§lamma§lddons §r§eto access settings.",
            "§eAlias: §b§l/na.",
            "",
            "§dTo list all mod commands, Use §b§l/na help",
            "§aand lastly join my §9§ldiscord server",
            "§9§lhttps://discord.gg/pj9mQGxMxB",
            "&b&m${getChatBreak()?.substring(1)}"
        ).run {
            UChat.chat(joinToString("\n") { getCenteredText(it) })
        }
    }


    fun Any?.equalsOneOf(vararg other: Any?): Boolean = other.any { this == it }

    fun Any?.containsOneOf(vararg other: Any): Boolean {
        return when (this) {
            is String -> other.any { this.contains("$it") }
            is Collection<*> -> other.any { this.contains(it) }
            is Array<*> -> other.any { this.contains(it) }
            else -> false
        }
    }

    fun Packet<*>.send(delay: Long? = null) {
        if (delay == null) mc.netHandler.networkManager.sendPacket(this)
        else setTimeout(delay) { mc.netHandler.networkManager.sendPacket(this) }
    }
}