package NoammAddons.features.dungeons

import NoammAddons.utils.ChatUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import NoammAddons.NoammAddons.Companion.CHAT_PREFIX
import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.ThreadUtils.setTimeout
import java.util.regex.Pattern

object AnnounceDraftResets {
    private val resetPattern = Pattern.compile("You used the Architect's First Draft to reset (Higher Or Lower|Boulder|Three Weirdos|Ice Path|Bomb Defuse|Tic Tac Toe)!")
    private val failPattern1 = Pattern.compile("^PUZZLE FAIL! (\\w{1,16}) .+\$")
    private val failPattern2 = Pattern.compile("^\\[STATUE\\] Oruo the Omniscient: (\\w{1,16}) chose the wrong answer! I shall never forget this moment of misrememberance\\.\$")


    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        val message = event.message.unformattedText.removeFormatting()

        val resetMatcher = resetPattern.matcher(message)
        if (resetMatcher.matches() && config.AnnounceDraftResets) {
            val type = resetMatcher.group(1)
            ChatUtils.sendChatMessage("/pc ${CHAT_PREFIX.removeFormatting()} Used Draft to Reset $type")
        }

        val failMatcher1 = failPattern1.matcher(message)
        val failMatcher2 = failPattern2.matcher(message)

        if ((failMatcher1.matches() || failMatcher2.matches()) && config.AutoArchitectDraft) {
            val player = failMatcher1.group(1) ?: failMatcher2.group(1)

            if (player == mc.session.username) {
                setTimeout(30*50) {
                    ChatUtils.sendChatMessage("/gfs ARCHITECT_FIRST_DRAFT 1")
                }
            }
        }
    }
}
