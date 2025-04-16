package noammaddons.mixins;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import noammaddons.events.AddMessageToChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.RegisterEvents.postAndCatch;

@Mixin(value = {GuiNewChat.class}, priority = 1001)
public abstract class MixinGuiNewChat {
    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), cancellable = true)
    private void onPrintChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId, CallbackInfo ci) {
        if (postAndCatch(new AddMessageToChatEvent(chatComponent, chatLineId))) {
            ci.cancel();
        }
    }
}
