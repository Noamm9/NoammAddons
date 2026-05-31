package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.general.SlotBinding;
import com.github.noamm9.features.impl.misc.HideRecipeBook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class MixinAbstractRecipeBookScreen<T extends RecipeBookMenu> extends AbstractContainerScreen<T> implements RecipeUpdateListener {
    public MixinAbstractRecipeBookScreen(T abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen;initButton()V"), cancellable = true)
    private void renderRecipeBook(CallbackInfo ci) {
        if (HideRecipeBook.INSTANCE.enabled) ci.cancel();
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;nextStratum()V", ordinal = 0, shift = At.Shift.AFTER))
    private void onRenderPre(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        SlotBinding.drawSlotBinding(graphics, mouseX, mouseY, this);
    }
}