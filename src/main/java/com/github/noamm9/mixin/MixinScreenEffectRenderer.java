package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
//? if <26.2 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
*///? }
import net.minecraft.client.renderer.ScreenEffectRenderer;
//? if <26.2 {
/*import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
*///? }
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <26.2 {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///? }

@Mixin(ScreenEffectRenderer.class)
public abstract class MixinScreenEffectRenderer {
    //? if <26.2 {
    /*@Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFire(PoseStack poseStack, MultiBufferSource multiBufferSource, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
    *///? } else {
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFire(CallbackInfo ci) {
    //? }
        if (Camera.INSTANCE.enabled && Camera.getHideFireOverlay().getValue()) {
            ci.cancel();
        }
    }

    //? if <26.2 {
    /*@Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void onRenderWater(Minecraft minecraft, PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
    *///? } else {
    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void onRenderWater(CallbackInfo ci) {
    //? }
        if (Camera.INSTANCE.enabled && Camera.getHideWaterOverlay().getValue()) {
            ci.cancel();
        }
    }

    //? if <26.2 {
    /*@Inject(method = "getViewBlockingState", at = @At("HEAD"), cancellable = true)
    private static void onRenderWafter(Player player, CallbackInfoReturnable<BlockState> cir) {
        if (Camera.INSTANCE.enabled && Camera.getHideBlockOverlay().getValue()) {
            cir.setReturnValue(null);
        }
    *///? } else {
    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    private static void onRenderBlock(CallbackInfo ci) {
        if (Camera.INSTANCE.enabled && Camera.getHideBlockOverlay().getValue()) ci.cancel();
    //? }
    }
}