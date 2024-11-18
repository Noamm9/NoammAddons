package noammaddons.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import noammaddons.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

import static gg.essential.universal.UGraphics.getStringWidth;


@Mixin(GuiButton.class)
public abstract class MixinGuiButton {

    @Unique
    private static final ResourceLocation noammAddons$buttonTexture = new ResourceLocation("textures/gui/widgets.png");
    @Shadow
    public int id;
    @Shadow
    public int xPosition;
    @Shadow
    public int yPosition;
    @Shadow
    public String displayString;
    @Shadow
    public int width;
    @Shadow
    public int height;
    @Shadow
    public boolean visible;

    @Shadow
    public boolean enabled;
    @Shadow
    protected boolean hovered;

    @Shadow
    protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    public void drawCleanButton(Minecraft mc, int mouseX, int mouseY, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
        if (visible) {
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
            Color buttonColor = hovered ? new Color(255, 255, 255, 120) : new Color(216, 222, 233, 100);


            RenderUtils.INSTANCE.drawRoundedRect(
                    buttonColor,
                    xPosition + 2.5,
                    yPosition + .5,
                    width - 5,
                    height - 1,
                    5f
            );

            RenderUtils.INSTANCE.drawRoundedBorder(
                    Color.WHITE,
                    xPosition + 2.5, yPosition + .5,
                    width - 5, height - 1,
                    5f, 2f
            );

            GlStateManager.enableTexture2D();
            mc.getTextureManager().bindTexture(noammAddons$buttonTexture);

            mouseDragged(mc, mouseX, mouseY);
            Color textColor = new Color(224, 224, 224);
            if (!enabled) textColor = new Color(160, 160, 160);
            else if (hovered) textColor = new Color(255, 255, 128);

            mc.fontRendererObj.drawStringWithShadow(
                    displayString,
                    (xPosition + (float) width / 2) - ((float) getStringWidth(displayString) / 2),
                    yPosition + 6,
                    textColor.getRGB()
            );
        }
        GlStateManager.popMatrix();
    }
}