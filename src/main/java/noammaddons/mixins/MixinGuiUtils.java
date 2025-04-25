package noammaddons.mixins;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.client.config.GuiUtils;
import noammaddons.config.EditGui.HudEditorScreen;
import noammaddons.features.impl.gui.ScalableTooltips;
import noammaddons.noammaddons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = {GuiUtils.class}, remap = false, priority = 1001)
public class MixinGuiUtils {
    @Inject(method = "drawHoveringText", at = @At("HEAD"), cancellable = true)
    private static void drawScaledHoveringText(List<String> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight, int maxTextWidth, FontRenderer font, CallbackInfo ci) {
        if (noammaddons.getMc().currentScreen instanceof HudEditorScreen) return;
        if (ScalableTooltips.drawScaledHoveringText(textLines, mouseX, mouseY, screenWidth, screenHeight, font)) {
            ci.cancel();
        }
    }
}