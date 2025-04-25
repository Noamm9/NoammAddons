package noammaddons.features.impl.alerts

import noammaddons.features.Feature
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.LocationUtils.inDungeon


object BloodReady: Feature("Notification for when Watcher finished spawning mobs") {
    private val regex = Regex("^\\[BOSS] The Watcher: (That will be enough for now|You have proven yourself. You may pass\\.)$")

    init {
        onChat(regex, { inDungeon }) {
            mc.thePlayer !!.playSound("random.orb", 1f, 0.5f)
            showTitle("§1[§6§kO§r§1] §dB§bl§do§bo§dd §bD§do§bn§de §1[§6§kO§r§1]")
        }
    }
}
