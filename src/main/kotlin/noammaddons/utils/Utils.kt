package noammaddons.utils

import gg.essential.universal.UChat
import net.minecraft.world.World
import noammaddons.noammaddons.Companion.mc
import java.awt.Color
import java.util.*
import kotlin.math.abs
import kotlin.math.sin


object Utils {
	fun ChromaColor(speed: Number = 1): Color {
		val time = System.currentTimeMillis()
		val divider = (1000.0 * speed.toFloat())
		
		return Color(
			Math.round(255 * abs(sin((time / divider) + 2))).toInt(),
			Math.round(255 * abs(sin(time / divider))).toInt(),
			Math.round(255 * abs(sin((time / divider) + 4))).toInt()
		)
	}
	
	fun Any?.isNull(): Boolean = this == null
	
	fun <T> splitArray(array: List<T>, size: Int): List<List<T>> {
		return array.chunked(size)
	}
	
	fun playFirstLoadMessage() {
		SoundUtils.chipiChapa.start()
		val centeredTexts = listOf(
			"§b§lThanks for installing NoammAddons§r §6§lForge!",
			"",
			"§dUse §b§l/no§lamma§lddons §r§eto access settings.",
			"§eAlias: §b§l/na.",
			"",
			"§dTo list all mod commands, Use §b§l/na help",
			"§aand lastly join my §9§ldiscord server",
			"§9§lhttps://discord.gg/pj9mQGxMxB"
		)
		
		UChat.chat("""&b&m${ChatUtils.getChatBreak()?.substring(1)}
${centeredTexts.joinToString("\n") { ChatUtils.getCenteredText(it) }}
&b&m${ChatUtils.getChatBreak()?.substring(1)}""".trim().trimIndent())
	}
}