package com.github.noamm9.commands

import com.github.noamm9.NoammAddons
import com.github.noamm9.NoammAddons.mc
import com.github.noamm9.mixin.ClientboundCommandsPacketAccessor
import com.github.noamm9.mixin.CommandNodeAccessor
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import com.mojang.brigadier.tree.RootCommandNode
import io.github.classgraph.ClassGraph
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.protocol.game.ClientboundCommandsPacket

object CommandManager {
    val commands = mutableSetOf<BaseCommand>()
    private var lastDispatcher: CommandDispatcher<FabricClientCommandSource>? = null
    private val registeredDynamicNodes = mutableSetOf<String>()

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
    }

    fun registerAll() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            commands.forEach { command ->
                val root = ClientCommandManager.literal(command.name)
                CommandNodeBuilder(root).apply { with(command) { build() } }
                dispatcher.register(root)
                commands.add(command)
                NoammAddons.logger.debug("Registered command: /${command.name}")
            }
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
        commands.forEach { registerToDispatcher(dispatcher, it) }
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
                    if (!originalNode.canUse(dispatcher.root as? FabricClientCommandSource)) continue
                    if (originalNode is LiteralCommandNode<*>) {
                        val builder = LiteralArgumentBuilder.literal<SharedSuggestionProvider>(originalNode.literal)
                        originalNode.children.forEach { child ->
                            if (child is LiteralCommandNode<*>) {
                                builder.then(LiteralArgumentBuilder.literal(child.literal))
                            }
                        }
                        syncRoot.addChild(builder.build())
                    }
                }
                val nodesMap = ClientboundCommandsPacketAccessor.callEnumerateNodes(syncRoot)
                val entriesList = ClientboundCommandsPacketAccessor.callCreateEntries(nodesMap, inspector)
                val emptyRoot = RootCommandNode<SharedSuggestionProvider>()
                val packet = ClientboundCommandsPacket(emptyRoot, inspector)
                val accessor = packet as ClientboundCommandsPacketAccessor
                accessor.setEntries(entriesList)
                accessor.setRootIndex(nodesMap.getInt(syncRoot))
                connection.handleCommands(packet)
            } catch (e: Exception) {
                NoammAddons.logger.error("Runtime sync failed", e)
            }
        }
    }

    private fun registerToDispatcher(dispatcher: CommandDispatcher<FabricClientCommandSource>, command: BaseCommand) {
        val root = ClientCommandManager.literal(command.name)
        CommandNodeBuilder(root).apply { with(command) { build() } }
        dispatcher.register(root)
        registeredDynamicNodes.add(command.name)
    }
}