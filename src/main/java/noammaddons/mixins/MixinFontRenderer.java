package noammaddons.mixins;

import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static noammaddons.features.impl.general.VisualWords.replaceText;

@Mixin(value = FontRenderer.class)
public class MixinFontRenderer {
    @ModifyVariable(method = "renderStringAtPos", at = @At("HEAD"), argsOnly = true)
    private String modifyRenderStringAtPos(String text) {
        return replaceText(text);
    }

    @ModifyVariable(method = "getStringWidth", at = @At(value = "HEAD"), argsOnly = true)
    private String modifyGetStringWidth(String text) {
        return replaceText(text);
    }
}