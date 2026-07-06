package com.github.noamm9.mixin;

import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(CommandNode.class)
public interface ICommandNode {
    @Accessor("children")
    Map<String, CommandNode<?>> getChildren();

    @Accessor("literals")
    Map<String, CommandNode<?>> getLiterals();
    
    @Accessor("arguments")
    Map<String, CommandNode<?>> getArguments();
}