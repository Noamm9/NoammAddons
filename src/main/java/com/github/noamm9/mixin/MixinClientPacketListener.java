package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent;
import com.github.noamm9.event.impl.MessageSentEvent;
import com.github.noamm9.features.impl.misc.TimeChanger;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @WrapOperation(
        method = "handleBundlePacket",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"
        )
    )
    private void wrapPacketHandle(Packet packet, PacketListener listener, Operation<Void> original) {
        if (EventBus.post(new MainThreadPacketReceivedEvent.Pre(packet))) return;
        original.call(packet, listener);
        EventBus.post(new MainThreadPacketReceivedEvent.Post(packet));
    }

    @Inject(
        method = "handleSetTime",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void onSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        if (TimeChanger.INSTANCE.enabled) {
            TimeChanger.setTime();
            ci.cancel();
        }
    }

    @WrapMethod(method = "sendChat")
    private void onSendChat(String content, Operation<Void> original) {
        var event = new MessageSentEvent(content);
        if (EventBus.post(event)) return;
        original.call(event.getMessage());
    }

    @WrapMethod(method = "sendCommand")
    private void onSendCommand(String command, Operation<Void> original) {
        var event = new MessageSentEvent(command);
        if (EventBus.post(event)) return;
        original.call(event.getMessage());
    }
}