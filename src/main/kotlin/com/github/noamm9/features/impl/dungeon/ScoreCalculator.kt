package com.github.noamm9.features.impl.dungeon

import com.github.noamm9.event.impl.DungeonEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.ui.clickgui.componnents.*
import com.github.noamm9.ui.clickgui.componnents.impl.TextInputSetting
import com.github.noamm9.ui.clickgui.componnents.impl.ToggleSetting
import com.github.noamm9.ui.hud.getValue
import com.github.noamm9.ui.hud.provideDelegate
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ColorUtils
import com.github.noamm9.utils.dungeons.map.handlers.ScoreCalculation
import com.github.noamm9.utils.location.LocationUtils
import com.github.noamm9.utils.render.Render2D
import com.github.noamm9.utils.render.Render2D.width
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents

object ScoreCalculator: Feature("Shows the score of the dungeon run") {
    val forcePaul by ToggleSetting("Force Paul")
    private val hudElement by ToggleSetting("HUD Element")
    val sendMimic by ToggleSetting("Send Mimic Message")
    val sendPrince by ToggleSetting("Send Prince Message")

    private val sendMsg270 by ToggleSetting("270 score message").section("270")
    private val msg270 by TextInputSetting("Message", "270 Score!").showIf { sendMsg270.value }
    private val title270 by ToggleSetting("270 score Title")
    private val titleMsg270 by TextInputSetting("Title Message", "&e270 Score!").showIf { title270.value }

    private val sendMsg300 by ToggleSetting("300 score message").section("300")
    private val msg300 by TextInputSetting("Message ", "300 Score!").showIf { sendMsg300.value }
    private val title300 by ToggleSetting("300 score Title")
    private val titleMsg300 by TextInputSetting("Title Message ", "&c300 Score!").showIf { title300.value }

    private data class Milestone(
        val score: Int, val sendMessage: ToggleSetting,
        val message: Setting<String>, val sendTitle: ToggleSetting,
        val title: Setting<String>
    )

    private val milestones by lazy {
        listOf(
            Milestone(270, sendMsg270, msg270, title270, titleMsg270),
            Milestone(300, sendMsg300, msg300, title300, titleMsg300)
        )
    }

    val scoreHud by hudElement("ScoreCalculator", enabled = { LocationUtils.inDungeon }, shouldDraw = { hudElement.value }) { ctx, demoMode ->
        val text = if (demoMode) "&eScore: &a300"
        else "&eScore: " + ColorUtils.colorizeScore(ScoreCalculation.score)

        Render2D.drawString(ctx, text, 0, 0)
        return@hudElement text.width().toFloat() to 9f
    }

    override fun init() {
        register<DungeonEvent.Score> {
            milestones.find { it.score == event.score }?.let(::triggerMilestone)
        }
    }

    private fun triggerMilestone(m: Milestone) {
        if (m.sendMessage.value) ChatUtils.sendPartyMessage(m.message.value)
        if (m.sendTitle.value) ChatUtils.showTitle(m.title.value)

        val timeStr = ScoreCalculation.secondsElapsed.formatTime()
        val floorColor = if (LocationUtils.isMasterMode) "&c" else "&a"
        val floorName = LocationUtils.dungeonFloor ?: "?"

        ChatUtils.modMessage("&e${m.score}&a score reached in &6$timeStr &f|| $floorColor$floorName.")
        playSuccessSound()
    }

    private fun playSuccessSound() {
        val sound = SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0F)
        mc.soundManager.play(sound)
        mc.soundManager.play(sound)
    }

    private fun Int.formatTime(): String {
        if (this <= 0) return "0s"
        val h = this / 3600
        val m = (this % 3600) / 60
        val s = this % 60

        return buildString {
            if (h > 0) append("${h}h ")
            if (m > 0) append("${m}m ")
            if (s > 0 || isEmpty()) append("${s}s")
        }.trim()
    }
}