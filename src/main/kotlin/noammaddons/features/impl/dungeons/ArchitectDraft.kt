package noammaddons.features.impl.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.Chat
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.dmap.core.map.RoomType
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.noFormatText
import noammaddons.utils.ChatUtils.sendChatMessage
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.LocationUtils.inBoss
import noammaddons.utils.LocationUtils.inDungeon
import noammaddons.utils.ScanUtils
import noammaddons.utils.ThreadUtils.setTimeout


object ArchitectDraft: Feature("auto architect draft and anounces resets") {
    private val resetPattern = Regex("^You used the Architect's First Draft to reset (.+)!\$")
    private val failPattern1 = Regex("^PUZZLE FAIL! (?<player>\\w{1,16}) .+$")
    private val failPattern2 = Regex("^\\[STATUE] Oruo the Omniscient: (?<player>\\w{1,16}) chose the wrong answer! I shall never forget this moment of misrememberance\\.$")

    val sayDraft = ToggleSetting("Announce Draft", true)
    val autoDraft = ToggleSetting("Auto Draft", true)

    override fun init() = addSettings(sayDraft, autoDraft)

    @SubscribeEvent
    fun onChat(event: Chat) {
        if (! inDungeon || inBoss) return
        if (ScanUtils.currentRoom?.data?.type != RoomType.PUZZLE) return
        val msg = event.component.noFormatText

        if (sayDraft.value) resetPattern.find(msg)?.destructured?.component1()?.let {
            sendPartyMessage("Used Draft to Reset $it")
        }

        if (autoDraft.value) {
            val match = failPattern1.find(msg) ?: failPattern2.find(msg)
            val name = match?.destructured?.component1()
            if (name == mc.session.username) setTimeout(1500) {
                sendChatMessage("/gfs ARCHITECT_FIRST_DRAFT 1")
            }
        }
    }
}
