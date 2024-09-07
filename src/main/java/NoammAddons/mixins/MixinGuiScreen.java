package NoammAddons.mixins;

import NoammAddons.events.GuiContainerEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Shadow public int height;
    @Shadow public int width;
    @Shadow public Minecraft mc;

    @Inject(
            method = "handleMouseInput",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiScreen;mouseClicked(III)V"
    ), cancellable = true)
    private void injectMouseClick(CallbackInfo ci) {
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int mouseButton = Mouse.getEventButton();

        GuiContainerEvent.GuiMouseClickEvent event = new GuiContainerEvent.GuiMouseClickEvent(mouseX, mouseY, mouseButton);
        MinecraftForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            ci.cancel();
        }
    }
}
