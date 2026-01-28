package noammaddons.mixins;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.IChatComponent;
import noammaddons.features.impl.general.VisualWords;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GuiUtilRenderComponents.class)
public class MixinGuiUtilRenderComponents {
    @Inject(method = "splitText", at = @At("HEAD"))
    private static void onSplitTextStart(IChatComponent p_178908_0_, int p_178908_1_, FontRenderer p_178908_2_, boolean p_178908_3_, boolean p_178908_4_, CallbackInfoReturnable<List<IChatComponent>> cir) {
        VisualWords.isSplittingChat = true;
    }

    @Inject(method = "splitText", at = @At("RETURN"))
    private static void onSplitTextEnd(IChatComponent component, int maxTextLenght, FontRenderer fontRendererIn, boolean p_178908_3_, boolean p_178908_4_, CallbackInfoReturnable<List<IChatComponent>> cir) {
        VisualWords.isSplittingChat = false;
    }
}
