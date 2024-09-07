package NoammAddons.features.dungeons.terminals

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.RenderTitleEvent
import NoammAddons.utils.ChatUtils.Text
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.RenderUtils
import NoammAddons.utils.RenderUtils.getHeight
import NoammAddons.utils.RenderUtils.getWidth
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BetterF7TerminalsTitles {

    private val infoText = Text("", .0, .0, 2.0)
    private val typeText = Text("",.0,.0,2.0)
    private const val msTime = 4_000L
    private val termCrystalRegex = Regex("activated a terminal! \\((\\d+/\\d+)\\)|completed a device! \\((\\d+/\\d+)\\)|activated a lever! \\((\\d+/\\d+)\\)|(\\d+/\\d+) Energy Crystals are now active!")
    private val progressRegex = Regex("\\d+/\\d+")
    private var type: String? = null
    private var progress: String? = null
    private var startTime: Long = 0L
    private var startRegister = false
    private var RenderOverlay = false
    private var cancelTitles = false
    private val replacements = mapOf(
        "1/7" to "&c1&r/&a7", "1/8" to "&c1&r/&a8", "1/2" to "&c1&r/&b2",
        "2/8" to "&c2&r/&a8", "2/7" to "&c2&r/&a7", "2/2" to "&b2&r/&b2",
        "3/" to "&c3&r/&a", "4/" to "&c4&r/&a", "5/" to "&c5&r/&a",
        "6/" to "&c6&r/&a", "7/8" to "&c7&r/&a8", "7/7" to "&6&l7&r&l/&6&l7",
        "8/8" to "&6&l8&r&l/&6&l8"
    )

    private fun colorTerminalInfo(progress: String): String {
        var result = progress
        replacements.forEach { (key, value) ->
            result = result.replace(key, value)
        }
        return result
    }


    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 3) return
        if (!startRegister) return

        val msg = event.message.unformattedText.removeFormatting()
        val match = termCrystalRegex.find(msg)

        val matchString = match?.groupValues?.get(0) ?: return
        progress = progressRegex.find(matchString)?.groupValues?.get(0) ?: return

        type = when {
            matchString.contains("device") -> "Dev"
            matchString.contains("lever") -> "Lever"
            matchString.contains("terminal") -> "Term"
            matchString.contains("Crystal") -> " "
            else -> ""
        }

        startTime = System.currentTimeMillis()

        infoText.text = "&r(${colorTerminalInfo(progress!!)}&r)".addColor()
        infoText.x = ((mc.getWidth()/2) - mc.fontRendererObj.getStringWidth(infoText.text.removeFormatting())).toDouble()
        infoText.y = ((mc.getHeight()/2) - (mc.getHeight()/13)).toDouble()

        typeText.text = "&d$type".addColor()
        typeText.x = ((mc.getWidth() / 2) - mc.fontRendererObj.getStringWidth(typeText.text.removeFormatting())/2).toDouble()
        typeText.y = ((mc.getHeight() / 2) - (mc.getHeight() / 13)).toDouble()

        RenderOverlay = true
        cancelTitles = true
    }


    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return
        if (!RenderOverlay) return
        val timeLeft = msTime - (System.currentTimeMillis() - startTime)

        RenderUtils.drawText(infoText.text, infoText.x, infoText.y, infoText.scale)

        RenderUtils.drawText(typeText.text, typeText.x, typeText.y, typeText.scale)

        if (timeLeft <= 0) RenderOverlay = false
    }

    @SubscribeEvent
    fun onTitle(event: RenderTitleEvent) {
        if (cancelTitles) event.isCanceled = true
    }

    @SubscribeEvent
    fun onBossMsg(event: ClientChatReceivedEvent) {
        if (!config.BetterF7TerminalsTitles) return

        when (event.message.unformattedText.removeFormatting()) {
            "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!",
            "[BOSS] Storm: I should have known that I stood no chance." -> {
                cancelTitles = true
                startRegister = true
            }
            "[BOSS] Maxor: I'M TOO YOUNG TO DIE AGAIN!", "[BOSS] Goldor: You have done it" -> {
                cancelTitles = false
                startRegister = false
            }
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        cancelTitles = false
        startRegister = false
    }

}