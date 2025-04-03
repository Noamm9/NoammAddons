package noammaddons.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import noammaddons.events.GuiKeybourdInputEvent;
import noammaddons.events.GuiMouseClickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

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
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int mouseButton = Mouse.getEventButton();
        GuiScreen gui = mc.currentScreen;

        if (postAndCatch(new GuiMouseClickEvent(mouseX, mouseY, mouseButton, gui))) {
            ci.cancel();
        }
    }

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void injectKeyboardInput(CallbackInfo ci) {
        if (!Keyboard.getEventKeyState()) return;
        char keyChar = Keyboard.getEventCharacter();
        int keyCode = Keyboard.getEventKey();

        @Nullable
        GuiScreen gui = mc.currentScreen;
        if (gui == null) return;

        if (postAndCatch(new GuiKeybourdInputEvent(keyChar, keyCode, gui))) {
            ci.cancel();
        }
    }

}
