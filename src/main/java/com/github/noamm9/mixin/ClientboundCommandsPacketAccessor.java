package com.github.noamm9.mixin;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ClientboundCommandsPacket.class)
public interface ClientboundCommandsPacketAccessor {

    @Mutable
    @Accessor("entries")
    void setEntries(List<?> entries);

    @Mutable
    @Accessor("rootIndex")
    void setRootIndex(int index);

    @Invoker("enumerateNodes")
    static Object2IntMap<CommandNode<SharedSuggestionProvider>> callEnumerateNodes(RootCommandNode<SharedSuggestionProvider> root) {
        throw new AssertionError();
    }

    @Invoker("createEntries")
    static List<?> callCreateEntries(Object2IntMap<CommandNode<SharedSuggestionProvider>> nodes, ClientboundCommandsPacket.NodeInspector<SharedSuggestionProvider> inspector) {
        throw new AssertionError();
    }
}