package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.TimeChanger;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AtmosphericFogEnvironment.class)
public class MixinAtmosphericFogEnvironment {
    @WrapMethod(method = "getBaseColor")
    private /*$ if <26.3 'int' else 'Vector3fc' >>*/ Vector3fc onGetBaseColor(ClientLevel level, Camera camera, int renderDistance, float partialTicks, Operation</*$ if <26.3 'Integer>' else 'Vector3fc>' >>*/ Vector3fc> original) {
        TimeChanger.extractingSky = TimeChanger.INSTANCE.enabled;
        /*$ if <26.3 'int' else 'Vector3fc' >>*/
        Vector3fc color = original.call(level, camera, renderDistance, partialTicks);
        TimeChanger.extractingSky = false;
        return color;
    }
}