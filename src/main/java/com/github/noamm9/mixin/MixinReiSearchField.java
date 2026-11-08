package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.InventorySearch;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides REI's main search field while Inventory Search "Replace REI Search" drives the item list.
 * Targets are matched by name only (and require = 0) so a signature change in REI degrades
 * to "field stays visible" instead of crashing.
 */
@Pseudo
@Mixin(targets = "me.shedaniel.rei.impl.client.gui.widget.search.OverlaySearchField", remap = false)
public abstract class MixinReiSearchField {
    @Dynamic
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideRender(CallbackInfo ci) {
        if (InventorySearch.shouldHideReiSearchField(this)) ci.cancel();
    }

    // Draws REI's hint popups above the field.
    @Dynamic
    @Inject(method = "laterRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideLaterRender(CallbackInfo ci) {
        if (InventorySearch.shouldHideReiSearchField(this)) ci.cancel();
    }

    // Also stops REI's hover tooltips and its double-click "highlight inventory" toggle.
    @Dynamic
    @Inject(method = "containsMouse(DD)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void noHover(CallbackInfoReturnable<Boolean> cir) {
        if (InventorySearch.shouldHideReiSearchField(this)) cir.setReturnValue(false);
    }

    @Dynamic
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void noClick(CallbackInfoReturnable<Boolean> cir) {
        if (InventorySearch.shouldHideReiSearchField(this)) cir.setReturnValue(false);
    }

    @Dynamic
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void noKeyPress(CallbackInfoReturnable<Boolean> cir) {
        if (InventorySearch.shouldHideReiSearchField(this)) cir.setReturnValue(false);
    }

    @Dynamic
    @Inject(method = "keyReleased", at = @At("HEAD"), cancellable = true, require = 0)
    private void noKeyRelease(CallbackInfoReturnable<Boolean> cir) {
        if (InventorySearch.shouldHideReiSearchField(this)) cir.setReturnValue(false);
    }

    @Dynamic
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true, require = 0)
    private void noCharTyped(CallbackInfoReturnable<Boolean> cir) {
        if (InventorySearch.shouldHideReiSearchField(this)) cir.setReturnValue(false);
    }
}
