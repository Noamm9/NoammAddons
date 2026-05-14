package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlay;
import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlayScreen;
import com.github.noamm9.interfaces.ICoordRememberingSlot;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
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

/**
 * Adapted from Firmament's PatchHandledScreen.java
 * Source: https://github.com/nea89o/Firmament/blob/master/src/main/java/moe/nea/firmament/mixins/customgui/PatchHandledScreen.java
 */
@Mixin(value = AbstractContainerScreen.class, priority = 2000)
public class PatchAbstractContainerScreen<T extends AbstractContainerMenu> extends Screen {
    @Shadow @Final protected T menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Unique private boolean hasRememberedSlots = false;

    protected PatchAbstractContainerScreen(Component title) {
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

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void onDrawForeground(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        if (storageOverlay() != null) ci.cancel();
    }

    @Inject(method = "renderCarriedItem", at = @At("HEAD"), cancellable = true)
    private void onRenderCarriedItem(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null && overlay.renderCarriedItem(guiGraphics, i, j)) ci.cancel();
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void onBeforeSlotRender(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (storageOverlay() != null && !(slot.container instanceof net.minecraft.world.entity.player.Inventory)) ci.cancel();
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    public void onIsClickOutsideBounds(double d, double e, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        if (storageOverlay() != null) cir.setReturnValue(false);
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    public void onIsPointOverSlot(Slot slot, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null) cir.setReturnValue(overlay.isPointOverSlot(slot, this.leftPos, this.topPos, d, e));
    }

    @Inject(method = "renderBackground", at = @At("HEAD"))
    public void moveSlotsBeforeRender(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
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

    @WrapWithCondition(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
    public boolean preventDrawingBackground(AbstractContainerScreen instance, GuiGraphics context, float delta, int mouseX, int mouseY) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null) {
            overlay.renderContainerOverlay(context, mouseX, mouseY);
            return false;
        }
        return true;
    }

    @Inject(at = @At("HEAD"), method = "onClose")
    private void onVoluntaryExit(CallbackInfo ci) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null) overlay.onContainerClose();
    }

    @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    public boolean overrideMouseClicks(AbstractContainerScreen instance, MouseButtonEvent click, boolean doubled, Operation<Boolean> original) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null && overlay.mouseClickFromContainer(click)) return true;
        return original.call(instance, click, doubled);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void overrideMouseDrags(MouseButtonEvent mouseButtonEvent, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null && overlay.mouseDragged(mouseButtonEvent.y())) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    public void overrideMouseReleases(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null && overlay.mouseReleased()) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    public void overrideMouseScroll(double d, double e, double f, double g, CallbackInfoReturnable<Boolean> cir) {
        StorageOverlayScreen overlay = storageOverlay();
        if (overlay != null && overlay.mouseScrolledFromContainer(d, e, g)) cir.setReturnValue(true);
    }
}