package com.github.noamm9.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.SharedSuggestionProvider

class CommandBuilder {
    private lateinit var names: Array<out String>
    private var executor: ((CommandContext<FabricClientCommandSource>) -> Unit)? = null
    private val children = mutableListOf<CommandBuilder>()
    private var requirement: (() -> Boolean)? = null
    private var descriptionText: String? = null

    private var argumentType: ArgumentType<*>? = null
    private var argumentName: String? = null
    private var literalName: String? = null
    private var suggestionProvider: SuggestionProvider<FabricClientCommandSource>? = null
    private var suggestionLambda: (() -> Iterable<String>)? = null

    fun setName(vararg names: String) = ::names.set(names)
    fun runs(block: (CommandContext<FabricClientCommandSource>) -> Unit) = ::executor.set(block)
    fun suggests(strings: () -> Iterable<String>) = ::suggestionLambda.set(strings)
    fun description(text: String) = ::descriptionText.set(text)

    fun literal(name: String, block: CommandBuilder.() -> Unit) = children.add(CommandBuilder().apply literal@{
        this@literal.literalName = name
        block()
    })

    fun <T: Any> argument(name: String, type: ArgumentType<T>, block: CommandBuilder.() -> Unit) = children.add(CommandBuilder().apply literal@{
        this@literal.argumentName = name
        this@literal.argumentType = type
        block()
    })

    fun build(): List<ArgumentBuilder<FabricClientCommandSource, *>> {
        if (! ::names.isInitialized) error("Command name must be initialized using setName(...)")
        return names.map { ClientCommands.literal(it).also(::setup) }
    }

    fun helpEntries(): List<HelpEntry> {
        if (! ::names.isInitialized) return emptyList()
        return buildList {
            descriptionText?.let { add(HelpEntry(names.first(), it)) }
            children.forEach { it.collect(names.first(), this) }
        }
    }

    data class HelpEntry(val usage: String, val description: String)

    private fun collect(prefix: String, out: MutableList<HelpEntry>) {
        val segment = literalName ?: argumentName?.let { "<$it>" } ?: return
        val path = "$prefix $segment"
        descriptionText?.let { out.add(HelpEntry(path, it)) }
        children.forEach { it.collect(path, out) }
    }

    private fun setup(target: ArgumentBuilder<FabricClientCommandSource, *>) {
        executor?.let { exec ->
            target.executes { context ->
                exec(context)
                Command.SINGLE_SUCCESS
            }
        }

        requirement?.let { req ->
            target.requires { req() }
        }

        val reqTarget = target as? RequiredArgumentBuilder<FabricClientCommandSource, *>
        if (reqTarget != null) {
            suggestionProvider?.let(reqTarget::suggests)
            suggestionLambda?.let { lambda ->
                reqTarget.suggests { _, suggestionsBuilder ->
                    SharedSuggestionProvider.suggest(lambda(), suggestionsBuilder)
                }
            }
        }

        children.forEach { child ->
            val childBuilder = if (child.literalName != null) ClientCommands.literal(child.literalName !!)
            else if (child.argumentName != null && child.argumentType != null) ClientCommands.argument(child.argumentName !!, child.argumentType !!)
            else return@forEach

            child.setup(childBuilder)
            target.then(childBuilder)
        }
    }
}