package noammaddons.mixins;

import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.util.IThreadListener;
import noammaddons.events.EventDispatcher;
import noammaddons.events.MainThreadPacketRecivedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PacketThreadUtil.class)
public class PacketThreadUtilMixin {
    @Redirect(
            method = "checkThreadAndEnqueue",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/IThreadListener;addScheduledTask(Ljava/lang/Runnable;)Lcom/google/common/util/concurrent/ListenableFuture;"
            )
    )
    private static com.google.common.util.concurrent.ListenableFuture<?> onAddScheduledTask(
            IThreadListener listener, Runnable originalRunnable, Packet<?> packet, INetHandler handler
    ) {
        Runnable wrapper = () -> {
            originalRunnable.run();
            EventDispatcher.postAndCatch(new MainThreadPacketRecivedEvent(packet));
        };

        return listener.addScheduledTask(wrapper);
    }
}