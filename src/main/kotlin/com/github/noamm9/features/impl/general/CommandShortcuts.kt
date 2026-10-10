package com.github.noamm9.features.impl.general

import com.github.noamm9.config.PogObject
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.ICommandNode
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket
import java.util.concurrent.ConcurrentHashMap

object CommandShortcuts: Feature("Create your own command shortcuts") {
    val shortcuts = PogObject("commandShortcuts", linkedMapOf<String, String>())
    private var currentDispatcher: CommandDispatcher<FabricClientCommandSource>? = null
    private var registeredShortcuts = emptySet<String>()
    private val pendingSuggestionRewrites = ConcurrentHashMap<Int, Int>()

    override fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> build(dispatcher) }
    }

    fun rewriteCommand(command: String) = if (enabled) findRewrite(command)?.rewritten ?: command else command

    fun rewriteSuggestionPacket(packet: ServerboundCommandSuggestionPacket): ServerboundCommandSuggestionPacket {
        if (! enabled) return packet
        val rewrite = findRewrite(packet.command) ?: return packet
        if (rewrite.lengthDelta != 0) pendingSuggestionRewrites[packet.id] = rewrite.lengthDelta
        return ServerboundCommandSuggestionPacket(packet.id, rewrite.rewritten)
    }

    fun restoreSuggestionPacket(packet: ClientboundCommandSuggestionsPacket): ClientboundCommandSuggestionsPacket {
        val lengthDelta = pendingSuggestionRewrites.remove(packet.id()) ?: return packet
        return ClientboundCommandSuggestionsPacket(
            packet.id(),
            (packet.start() - lengthDelta).coerceAtLeast(0),
            packet.length(),
            packet.suggestions()
        )
    }

    fun build(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        if (currentDispatcher !== dispatcher) registeredShortcuts = emptySet()
        currentDispatcher = dispatcher
        val currentShortcuts = shortcuts.get().toMap()
        removeShortcuts(registeredShortcuts - currentShortcuts.keys, dispatcher)
        injectShortcuts(currentShortcuts, dispatcher)
        registeredShortcuts = currentShortcuts.keys
    }

    private fun injectShortcuts(shortcuts: Map<String, String>, dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        for ((shortcut, replacement) in shortcuts) {
            val shortcutParts = commandParts(shortcut)
            if (shortcutParts.isEmpty()) continue

            var parent: CommandNode<FabricClientCommandSource> = dispatcher.root
            for (part in shortcutParts.dropLast(1)) {
                parent = parent.getChild(part)
                    ?: LiteralArgumentBuilder.literal<FabricClientCommandSource>(part).build().also(parent::addChild)
            }

            val name = shortcutParts.last()
            parent.removeChild(name)

            val target = findTarget(dispatcher, replacement)
            val shortcutBuilder = LiteralArgumentBuilder.literal<FabricClientCommandSource>(name)
            if (target != null) shortcutBuilder.redirect(target)
            parent.addChild(shortcutBuilder.build())
        }
    }

    fun removeShortcuts(remove: Set<String>, dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        for (shortcut in remove) {
            val parts = commandParts(shortcut)
            if (parts.isEmpty()) continue
            val parent = if (parts.size == 1) dispatcher.root
            else dispatcher.findNode(parts.dropLast(1)) ?: continue
            parent.removeChild(parts.last())
        }
    }

    private fun CommandNode<FabricClientCommandSource>.removeChild(name: String) {
        val accessor = this as ICommandNode
        accessor.children.remove(name)
        accessor.literals.remove(name)
        accessor.arguments.remove(name)
    }

    private fun findTarget(dispatcher: CommandDispatcher<FabricClientCommandSource>, replacement: String): CommandNode<FabricClientCommandSource>? {
        var node: CommandNode<FabricClientCommandSource> = dispatcher.root
        for (part in commandParts(replacement)) {
            node = node.getChild(part) ?: return node.takeIf { it !== dispatcher.root }
        }
        return node.takeIf { it !== dispatcher.root }
    }

    private fun findRewrite(command: String): CommandRewrite? {
        val commandEnd = command.indexOfFirst(Char::isWhitespace).takeIf { it >= 0 } ?: command.length
        if (commandEnd == 0) return null

        val shortcut = command.substring(0, commandEnd).removePrefix("/").lowercase()
        val configuredShortcuts = shortcuts.get()
        val replacement = configuredShortcuts[shortcut] ?: configuredShortcuts["/$shortcut"] ?: return null

        val replacementCommand = replacement.trim().removePrefix("/")
        if (replacementCommand.isEmpty()) return null

        val replacementPrefix = if (command.startsWith('/')) "/$replacementCommand" else replacementCommand
        return CommandRewrite(rewritten = replacementPrefix + command.substring(commandEnd), lengthDelta = replacementPrefix.length - commandEnd)
    }

    private data class CommandRewrite(val rewritten: String, val lengthDelta: Int)

    private fun commandParts(command: String) = command.trim().removePrefix("/").split(Regex("\\s+")).filter(String::isNotEmpty)
}