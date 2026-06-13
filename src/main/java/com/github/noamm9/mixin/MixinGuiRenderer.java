package com.github.noamm9.mixin;

import com.github.noamm9.utils.render.ItemRenderer;
import net.fabricmc.fabric.impl.client.rendering.GuiRendererExtensions;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

// hotfix for custom PIP breaking with Better Screens mod
@Mixin(GuiRenderer.class)
public class MixinGuiRenderer {
    @Shadow @Final private Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRenderer<?>> pictureInPictureRenderers;
    @Shadow @Final private SubmitNodeCollector submitNodeCollector;
    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;

    @Inject(method = "preparePictureInPicture", at = @At("HEAD"))
    private void noammaddons$ensureFabricInit(CallbackInfo ci) {
        if (pictureInPictureRenderers.containsKey(ItemRenderer.ItemState.class)) return;
        if (submitNodeCollector instanceof SubmitNodeStorage storage) {
            ((GuiRendererExtensions) (Object) this).fabric_onReady(storage);
        } else {
            pictureInPictureRenderers.put(ItemRenderer.ItemState.class, new ItemRenderer(bufferSource));
        }
    }
}