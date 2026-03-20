package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;

/***
 * @author DocilElm
 * @from https://github.com/Synnerz/devonian/blob/1.21.10/src/main/java/com/github/synnerz/devonian/mixin/GlDeviceMixin.java
 * Slightly modified
 */
@Mixin(GlDevice.class)
public class MixinGlDevice {
    @Unique private static boolean lastFullBright = false;

    @WrapOperation(method = "compileShader", at = @At(value = "INVOKE", target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object noammaddons$fullbright(BiFunction<ResourceLocation, ShaderType, String> instance, Object id, Object type, Operation<String> original) {
        if (!Camera.INSTANCE.enabled || !Camera.getFullBright().getValue()) return original.call(instance, id, type);
        if (type != ShaderType.FRAGMENT || id != RenderPipelines.LIGHTMAP.getFragmentShader()) return original.call(instance, id, type);

        return """
            #version 150
            
            in vec2 texCoord;
            out vec4 fragColor;
            
            void main() {
                fragColor = vec4(1.0);
            }
            """;
    }

    @Inject(method = "getOrCompilePipeline", at = @At("HEAD"))
    private void noammaddons$checkFullBrightToggle(RenderPipeline pipeline, CallbackInfoReturnable<GlRenderPipeline> cir) {
        boolean current = Camera.INSTANCE.enabled && Camera.getFullBright().getValue();
        if (current == lastFullBright) return;
        lastFullBright = current;
        ((GlDevice) (Object) this).clearPipelineCache();
    }
}