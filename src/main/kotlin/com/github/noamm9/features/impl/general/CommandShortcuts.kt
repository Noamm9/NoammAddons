package com.github.noamm9.features.impl.general

import com.github.noamm9.config.PogObject
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.ICommandNode
import com.github.noamm9.mixin.IServerboundChatCommandPacket
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

object CommandShortcuts: Feature("Create your own command shortcuts") {
    val shortcuts = PogObject("commandShortcuts", linkedMapOf<String, String>())
    private var currentDispatcher: CommandDispatcher<FabricClientCommandSource>? = null
    private var registeredShortcuts = setOf<String>()

    override fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> build(dispatcher) }

        register<PacketEvent.Sent> {
            val packet = event.packet as? IServerboundChatCommandPacket ?: return@register
            val args = packet.command.split(" ", limit = 2)
            val replacement = shortcuts.get()[args[0].lowercase()] ?: return@register
            val message = if (args.size > 1) "$replacement ${args[1]}" else replacement
            packet.command = message
        }
    }

    fun build(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        currentDispatcher = dispatcher
        val currentKeys = shortcuts.get().keys
        removeShortcuts(registeredShortcuts - currentKeys, dispatcher)
        injectShortcuts(currentKeys, dispatcher)
        registeredShortcuts = currentKeys
    }

    fun injectShortcuts(shortcutKeys: Set<String>, dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        for (shortcut in shortcutKeys) {
            val parts = shortcut.split(" ")
            var node = dispatcher.root.getChild(parts[0]) ?: LiteralArgumentBuilder.literal<FabricClientCommandSource>(parts[0]).build().also(dispatcher.root::addChild)
            for (part in parts.drop(1)) node = node.getChild(part) ?: LiteralArgumentBuilder.literal<FabricClientCommandSource>(part).build().also(node::addChild)
        }
    }

    fun removeShortcuts(remove: Set<String>, dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        for (shortcut in remove) {
            val parts = shortcut.split(" ")
            val parent = if (parts.size == 1) dispatcher.root
            else dispatcher.findNode(parts.dropLast(1)) ?: continue
            val accessor = parent as ICommandNode
            accessor.children.remove(parts.last())
            accessor.literals.remove(parts.last())
        }
    }
}