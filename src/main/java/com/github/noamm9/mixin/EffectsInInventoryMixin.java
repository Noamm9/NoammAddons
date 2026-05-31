package com.github.noamm9.mixin;

import com.github.noamm9.utils.location.LocationUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(EffectsInInventory.class)
public class EffectsInInventoryMixin {
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void onInventoryEffects(GuiGraphicsExtractor graphics, Collection<MobEffectInstance> activeEffects, int x0, int yStep, int mouseX, int mouseY, int maxWidth, CallbackInfo ci) {
        if (LocationUtils.inSkyblock) ci.cancel();
    }
}