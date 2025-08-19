package noammaddons.features.impl.dungeons

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.config.EditGui.GuiElement
import noammaddons.config.EditGui.HudEditorScreen
import noammaddons.events.RenderOverlay
import noammaddons.features.Feature
import noammaddons.features.impl.dungeons.dmap.core.DungeonMapElement.colorizeScore
import noammaddons.features.impl.dungeons.dmap.handlers.ScoreCalculation
import noammaddons.ui.config.core.impl.*
import noammaddons.utils.*
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.sendPartyMessage
import noammaddons.utils.ChatUtils.showTitle
import noammaddons.utils.RenderUtils.drawText
import java.util.concurrent.TimeUnit
import kotlin.reflect.KMutableProperty0

object ScoreCalculator: Feature("Shows the score of the dungeon run") {
    object ScoreCalculatorElement: GuiElement(hudData.getData().scoreCalculator) {
        override val enabled: Boolean get() = ScoreCalculator.enabled && hudElement.value
        private val text: String
            get() {
                if (HudEditorScreen.isOpen()) return "&7Score: &a300"
                val score = colorizeScore(ScoreCalculation.score)
                return "&eScore: $score"
            }
        override val width: Float get() = RenderHelper.getStringWidth(text)
        override val height: Float get() = 9f

        override fun draw() = drawText(text, getX(), getY(), getScale())
    }

    private val hudElement = ToggleSetting("HUD Element")
    private val sendMessageOn270Score = ToggleSetting("Send message on 270 score", false)
    private val message270Score = TextInputSetting("Message for 270 score", "270 Score!").addDependency(sendMessageOn270Score)
    private val createTitleOn270Score = ToggleSetting("Create Title on 270 score", false)
    private val messageTitle270Score = TextInputSetting("270 Title Message", "270 Score!").addDependency(createTitleOn270Score)

    private val sendMessageOn300Score = ToggleSetting("Send message on 300 score", false)
    private val message300Score = TextInputSetting("Message for 300 score", "300 Score!").addDependency(sendMessageOn300Score)
    private val createTitleOn300Score = ToggleSetting("Create Title on 300 score", false)
    private val messageTitle300Score = TextInputSetting("300 Title Message", "300 Score!").addDependency(createTitleOn300Score)

    override fun init() {
        addSettings(
            hudElement,
            sendMessageOn270Score, message270Score,
            createTitleOn270Score, messageTitle270Score,
            sendMessageOn300Score, message300Score,
            createTitleOn300Score, messageTitle300Score
        )

        ThreadUtils.loop(1000) { ScoreCalculation.score }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderOverlay) {
        if (HudEditorScreen.isOpen()) return
        if (! LocationUtils.inDungeon) return
        ScoreCalculatorElement.draw()
    }

    fun on270Score() = onScoreMileStone(270, sendMessageOn270Score, message270Score, createTitleOn270Score, messageTitle270Score, ScoreCalculation::alerted270)
    fun on300Score() = onScoreMileStone(300, sendMessageOn300Score, message300Score, createTitleOn300Score, messageTitle300Score, ScoreCalculation::alerted300)

    private fun onScoreMileStone(
        score: Int,
        sendMessageToggle: ToggleSetting,
        messageText: Component<String>,
        createTitleToggle: ToggleSetting,
        titleText: Component<String>,
        field: KMutableProperty0<Boolean>
    ) {
        if (! enabled) return
        if (field.get()) return
        if (sendMessageToggle.value) sendPartyMessage(messageText.value)
        if (createTitleToggle.value) showTitle(titleText.value)

        val floorColor = if (LocationUtils.isMasterMode) "&c" else "&a"
        val time = formatTime(ScoreCalculation.secondsElapsed)
        modMessage("&e$score&a score reached in &6$time &f|| $floorColor${LocationUtils.dungeonFloor}.")
        repeat(2) { mc.thePlayer.playSound("random.orb", 1f, 0f) }
        field.set(true)
    }

    private fun formatTime(totalSeconds: Int): String {
        if (totalSeconds <= 0) return "0s"

        val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
        val seconds = totalSeconds % 60

        val formattedTime = StringBuilder()
        if (hours > 0) formattedTime.append(hours).append("h ")
        if (minutes > 0) formattedTime.append(minutes).append("m ")
        if (seconds > 0 || formattedTime.isEmpty()) formattedTime.append(seconds).append("s")

        return formattedTime.toString().trim { it <= ' ' }
    }
}
