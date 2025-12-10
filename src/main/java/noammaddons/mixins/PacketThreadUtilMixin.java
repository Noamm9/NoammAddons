package noammaddons.mixins;

import com.google.common.util.concurrent.ListenableFuture;
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
    @Redirect(method = "checkThreadAndEnqueue", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/IThreadListener;addScheduledTask(Ljava/lang/Runnable;)Lcom/google/common/util/concurrent/ListenableFuture;"))
    private static ListenableFuture<?> onAddScheduledTask(IThreadListener listener, Runnable originalRunnable, Packet<?> packet, INetHandler handler) {
        Runnable wrapper = () -> {
            if (!EventDispatcher.postAndCatch(new MainThreadPacketRecivedEvent.Pre(packet))) {
                try {
                    originalRunnable.run();
                } catch (Exception ignored) {

                }
                EventDispatcher.postAndCatch(new MainThreadPacketRecivedEvent.Post(packet));
            }
        };

        return listener.addScheduledTask(wrapper);
    }
}