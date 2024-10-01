package noammaddons.mixins;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import noammaddons.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.util.StringUtils.stripControlCodes;
import static noammaddons.features.General.RemoveUselessMessages.*;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), cancellable = true)
    private void onPrintChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId, CallbackInfo ci) {
        if (Config.INSTANCE.getRemoveUselessMessages()) {
            String message = chatComponent.getFormattedText();

            INSTANCE.getRegexsWithFormatting().forEach(regex -> {
                if (message.matches(regex.toString())) {
                    ci.cancel();
                }
            });

            INSTANCE.getRegexsWithoutFormatting().forEach(regex -> {
                if (stripControlCodes(message).matches(regex.toString())) {
                    ci.cancel();
                }
            });
        }
    }
}
