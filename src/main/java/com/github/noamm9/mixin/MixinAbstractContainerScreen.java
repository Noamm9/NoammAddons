package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.ContainerEvent;
import com.github.noamm9.features.impl.misc.ScrollableTooltip;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen extends Screen {
    @Shadow @Nullable protected Slot hoveredSlot;

    protected MixinAbstractContainerScreen(Component component) {
        super(component);
    }

    @Shadow
    protected abstract List<Component> getTooltipFromContainerItem(ItemStack itemStack);

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    protected void onInit(CallbackInfo ci) {
        if (EventBus.post(new ContainerEvent.Open(this))) {
            ci.cancel();
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    protected void onClose(CallbackInfo ci) {
        if (EventBus.post(new ContainerEvent.Close(this))) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void onDrawSlotPre(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (EventBus.post(new ContainerEvent.Render.Slot.Pre(this, guiGraphics, slot))) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void onDrawSlotPost(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        EventBus.post(new ContainerEvent.Render.Slot.Post(this, guiGraphics, slot));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (EventBus.post(new ContainerEvent.MouseClick(this, click.x(), click.y(), click.button(), click.modifiers()))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (EventBus.post(new ContainerEvent.Keyboard(this, input.key(), (char) input.input(), input.scancode(), input.modifiers()))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("TAIL"))
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        EventBus.post(new ContainerEvent.MouseScroll(this, mouseX, mouseY, horizontalAmount, verticalAmount));
    }

    @WrapOperation(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;IILnet/minecraft/resources/ResourceLocation;)V"))
    private void onRenderTooltipMerged(GuiGraphics instance, Font font, List<Component> lines, Optional<TooltipComponent> tooltipImage, int x, int y, @Nullable ResourceLocation background, Operation<Void> original, @Local ItemStack stack) {
        if (stack == null || stack.isEmpty()) original.call(instance, font, lines, tooltipImage, x, y, background);
        else {
            ScrollableTooltip.setSlot(this.hoveredSlot.index);

            var event = new ContainerEvent.Render.Tooltip(this, instance, stack, x, y, new ArrayList<>(lines));
            if (EventBus.post(event)) return;

            original.call(instance, font, event.getLore(), tooltipImage, x, y, background);
        }
    }
}