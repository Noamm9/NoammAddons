package NoammAddons.mixins;

import NoammAddons.events.MessageSentEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(EntityPlayerSP.class)
public class MixinThePlayer {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void sendChatMessage(String message, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new MessageSentEvent(message))) {
            ci.cancel();
        }
    }
}
