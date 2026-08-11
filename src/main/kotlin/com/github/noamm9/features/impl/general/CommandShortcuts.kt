package com.github.noamm9.features.impl.general

import com.github.noamm9.config.PogObject
import com.github.noamm9.event.impl.PacketEvent
import com.github.noamm9.features.Feature
import com.github.noamm9.mixin.ICommandNode
import com.github.noamm9.mixin.IServerboundChatCommandPacket
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.RootCommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

@Suppress("UNCHECKED_CAST")
object CommandShortcuts: Feature("Create your own command shortcuts") {
    val shortcuts = PogObject("commandShortcuts", linkedMapOf<String, String>())

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
        shortcuts.get().keys.forEach { key ->
            unregisterNode(dispatcher.root, key)
            dispatcher.register(ClientCommands.literal(key).then(ClientCommands.argument("arguments", StringArgumentType.greedyString())))
        }
    }

    private fun unregisterNode(root: RootCommandNode<FabricClientCommandSource>, key: String) {
        val node = root as ICommandNode
        node.children.remove(key)
        node.literals.remove(key)
        node.arguments.remove(key)
    }
}