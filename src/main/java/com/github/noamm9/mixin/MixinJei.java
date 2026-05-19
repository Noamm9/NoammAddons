package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.floor7.terminals.TerminalListener;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.jei.fabric.startup.EventRegistration", remap = false)
public abstract class MixinJei {
    @Dynamic
    @Inject(method = "registerScreenEvents", at = @At("HEAD"), cancellable = true)
    public void cancelEventsInTerm(CallbackInfo ci) {
        if (TerminalListener.inTerm) ci.cancel();
    }
}