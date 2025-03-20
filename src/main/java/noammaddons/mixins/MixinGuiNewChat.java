package noammaddons.mixins;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import noammaddons.features.general.RemoveUselessMessages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.util.StringUtils.stripControlCodes;
import static noammaddons.features.general.RemoveUselessMessages.regexsToRemove;
import static noammaddons.noammaddons.config;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {
    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"), cancellable = true)
    private void onPrintChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId, CallbackInfo ci) {
        String message = chatComponent.getFormattedText();

        if (config.getRemoveUselessMessages()) {
            RemoveUselessMessages.RegexsWithFormatting.forEach(regex -> {
                if (message.matches(regex.toString())) {
                    ci.cancel();
                }
            });

            RemoveUselessMessages.RegexsWithoutFormatting.forEach(regex -> {
                if (stripControlCodes(message).matches(regex.toString())) {
                    ci.cancel();
                }
            });
        }

        regexsToRemove.forEach(regex -> {
            if (stripControlCodes(message).matches(regex.toString().replace("&", "§")) || message.matches(regex.toString().replace("&", "§"))) {
                ci.cancel();
            }
        });
    }
}
