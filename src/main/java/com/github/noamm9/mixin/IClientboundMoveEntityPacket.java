package com.github.noamm9.mixin;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundMoveEntityPacket.class)
public interface IClientboundMoveEntityPacket {
    @Accessor("entityId")
    int getEntityId();
}
