package noammaddons.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import noammaddons.features.impl.DevOptions;
import noammaddons.utils.MouseUtils;
import noammaddons.utils.RenderHelper;
import noammaddons.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;


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
        if (!DevOptions.getClientBranding()) return;

        callbackInfo.cancel();
        if (!visible) return;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        hovered = MouseUtils.isMouseOver(mouseX, mouseY, xPosition, yPosition, width, height);
        Color buttonColor = hovered ? new Color(19, 19, 19) : new Color(33, 33, 33);

        RenderUtils.INSTANCE.drawFloatingRect(
                xPosition,
                yPosition,
                width,
                height,
                buttonColor
        );

        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(noammAddons$buttonTexture);

        mouseDragged(mc, mouseX, mouseY);
        Color textColor = new Color(224, 224, 224);
        if (!enabled) textColor = new Color(160, 160, 160);
        else if (hovered) textColor = new Color(255, 255, 128);

        mc.fontRendererObj.drawStringWithShadow(
                displayString,
                (xPosition + (float) width / 2) - (RenderHelper.getStringWidth(displayString, 1) / 2),
                yPosition + 6,
                textColor.getRGB()
        );

        GlStateManager.popMatrix();
    }
}