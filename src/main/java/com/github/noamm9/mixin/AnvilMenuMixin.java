package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.dev.ModHider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @WrapOperation(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    public String getString(Component instance, Operation<String> original) {
        return ModHider.getString(instance);
    }
}
