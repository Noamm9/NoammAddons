package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlay;
import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlayScreen;
import com.github.noamm9.interfaces.ICoordRememberingSlot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractContainerScreen.class, priority = 2000)
public class MixinAbstractContainerScreenHook<T extends AbstractContainerMenu> extends Screen {
    @Shadow @Final protected T menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Unique private boolean hasRememberedSlots = false;

    protected MixinAbstractContainerScreenHook(Component title) {
        super(title);
    }

    @Unique
    @SuppressWarnings("ConstantValue")
    private StorageOverlayScreen storageOverlay() {
        if (!((Object) this instanceof ContainerScreen)) return null;
        return StorageOverlay.activeFor((ContainerScreen) (Object) this);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null) overlay.updateBounds();
    }

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void onRenderLabels(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci) {
        if (storageOverlay() != null) ci.cancel();
    }

    @Inject(method = "extractCarriedItem", at = @At("HEAD"), cancellable = true)
    private void onRenderCarriedItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null && overlay.renderCarriedItem(graphics, mouseX, mouseY)) ci.cancel();
    }

    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void onRenderSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (storageOverlay() != null && !(slot.container instanceof net.minecraft.world.entity.player.Inventory)) ci.cancel();
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    public void onHasClickedOutside(double mx, double my, int xo, int yo, CallbackInfoReturnable<Boolean> cir) {
        if (storageOverlay() != null) cir.setReturnValue(false);
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    public void onIsHovering(Slot slot, double xm, double ym, CallbackInfoReturnable<Boolean> cir) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null) cir.setReturnValue(overlay.isPointOverSlot(slot, this.leftPos, this.topPos, xm, ym));
    }

    @Inject(method = "extractSlotHighlightBack", at = @At("HEAD"))
    public void onRenderBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null) {
            for (Slot slot : menu.slots) {
                if (!hasRememberedSlots) ((ICoordRememberingSlot) slot).noammaddons_rememberCoords();
                ((ICoordRememberingSlot) slot).noammaddons_setX(-100000);
                ((ICoordRememberingSlot) slot).noammaddons_setY(-100000);
            }
            hasRememberedSlots = true;
        } else if (hasRememberedSlots) {
            for (Slot slot : menu.slots) ((ICoordRememberingSlot) slot).noammaddons_restoreCoords();
            hasRememberedSlots = false;
        }
    }

    @Inject(at = @At("HEAD"), method = "onClose")
    private void onContainerClose(CallbackInfo ci) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null) overlay.onContainerClose();
    }

    @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    public boolean onMouseClick(AbstractContainerScreen instance, MouseButtonEvent click, boolean doubled, Operation<Boolean> original) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null && overlay.mouseClicked(click)) return true;
        return original.call(instance, click, doubled);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void onMouseDrag(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null && overlay.mouseDragged(event.y())) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    public void onMouseRelease(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null && overlay.mouseReleased()) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    public void onMouseScroll(double x, double y, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null && overlay.mouseScrolled(scrollY)) cir.setReturnValue(true);
    }
}