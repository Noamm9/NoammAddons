package com.github.noamm9.features.impl.general

import com.github.noamm9.NoammAddons
import com.github.noamm9.commands.CommandBuilder
import com.github.noamm9.config.PogObject
import com.github.noamm9.config.types.ToggleSetting
import com.github.noamm9.event.impl.ChatMessageEvent
import com.github.noamm9.event.impl.MouseClickEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.init.DataDownloader
import com.github.noamm9.init.types.ICommandProvider
import com.github.noamm9.interfaces.IChatComponent
import com.github.noamm9.ui.notification.NotificationManager
import com.github.noamm9.utils.ChatUtils
import com.github.noamm9.utils.ChatUtils.removeFormatting
import com.github.noamm9.utils.ChatUtils.unformattedText
import com.github.noamm9.utils.NumbersUtils
import com.github.noamm9.utils.catch
import com.github.noamm9.utils.ColorUtils.char
import com.github.noamm9.utils.ColorUtils.color
import com.github.noamm9.utils.ColorUtils.isColor
import com.mojang.brigadier.arguments.StringArgumentType
import gg.essential.universal.UKeyboard
import gg.essential.universal.UMinecraft
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object Chat: Feature("Useful tweaks for the chat such as Ctrl + Click to copy messages."), ICommandProvider {
    private val ctrlClickToCopy by ToggleSetting("Ctrl Click to Copy", true).withDescription("Ctrl + Left Click a message to copy it to your clipboard.")
    private val removeUselessMessages by ToggleSetting("Remove useless messages", true).withDescription("Removes a lot of useless messages from the chat.")

    //#if CHEAT
    private val autoDialogue by ToggleSetting("Auto dialogue").withDescription("Automatically continues dialogues with NPCs.")
    //#endif

    private val uselessMessages by lazy { DataDownloader.loadJson<List<String>>("uselessMessages.json").map(::Regex) }
    private val customHiders = PogObject("customHiders", mutableListOf<Regex>())

    private val explosiveShotRegex = Regex("^Your Explosive Shot hit (\\d+) (enemy|enemies) for ([\\d,.]+) damage\\.$") // https://regex101.com/r/xsBn8E/1
    private var lastMessageBlank = false

    override fun CommandBuilder.command() {
        setName("chathider")
        runs { ChatUtils.modMessage("&bUsage: /chathider <add|remove|list>") }

        literal("add") {
            argument("regex", StringArgumentType.greedyString()) {
                runs {
                    val str = StringArgumentType.getString(it, "regex")
                    val list = customHiders.get().map(Regex::pattern)
                    if (str in list) return@runs ChatUtils.modMessage("&cRegex is Already in the list!")
                    val regex = catch { Regex(str) } ?: return@runs ChatUtils.modMessage("&cInvalid Regex syntax!")
                    customHiders.get().add(regex)
                    customHiders.save()
                    ChatUtils.modMessage("&aSaved new Regex.")
                }
            }
        }

        literal("remove") {
            argument("regex", StringArgumentType.greedyString()) {
                suggests { customHiders.get().map(Regex::pattern) }
                runs {
                    val str = StringArgumentType.getString(it, "regex")
                    val removed = customHiders.get().removeIf { it.pattern == str }
                    ChatUtils.modMessage(if (removed) "&aRemoved Regex." else "&cNo matching regex found.")
                    customHiders.save()
                }
            }
        }

        literal("list") {
            runs {
                ChatUtils.modMessage("customHiders:")
                ChatUtils.chat(customHiders.get().joinToString("\n") { it.pattern })
            }
        }
    }

    override fun init() {
        register<MouseClickEvent> {
            if (! ctrlClickToCopy.value) return@register
            if (UMinecraft.currentScreenObj !is ChatScreen) return@register
            if (event.button != 0) return@register
            if (event.action != GLFW.GLFW_PRESS) return@register
            if (! UKeyboard.isCtrlKeyDown()) return@register
            val message = getHoveredMsg().takeUnless(String::isBlank) ?: return@register

            NotificationManager.push("Message copied to clipboard", message)
            mc.keyboardHandler.clipboard = message
            event.isCanceled = true
        }

        register<ChatMessageEvent> {
            if (! removeUselessMessages.value) return@register
            val (sHits, _, fDamage) = explosiveShotRegex.find(event.unformattedText)?.destructured ?: return@register
            val hits = sHits.toIntOrNull() ?: return@register
            val totalDamage = fDamage.replace(",", "").toDoubleOrNull() ?: return@register
            val damagePerEntity = if (hits > 0) totalDamage / hits else totalDamage
            ChatUtils.modMessage("&aExplosive shot did &e${NumbersUtils.format(damagePerEntity.toLong())}&a damage per enemy.")
        }

        //#if CHEAT
        // https://github.com/jcnlk/quoi/blob/6e74cc3536aa1db91fe4a134254668a96c2ea072/src/main/kotlin/quoi/module/impl/general/chat/impl/AutoDialogue.kt#L4
        register<ChatMessageEvent> {
            if (! autoDialogue.value) return@register
            if ("[BARBARIANS] [MAGES]" in event.unformattedText) return@register
            if (! event.unformattedText.startsWith("Select an option: ")) return@register
            val cmd = (event.component.siblings.getOrNull(0)?.style?.clickEvent as? ClickEvent.RunCommand)?.command ?: return@register
            ChatUtils.sendCommand(cmd)
        }
        //#endif
    }

    @JvmStatic
    fun addMassageHook(comp: Component, ci: CallbackInfo) {
        if (! enabled) return
        if (! removeUselessMessages.value) return
        val msg = comp.unformattedText

        if (msg.isBlank()) return if (lastMessageBlank) ci.cancel() else ::lastMessageBlank.set(true)

        if (uselessMessages.any { it.matches(msg) } || customHiders.get().any { it.matches(msg) }) return ci.cancel()

        lastMessageBlank = false
    }

    private fun getHoveredMsg(): String {
        val chatHud = (UMinecraft.getChatGUI() as? IChatComponent) ?: return ""
        val i = chatHud.getLineIndex().takeUnless { it < 0 || it >= chatHud.visibleMessages.size } ?: return ""

        val lines = ArrayList<GuiMessage.Line>()
        val builder = StringBuilder()

        for (j in i.toInt() + 1 until chatHud.visibleMessages.size) {
            val line = chatHud.visibleMessages[j]
            if (line.endOfEntry()) break
            lines.add(0, line)
        }

        for (j in i.toInt() downTo 0) {
            val line = chatHud.visibleMessages[j]
            lines.add(line)
            if (line.endOfEntry()) break
        }

        for (line in lines) {
            var lastStyle: Style? = null
            line.content().accept { _, style, codePoint ->
                if (style != lastStyle) {
                    style.color?.let { textColor ->
                        ChatFormatting.entries.firstOrNull { it.isColor && it.color == textColor.value }?.let {
                            builder.append(it)
                        }
                    }

                    if (style.isBold) builder.append(ChatFormatting.BOLD)
                    if (style.isItalic) builder.append(ChatFormatting.ITALIC)
                    if (style.isUnderlined) builder.append(ChatFormatting.UNDERLINE)
                    if (style.isStrikethrough) builder.append(ChatFormatting.STRIKETHROUGH)
                    if (style.isObfuscated) builder.append(ChatFormatting.OBFUSCATED)
                    lastStyle = style
                }
                builder.appendCodePoint(codePoint)
                true
            }
        }

        val text = builder.toString()
        return if ("chat" in NoammAddons.debugFlags) text else text.removeFormatting()
    }
}