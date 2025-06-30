package noammaddons.features.impl.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.NoammAddons.Companion.CHAT_PREFIX
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.ThreadUtils.setTimeout
import java.util.regex.Pattern


object ArchitectDraft: Feature("auto architect draft and anounces resets") {
    private val resetPattern = Pattern.compile("^You used the Architect's First Draft to reset (Higher Or Lower|Boulder|Three Weirdos|Ice Path|Bomb Defuse|Tic Tac Toe)!$")
    private val failPattern1 = Pattern.compile("^PUZZLE FAIL! (\\w{1,16}) .+\$")
    private val failPattern2 = Pattern.compile("^\\[STATUE] Oruo the Omniscient: (\\w{1,16}) chose the wrong answer! I shall never forget this moment of misrememberance\\.$")

    val sayDraft = ToggleSetting("Announce Draft", true)
    val autoDraft = ToggleSetting("Auto Draft", true)

    override fun init() = addSettings(sayDraft, autoDraft)

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! inDungeon) return
        if (inBoss) return
        val msg = event.component.noFormatText

        runCatching {
            val resetMatcher = resetPattern.matcher(msg)
            if (resetMatcher.matches() && sayDraft.value) {
                val type = resetMatcher.group(1)
                sendPartyMessage("$CHAT_PREFIX Used Draft to Reset $type")
            }

            if (! autoDraft.value) return@onChat

            val failMatcher1 = failPattern1.matcher(msg)
            val failMatcher2 = failPattern2.matcher(msg)

            if ((failMatcher1.matches() || failMatcher2.matches())) {
                val player = failMatcher1.group(1) ?: failMatcher2.group(1)

                if (player == mc.session.username) {
                    setTimeout(30 * 50) {
                        sendChatMessage("/gfs ARCHITECT_FIRST_DRAFT 1")
                    }
                }
            }
        }
    }
}
