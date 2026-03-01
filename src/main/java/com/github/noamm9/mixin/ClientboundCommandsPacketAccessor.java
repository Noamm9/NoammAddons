package com.github.noamm9.mixin;

import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ClientboundCommandsPacket.class)
public interface ClientboundCommandsPacketAccessor {

    @Mutable
    @Accessor("entries")
    void setEntries(List<?> entries);

    @Mutable
    @Accessor("rootIndex")
    void setRootIndex(int index);
}