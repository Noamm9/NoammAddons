package noammaddons.features.impl.alerts

import noammaddons.features.Feature
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.DungeonUtils.thePlayer
import noammaddons.utils.LocationUtils.F7Phase
import noammaddons.utils.SoundUtils
import noammaddons.utils.ThreadUtils.setTimeout

object RagAxe: Feature() {
    private val m7Alert by ToggleSetting("M7 Dragon Alert")

    init {
        onChat(Regex("\\[BOSS] Wither King: You... again?"), { m7Alert && F7Phase == 5 && thePlayer?.isDead == false }) {
            setTimeout(1800) {
                SoundUtils.iHaveNothing()
                showTitle("rag", rainbow = true)
            }
        }
    }
}