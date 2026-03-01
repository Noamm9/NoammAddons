package com.github.noamm9.commands

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.features.impl.misc.WarpShortcuts
import com.github.noamm9.mixin.ClientboundCommandsPacketAccessor
import com.github.noamm9.mixin.CommandNodeAccessor
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import com.mojang.brigadier.tree.RootCommandNode
import io.github.classgraph.ClassGraph
import it.unimi.dsi.fastutil.objects.Object2IntMap
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.protocol.game.ClientboundCommandsPacket

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
                    val instanceField = classInfo.loadClass().declaredFields.find { it.name == "INSTANCE" }
                    if (instanceField != null) {
                        val instance = instanceField.get(null) as? BaseCommand
                        instance?.let { command ->
                            commands.add(command)
                            NoammAddons.logger.info("Registered command: /${command.name}")
                        }
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
            val root = dispatcher.root as CommandNodeAccessor
            registeredDynamicNodes.forEach { name ->
                root.children.remove(name)
                root.literals.remove(name)
            }
            registeredDynamicNodes.clear()
        } catch (e: Exception) {
            NoammAddons.logger.error("Pruning failed", e)
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
                    override fun suggestionId(node: ArgumentCommandNode<SharedSuggestionProvider, *>) = null
                    override fun isExecutable(node: CommandNode<SharedSuggestionProvider>) = node.command != null
                    override fun isRestricted(node: CommandNode<SharedSuggestionProvider>) = false
                }

                val syncRoot = RootCommandNode<SharedSuggestionProvider>()
                for (nodeName in registeredDynamicNodes) {
                    val originalNode = dispatcher.root.getChild(nodeName) ?: continue
                    if (originalNode is LiteralCommandNode<*>) {
                        val builder = LiteralArgumentBuilder.literal<SharedSuggestionProvider>(originalNode.literal)
                        originalNode.children.forEach { child ->
                            if (child is LiteralCommandNode<*>) {
                                builder.then(LiteralArgumentBuilder.literal<SharedSuggestionProvider>(child.literal))
                            }
                        }
                        syncRoot.addChild(builder.build())
                    }
                }

                val packetClass = ClientboundCommandsPacket::class.java
                val emptyRoot = RootCommandNode<SharedSuggestionProvider>()
                val packet = ClientboundCommandsPacket(emptyRoot, inspector)

                val methods = packetClass.declaredMethods
                val enumerateMethod = methods.find {
                    it.parameterCount == 1 && it.parameterTypes[0] == RootCommandNode::class.java
                }?.apply { isAccessible = true }

                val createEntriesMethod = methods.find {
                    it.parameterCount == 2 &&
                            it.parameterTypes[0] == Object2IntMap::class.java &&
                            it.parameterTypes[1] == ClientboundCommandsPacket.NodeInspector::class.java
                }?.apply { isAccessible = true }

                if (enumerateMethod != null && createEntriesMethod != null) {
                    val nodesMap = enumerateMethod.invoke(null, syncRoot) as Object2IntMap<CommandNode<SharedSuggestionProvider>>
                    val entriesList = createEntriesMethod.invoke(null, nodesMap, inspector) as List<*>

                    val accessor = packet as ClientboundCommandsPacketAccessor
                    accessor.setEntries(entriesList)
                    accessor.setRootIndex(nodesMap.getInt(syncRoot))

                    connection.handleCommands(packet)

                    if (mc.screen is ChatScreen) {
                        // Updated to use mouseScrolled as requested
                        (mc.screen as ChatScreen).mouseScrolled(0.0, 0.0, 0.0, 0.0)
                    }
                    NoammAddons.logger.info("Successfully synced ${registeredDynamicNodes.size} nodes.")
                }
            } catch (e: Exception) {
                NoammAddons.logger.error("Deep sync failed during packet injection", e)
            }
        }
    }

    private fun registerToDispatcher(
        dispatcher: CommandDispatcher<FabricClientCommandSource>,
        name: String,
        nodeSetup: LiteralArgumentBuilder<FabricClientCommandSource>.() -> Unit
    ) {
        val builder = ClientCommandManager.literal(name)
        builder.nodeSetup()
        dispatcher.register(builder)
        registeredDynamicNodes.add(name)
    }
}