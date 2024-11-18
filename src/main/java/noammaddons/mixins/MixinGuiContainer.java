package noammaddons.mixins;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import noammaddons.events.GuiContainerEvent;
import noammaddons.features.gui.ScalableTooltips;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.RegisterEvents.postAndCatch;


@Mixin(GuiContainer.class)
public class MixinGuiContainer {
    private final GuiContainer gui = (GuiContainer) (Object) this;
    @Shadow
    public Container inventorySlots;

    @Inject(method = "drawSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlot(Slot slot, CallbackInfo ci) {
        if (postAndCatch(new GuiContainerEvent.DrawSlotEvent(inventorySlots, gui, slot)))
            ci.cancel();
    }

    @Inject(method = "handleMouseClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;windowClick(IIIILnet/minecraft/entity/player/EntityPlayer;)Lnet/minecraft/item/ItemStack;"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        if (postAndCatch(new GuiContainerEvent.SlotClickEvent(inventorySlots, gui, slot, slotId)))
            ci.cancel();
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void onGuiClosed(CallbackInfo ci) {
        ScalableTooltips.INSTANCE.resetPos();
        postAndCatch(new GuiContainerEvent.CloseEvent(inventorySlots, gui));
    }
}

