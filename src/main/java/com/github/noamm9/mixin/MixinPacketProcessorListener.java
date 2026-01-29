package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.MainThreadPacketReceivedEvent;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public class MixinPacketProcessorListener {
    @Shadow @Final private Packet<?> packet;

    @Inject(
        method = "handle",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"),
        cancellable = true
    )
    private void onPreHandle(CallbackInfo ci) {
        if (EventBus.post(new MainThreadPacketReceivedEvent.Pre(this.packet))) {
            ci.cancel();
        }
    }

    @Inject(
        method = "handle",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.AFTER)
    )
    private void onPostHandle(CallbackInfo ci) {
        EventBus.post(new MainThreadPacketReceivedEvent.Post(this.packet));
    }
}
