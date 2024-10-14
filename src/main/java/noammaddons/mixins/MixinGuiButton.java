package noammaddons.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import noammaddons.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(GuiButton.class)
public abstract class MixinGuiButton {

    @Shadow public int id;
    @Shadow public int xPosition;
    @Shadow public int yPosition;
    @Shadow public String displayString;
    @Shadow public int width;
    @Shadow public int height;
    @Shadow public boolean visible;
    @Shadow protected boolean hovered;

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    public void drawCleanButton(Minecraft mc, int mouseX, int mouseY, CallbackInfo callbackInfo) {
        if(visible) {
            hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition+width && mouseY < yPosition+height;
            Color color = hovered ? new Color(255, 255, 255, 120) : new Color(216, 222, 233, 100);

            RenderUtils.INSTANCE.drawRoundedRect(
                    color,
                    xPosition,
                    yPosition,
                    width,
                    height,
                    5f
            );

            mc.fontRendererObj.drawStringWithShadow(
                    displayString,
                    (xPosition+ (float) width /2) - ((float) mc.fontRendererObj.getStringWidth(displayString) /2),
                    yPosition + 6,
                    Color.WHITE.getRGB()
            );
        }
        callbackInfo.cancel();
    }
}