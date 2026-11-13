package com.github.noamm9.mixin;

import com.github.noamm9.interfaces.ITranslucentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PoseStack.Pose.class)
public class MixinPoseStackPose implements ITranslucentEntity {
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
