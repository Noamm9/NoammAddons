package noammaddons.mixins;

import net.minecraft.client.entity.EntityPlayerSP;
import noammaddons.events.MessageSentEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.EventDispatcher.postAndCatch;


@Mixin(EntityPlayerSP.class)
public class MixinThePlayer {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void sendChatMessage(String message, CallbackInfo ci) {
        if (postAndCatch(new MessageSentEvent(message))) {
            ci.cancel();
        }
    }
}
