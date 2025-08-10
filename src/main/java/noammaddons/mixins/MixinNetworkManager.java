package noammaddons.mixins;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import noammaddons.NoammAddons;
import noammaddons.events.PacketEvent;
import noammaddons.events.PostPacketEvent;
import noammaddons.utils.Utils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static noammaddons.events.EventDispatcher.postAndCatch;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager extends SimpleChannelInboundHandler<Packet> implements Utils.INetworkManager {
    @Shadow
    @Final
    private ReentrantReadWriteLock readWriteLock;
    @Shadow
    @Final
    private Queue<?> outboundPacketsQueue;

    @Shadow
    public abstract boolean isChannelOpen();

    @Shadow
    protected abstract void flushOutboundQueue();

    @Shadow
    protected abstract void dispatchPacket(Packet<?> inPacket, GenericFutureListener<? extends Future<? super Void>>[] futureListeners);

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

    @Unique
    @Override
    @SuppressWarnings({"unchecked", "AddedMixinMembersNamePattern"})
    public void sendPacketNoEvent(Packet<?> packet) {
        if (this.isChannelOpen()) {
            this.flushOutboundQueue();
            this.dispatchPacket(packet, null);
        } else {
            this.readWriteLock.writeLock().lock();
            try {
                Object tuple = noammAddons$createInboundHandlerTuple(packet, null);
                if (tuple != null) ((Queue<Object>) outboundPacketsQueue).add(tuple);
            } finally {
                this.readWriteLock.writeLock().unlock();
            }
        }
    }

    @Unique
    private Object noammAddons$createInboundHandlerTuple(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>>[] listeners) {
        try {
            Class<?> tupleClass = Class.forName("net.minecraft.network.NetworkManager$InboundHandlerTuplePacketListener");
            Constructor<?> constructor = tupleClass.getDeclaredConstructor(Packet.class, GenericFutureListener[].class);
            constructor.setAccessible(true);
            return constructor.newInstance(packet, listeners);

        } catch (Exception e) {
            NoammAddons.Logger.error("Failed to create InboundHandlerTuplePacketListener via reflection!", e);
            return null;
        }
    }
}