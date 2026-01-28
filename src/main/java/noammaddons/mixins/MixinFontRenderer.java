package noammaddons.mixins;

import net.minecraft.client.gui.FontRenderer;
import noammaddons.features.impl.general.VisualWords;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = FontRenderer.class)
public class MixinFontRenderer {
    @ModifyVariable(method = "renderStringAtPos", at = @At("HEAD"), argsOnly = true)
    private String modifyRenderStringAtPos(String text) {
        if (text == null) return null;
        return VisualWords.replaceText(text);
    }

    @ModifyVariable(method = "getStringWidth", at = @At(value = "HEAD"), argsOnly = true)
    private String modifyGetStringWidth(String text) {
        if (text == null) return null;
        if (VisualWords.isSplittingChat) return text;
        return VisualWords.replaceText(text);
    }
}