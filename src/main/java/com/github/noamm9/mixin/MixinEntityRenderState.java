package com.github.noamm9.mixin;

import com.github.noamm9.interfaces.ITranslucentEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class MixinEntityRenderState implements ITranslucentEntity {
    @Unique private float noammaddons$opacity = 1f;

    @Override
    public void noammaddons$setOpacity(float opacity) {
        noammaddons$opacity = opacity;
    }

    @Override
    public float noammaddons$getOpacity() {
        return noammaddons$opacity;
    }
}
