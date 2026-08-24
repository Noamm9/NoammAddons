package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public class MixinPacketProcessorListener<T extends PacketListener> {
    @Shadow @Final private Packet<T> packet;

    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"))
    private void onHandle(Packet<T> instance, T listener, Operation<Void> original) {
        if (EventBus.post(new MainThreadPacketReceivedEvent.Pre(packet))) return;
        original.call(instance, listener);
        EventBus.post(new MainThreadPacketReceivedEvent.Post(packet));
    }
}