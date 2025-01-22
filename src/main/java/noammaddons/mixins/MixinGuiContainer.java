package noammaddons.mixins;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import noammaddons.config.Config;
import noammaddons.events.DrawSlotEvent;
import noammaddons.events.SlotClickEvent;
import noammaddons.features.gui.ScalableTooltips;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraftforge.fml.client.config.GuiUtils.drawGradientRect;
import static noammaddons.events.RegisterEvents.postAndCatch;


@Mixin(GuiContainer.class)
public class MixinGuiContainer {
    @Unique
    private final GuiContainer noammAddons$gui = (GuiContainer) (Object) this;
    @Shadow
    public Container inventorySlots;


    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(Slot slot, CallbackInfo ci) {
        if (postAndCatch(new DrawSlotEvent(inventorySlots, noammAddons$gui, slot)))
            ci.cancel();
    }

    @Inject(method = "handleMouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;windowClick(IIIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        if (postAndCatch(new SlotClickEvent(inventorySlots, noammAddons$gui, slot, slotId)))
            ci.cancel();
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void onGuiClosed(CallbackInfo ci) {
        ScalableTooltips.resetPos();
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGradientRect(IIIIII)V"))
    private void redirectDrawGradientRect(GuiContainer instance, int left, int top, int right, int bottom, int startColor, int endColor, int mouseX, int mouseY, float partialTicks) {
        if (Config.INSTANCE.getCustomSlotHighlight()) Gui.drawRect(left, top, right, bottom, Config.INSTANCE.getCustomSlotHighlightColor().getRGB());
        else drawGradientRect(300, left, top, right, bottom, startColor, endColor);
    }
}

