package noammaddons.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import noammaddons.events.GuiContainerEvent;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.RegisterEvents.postAndCatch;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Shadow
    public int height;
    @Shadow
    public int width;
    @Shadow
    public Minecraft mc;

    @Inject(
            method = "handleMouseInput",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiScreen;mouseClicked(III)V"
            ), cancellable = true
    )
    private void injectMouseClick(CallbackInfo ci) {
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int mouseButton = Mouse.getEventButton();
        GuiScreen guiScreen = this.mc.currentScreen;

        if (postAndCatch(new GuiContainerEvent.GuiMouseClickEvent(mouseX, mouseY, mouseButton, guiScreen))) {
            ci.cancel();
        }
    }
}
