package com.github.noamm9.commands

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.misc.WarpShortcuts
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.RootCommandNode
import io.github.classgraph.ClassGraph
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.protocol.game.ClientboundCommandsPacket
import net.minecraft.resources.ResourceLocation

object CommandManager {
    val commands = mutableSetOf<BaseCommand>()
    var lastDispatcher: CommandDispatcher<FabricClientCommandSource>? = null
    private val registeredDynamicNodes = mutableSetOf<String>()
    private val dynamicProviders = mutableListOf<(CommandDispatcher<FabricClientCommandSource>) -> Unit>()

    init {
        val scanResult = ClassGraph()
            .enableAllInfo()
            .acceptPackages("com.github.noamm9")
            .ignoreClassVisibility()
            .overrideClassLoaders(Thread.currentThread().contextClassLoader)
            .scan()

        scanResult.use { result ->
            val commandClasses = result.getSubclasses("com.github.noamm9.commands.BaseCommand")
            NoammAddons.logger.info("CommandManager found ${commandClasses.size} commands.")

            commandClasses.forEach { classInfo ->
                try {
                    val instance = classInfo.loadClass().getDeclaredField("INSTANCE").get(null) as? BaseCommand

                    instance?.let { command ->
                        commands.add(command)
                        NoammAddons.logger.info("Registered command: /${command.name}")
                    }
                } catch (e: Exception) {
                    NoammAddons.logger.error("Failed to register command: ${classInfo.name}", e)
                }
            }
        }

        WarpShortcuts.init()
        NoammAddons.logger.info("CommandManager initialized with ${WarpShortcuts.commands.size} commands.")
        registerProvider { dispatcher ->
            if (WarpShortcuts.enabled) {
                WarpShortcuts.commands.forEach { command ->
                    registerToDispatcher(dispatcher, command.input) {
                        CommandNodeBuilder(this).apply { with(command) { build() } }
                    }
                }
            }
        }
    }

    fun registerProvider(provider: (CommandDispatcher<FabricClientCommandSource>) -> Unit) {
        dynamicProviders.add(provider)
    }

    fun registerAll() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            lastDispatcher = dispatcher
            registeredDynamicNodes.clear()
            commands.forEach { command ->
                registerToDispatcher(dispatcher, command.name) {
                    CommandNodeBuilder(this).apply { with(command) { build() } }
                }
            }
            dynamicProviders.forEach { it.invoke(dispatcher) }
        }
    }

    fun updateCommandsAtRuntime() {
        val dispatcher = ClientCommandManager.getActiveDispatcher() ?: lastDispatcher ?: return
        try {
            val root = dispatcher.root
            val childrenField = CommandNode::class.java.getDeclaredField("children").apply { isAccessible = true }
            val literalsField = CommandNode::class.java.getDeclaredField("literals").apply { isAccessible = true }

            val children = childrenField.get(root) as MutableMap<*, *>
            val literals = literalsField.get(root) as MutableMap<*, *>
            registeredDynamicNodes.forEach { name ->
                children.remove(name)
                literals.remove(name)
            }
            registeredDynamicNodes.clear()
        } catch (e: Exception) {
            NoammAddons.logger.error("Failed to prune old command nodes", e)
        }
        commands.forEach { command ->
            registerToDispatcher(dispatcher, command.name) {
                CommandNodeBuilder(this).apply { with(command) { build() } }
            }
        }
        dynamicProviders.forEach { it.invoke(dispatcher) }
        mc.execute {
            val connection = mc.connection ?: return@execute
            try {
                val inspector = object : ClientboundCommandsPacket.NodeInspector<SharedSuggestionProvider> {
                    override fun suggestionId(node: ArgumentCommandNode<SharedSuggestionProvider, *>): ResourceLocation? =
                        null

                    override fun isExecutable(node: CommandNode<SharedSuggestionProvider>): Boolean =
                        node.command != null

                    override fun isRestricted(node: CommandNode<SharedSuggestionProvider>): Boolean = false
                }

                val constructor = ClientboundCommandsPacket::class.java.declaredConstructors.first {
                    it.parameterCount == 2 && RootCommandNode::class.java.isAssignableFrom(it.parameterTypes[0])
                }.apply { isAccessible = true }

                val packet = constructor.newInstance(dispatcher.root, inspector) as ClientboundCommandsPacket
                connection.handleCommands(packet)

                if (mc.screen is ChatScreen) {
                    (mc.screen as ChatScreen).mouseScrolled(0.0, 0.0, 0.0, 0.0)
                }
            } catch (e: Exception) {
                NoammAddons.logger.error("Reflection failed to sync command packet", e)
            }
        }
    }

    private fun registerToDispatcher(
        dispatcher: CommandDispatcher<FabricClientCommandSource>,
        name: String,
        nodeSetup: com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource>.() -> Unit
    ) {
        val builder = ClientCommandManager.literal(name)
        builder.nodeSetup()
        dispatcher.register(builder)
        registeredDynamicNodes.add(name)
    }
}