package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.NameTagTweaks;
import com.github.noamm9.features.impl.visual.PlayerOpacity;
import com.github.noamm9.interfaces.ITranslucentEntity;
import com.github.noamm9.utils.render.DepthTestGlow;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow protected abstract @Nullable Component getNameTag(Entity entity);

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void tagDepthTestedGlow(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
        if (state.outlineColor == 0) return;
        if (! DepthTestGlow.shouldDepthTest(entity)) return;
        state.outlineColor = DepthTestGlow.tag(state.outlineColor);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void setTranslucentOpacity(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
        ((ITranslucentEntity) state).noammaddons$setOpacity(PlayerOpacity.getOpacity(entity));
    }

    @Inject(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V", at = @At("RETURN"))
    private void onExtractName(Entity entity, EntityRenderState state, float partialTicks, double nameTagDistance, double belowNameDistance, CallbackInfo ci) {
        if (! NameTagTweaks.shouldShowNametag(entity)) return;
        if (state.scoreText == null) state.scoreText = entity.belowNameDisplay();
        if (state.nameTag == null) state.nameTag = getNameTag(entity);
        if (state.nameTagAttachment == null) state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTicks));
    }

    //#if CHEAT
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void moveToZeroPingPosition(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
        com.github.noamm9.features.impl.misc.ZeroPingCamera.renderStateHook(entity, state);
    }
    //#endif
}