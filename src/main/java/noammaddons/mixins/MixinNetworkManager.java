package noammaddons.mixins;


import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import noammaddons.events.PacketEvent;
import noammaddons.events.PostPacketEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.EventDispatcher.postAndCatch;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {
    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (postAndCatch(new PacketEvent.Received(packet))) ci.cancel();
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSentPacket(Packet<?> packet, CallbackInfo ci) {
        if (postAndCatch(new PacketEvent.Sent(packet))) ci.cancel();
    }


    @Inject(method = "channelRead0*", at = @At("TAIL"))
    private void onPostReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        postAndCatch(new PostPacketEvent.Received(packet));
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("TAIL"))
    private void onPostSentPacket(Packet<?> packet, CallbackInfo ci) {
        postAndCatch(new PostPacketEvent.Sent(packet));
    }
}
