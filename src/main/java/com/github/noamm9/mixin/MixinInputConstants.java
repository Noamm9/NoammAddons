package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.NoCursorReset;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InputConstants.class)
public abstract class MixinInputConstants {
    @WrapOperation(method = "grabOrReleaseMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V"))
    private static void a(long window, double xpos, double ypos, Operation<Void> original) {
        if (NoCursorReset.INSTANCE.enabled && NoCursorReset.inContainer) return;
        original.call(window, xpos, ypos);
    }
}
