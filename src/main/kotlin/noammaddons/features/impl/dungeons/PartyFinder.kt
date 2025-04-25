package noammaddons.features.impl.dungeons

import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noammaddons.events.AddMessageToChatEvent
import noammaddons.features.Feature
import noammaddons.features.impl.gui.Menus.impl.CustomPartyFinderMenu
import noammaddons.noammaddons.Companion.CHAT_PREFIX
import noammaddons.ui.config.core.impl.ToggleSetting
import noammaddons.utils.ChatUtils.addColor
import noammaddons.utils.ChatUtils.modMessage
import noammaddons.utils.ChatUtils.removeFormatting
import noammaddons.utils.SoundUtils

// todo pf join stats message and autokick?
object PartyFinder: Feature("A group of many features regarding the dungeon ape finder") {
    val customMenu = ToggleSetting("Custom Menu")
    private val reformatPfMessages = ToggleSetting("Cleaner Messages")
    private val joinedSound = ToggleSetting("Join Sound")
    override fun init() = addSettings(customMenu, reformatPfMessages, joinedSound)

    private val joinedRegex = Regex("^&dParty Finder &r&f> (.+?) &r&ejoined the dungeon group! \\(&r&b(\\w+) Level (\\d+)&r&e\\)&r\$".addColor())
    private val playerClassChangeRegex = Regex("^&dParty Finder &r&f> (.+?) &r&eset their class to &r&b(\\w+) Level (\\d+)&r&e!&r\$".addColor())
    private val messageReplacements = mapOf(
        "Party Finder > Your party has been queued in the dungeon finder!" to "&d&lPF > &aParty Queued.",
        "Party Finder > Your group has been de-listed!" to "&d&lPF > &aParty Delisted."
    )

    init {
        onChat(Regex("Party Finder > .+ joined the dungeon group! \\(.+ Level \\d+\\)"), { joinedSound.value }) {
            SoundUtils.Pling()
        }
    }

    override fun onEnable() {
        super.onEnable()
        MinecraftForge.EVENT_BUS.register(CustomPartyFinderMenu)
    }

    override fun onDisable() {
        super.onDisable()
        MinecraftForge.EVENT_BUS.unregister(CustomPartyFinderMenu)
    }


    @SubscribeEvent
    fun onNewChatMessage(event: AddMessageToChatEvent) {
        if (! reformatPfMessages.value) return
        val text = event.component.formattedText

        when {
            "joined the dungeon group!" in text -> {
                val (playerFormatted, clazz, level) = joinedRegex.find(text)?.destructured ?: return
                val unformattedPlayer = playerFormatted.removeFormatting()

                val baseComp = ChatComponentText("$CHAT_PREFIX &d&lPF > $playerFormatted &8| &b$clazz $level".addColor())

                if (unformattedPlayer != mc.session.username) listOf(
                    createComponent(" &8| &c[Kick]", "/p kick $unformattedPlayer", "&c/p kick $unformattedPlayer"),
                    createComponent(" &7[Block]", "/block add $unformattedPlayer", "&7/block add $unformattedPlayer"),
                    createComponent(" &d[PV]", "/pv $unformattedPlayer", "&d/pv $unformattedPlayer")
                ).forEach(baseComp::appendSibling)

                mc.thePlayer?.addChatMessage(baseComp)
                event.isCanceled = true
            }

            "set their class to" in text -> {
                val (player, clazz, level) = playerClassChangeRegex.find(text)?.destructured ?: return
                modMessage("&d&lPF > &r$player &echanged to &b$clazz $level&e!")
                event.isCanceled = true
            }

            else -> messageReplacements[text.removeFormatting()]?.let {
                event.isCanceled = true
                modMessage(it)
            }
        }
    }

    private fun createComponent(text: String, command: String, hoverText: String): ChatComponentText {
        return ChatComponentText(text.addColor()).apply {
            chatStyle = ChatStyle().apply {
                chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
                chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hoverText.addColor()))
            }
        }
    }
}
