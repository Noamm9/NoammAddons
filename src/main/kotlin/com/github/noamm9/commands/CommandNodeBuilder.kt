package com.github.noamm9.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.SharedSuggestionProvider

class CommandNodeBuilder(private val builder: ArgumentBuilder<FabricClientCommandSource, *>) {
    fun runs(block: (CommandContext<FabricClientCommandSource>) -> Unit) {
        builder.executes { context ->
            block(context)
            Command.SINGLE_SUCCESS
        }
    }

    fun literal(name: String, block: CommandNodeBuilder.() -> Unit) {
        val subNode = ClientCommandManager.literal(name)
        CommandNodeBuilder(subNode).apply(block)
        builder.then(subNode)
    }

    fun <T> argument(name: String, type: ArgumentType<T>, block: CommandNodeBuilder.() -> Unit) {
        val argNode = ClientCommandManager.argument(name, type)
        CommandNodeBuilder(argNode).apply(block)
        builder.then(argNode)
    }

    fun suggests(provider: SuggestionProvider<FabricClientCommandSource>) {
        if (builder is RequiredArgumentBuilder<*, *>) {
            (builder as RequiredArgumentBuilder<FabricClientCommandSource, *>).suggests(provider)
        }
    }

    fun suggests(strings: () -> Iterable<String>) {
        if (builder is RequiredArgumentBuilder<*, *>) {
            (builder as RequiredArgumentBuilder<FabricClientCommandSource, *>).suggests { _, suggestionsBuilder ->
                SharedSuggestionProvider.suggest(strings(), suggestionsBuilder)
            }
        }
    }

    fun requires(condition: () -> Boolean) {
        builder.requires { condition() }
    }
}
