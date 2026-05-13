package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.storageoverlay.ICoordRememberingSlot;
import com.github.noamm9.features.impl.general.storageoverlay.IStorageOverlayHolder;
import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlayCustom;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
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
public class PatchAbstractContainerScreen<T extends AbstractContainerMenu> extends Screen implements IStorageOverlayHolder {
    @Shadow @Final protected T menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageHeight;
    @Shadow protected int imageWidth;

    @Unique public StorageOverlayCustom override;
    @Unique public boolean hasRememberedSlots = false;
    @Unique private int originalBackgroundWidth;
    @Unique private int originalBackgroundHeight;

    protected PatchAbstractContainerScreen(Component title) {
        super(title);
    }

    @Nullable
    @Override
    public StorageOverlayCustom noammaddons_getStorageOverlay() {
        return override;
    }

    @Override
    public void noammaddons_setStorageOverlay(@Nullable StorageOverlayCustom gui) {
        if (this.override != null) {
            imageHeight = originalBackgroundHeight;
            imageWidth = originalBackgroundWidth;
        }
        if (gui != null) {
            originalBackgroundHeight = imageHeight;
            originalBackgroundWidth = imageWidth;
        }
        this.override = gui;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (override != null) override.onInit();
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void onDrawForeground(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        if (override != null) ci.cancel();
    }

    @Inject(method = "renderCarriedItem", at = @At("HEAD"), cancellable = true)
    private void onRenderCarriedItem(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        if (override != null && override.renderCarriedItem(guiGraphics, i, j)) ci.cancel();
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void onBeforeSlotRender(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (override == null) return;
        if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory)) ci.cancel();
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    public void onIsClickOutsideBounds(double d, double e, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) cir.setReturnValue(false);
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    public void onIsPointOverSlot(Slot slot, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) cir.setReturnValue(override.isPointOverSlot(slot, this.leftPos, this.topPos, d, e));
    }

    @Inject(method = "renderBackground", at = @At("HEAD"))
    public void moveSlotsBeforeRender(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (override != null) {
            for (Slot slot : menu.slots) {
                if (!hasRememberedSlots) ((ICoordRememberingSlot) slot).noammaddons_rememberCoords();
                override.moveSlot(slot);
            }
            hasRememberedSlots = true;
        } else if (hasRememberedSlots) {
            for (Slot slot : menu.slots) ((ICoordRememberingSlot) slot).noammaddons_restoreCoords();
            hasRememberedSlots = false;
        }

    }

    @WrapWithCondition(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
    public boolean preventDrawingBackground(AbstractContainerScreen instance, GuiGraphics context, float delta, int mouseX, int mouseY) {
        if (override != null) {
            override.render(context, delta, mouseX, mouseY);
            return false;
        }
        return true;
    }

    @Inject(at = @At("HEAD"), method = "onClose")
    private void onVoluntaryExit(CallbackInfo ci) {
        if (override != null) override.onVoluntaryExit();
    }

    @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    public boolean overrideMouseClicks(AbstractContainerScreen instance, MouseButtonEvent click, boolean doubled, Operation<Boolean> original) {
        if (override != null) if (override.mouseClick(click, doubled)) return true;
        return original.call(instance, click, doubled);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void overrideMouseDrags(MouseButtonEvent mouseButtonEvent, double d, double e, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) if (override.mouseDragged(mouseButtonEvent, d, e)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    public void overrideMouseReleases(MouseButtonEvent mouseButtonEvent, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) if (override.mouseReleased(mouseButtonEvent)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    public void overrideMouseScroll(double d, double e, double f, double g, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) if (override.mouseScrolled(d, e, f, g)) cir.setReturnValue(true);
    }
}
