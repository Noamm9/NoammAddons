package com.github.noamm9.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput {
    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input resolveSnappyTappyInput(Input original) {
        //#if CHEAT
        return com.github.noamm9.features.impl.misc.SnappyTappy.resolveInput(original);
        //#else
        //$return original;
        //#endif
    }
}