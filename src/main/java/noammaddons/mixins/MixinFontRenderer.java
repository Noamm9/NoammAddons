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

   /* @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void drawStringHook(String text, float x, float y, int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        if (!noammaddons.getInitialized()) return;
        TextRenderer fr = noammaddons.getTextRenderer();
        int i = fr.getFr().drawString(text, x - 1, y - 3, color, dropShadow);
        cir.setReturnValue(i);
    }

    @Inject(method = "getStringWidth", at = @At(value = "HEAD"), cancellable = true)
    private void getStringWidthHook(String text, CallbackInfoReturnable<Integer> cir) {
        if (!noammaddons.getInitialized()) return;
        cir.setReturnValue((int) noammaddons.getTextRenderer().getStringWidth(text, 1));
    }*/
}