package com.github.noamm9.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.github.noamm9.ui.customgui.ICoordRememberingSlot;
import com.github.noamm9.ui.customgui.IHasCustomGui;
import com.github.noamm9.ui.customgui.CustomGui;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
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
public class PatchAbstractContainerScreen<T extends AbstractContainerMenu> extends Screen implements IHasCustomGui {
    @Shadow @Final protected T menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageHeight;
    @Shadow protected int imageWidth;

    @Unique public CustomGui override;
    @Unique public boolean hasRememberedSlots = false;
    @Unique private int originalBackgroundWidth;
    @Unique private int originalBackgroundHeight;

    protected PatchAbstractContainerScreen(Component title) {
        super(title);
    }

    @Nullable
    @Override
    public CustomGui noammaddons_getCustomGui() {
        return override;
    }

    @Override
    public void noammaddons_setCustomGui(@Nullable CustomGui gui) {
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
        if (override != null) {
            override.onInit();
        }
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void onDrawForeground(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
        if (override != null && !override.shouldDrawForeground())
            ci.cancel();
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void onBeforeSlotRender(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (override != null) {
            if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory)) {
                ci.cancel();
                return;
            }
            override.beforeSlotRender(guiGraphics, slot);
        }
    }

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void onAfterSlotRender(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (override != null) override.afterSlotRender(guiGraphics, slot);
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    public void onIsClickOutsideBounds(double mouseX, double mouseY, int left, int top, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) {
            cir.setReturnValue(override.isClickOutsideBounds(mouseX, mouseY));
        }
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At("HEAD"), cancellable = true)
    public void onIsPointOverSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) {
            cir.setReturnValue(override.isPointOverSlot(slot, this.leftPos, this.topPos, pointX, pointY));
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"))
    public void moveSlotsBeforeRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (override != null) {
            for (Slot slot : menu.slots) {
                if (!hasRememberedSlots) {
                    ((ICoordRememberingSlot) slot).noammaddons_rememberCoords();
                }
                override.moveSlot(slot);
            }
            hasRememberedSlots = true;
        } else {
            if (hasRememberedSlots) {
                for (Slot slot : menu.slots) {
                    ((ICoordRememberingSlot) slot).noammaddons_restoreCoords();
                }
                hasRememberedSlots = false;
            }
        }
    }

    @WrapWithCondition(
        method = "renderBackground",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
    public boolean preventDrawingBackground(AbstractContainerScreen instance, GuiGraphics context, float delta, int mouseX, int mouseY) {
        if (override != null) {
            override.render(context, delta, mouseX, mouseY);
            return false;
        }
        return true;
    }

    @Inject(at = @At("HEAD"), method = "onClose", cancellable = true)
    private void onVoluntaryExit(CallbackInfo ci) {
        if (override != null) {
            if (!override.onVoluntaryExit()) ci.cancel();
        }
    }

    @WrapOperation(
        method = "mouseClicked",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    public boolean overrideMouseClicks(AbstractContainerScreen instance, MouseButtonEvent click, boolean doubled, Operation<Boolean> original) {
        if (override != null) {
            if (override.mouseClick(click, doubled)) return true;
        }
        return original.call(instance, click, doubled);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void overrideMouseDrags(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) {
            if (override.mouseDragged(click, offsetX, offsetY)) cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void overrideKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) {
            if (override.keyPressed(input)) cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    public void overrideMouseReleases(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) {
            if (override.mouseReleased(click)) cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    public void overrideMouseScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (override != null) {
            if (override.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) cir.setReturnValue(true);
        }
    }
}
