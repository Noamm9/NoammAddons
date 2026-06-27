package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.Camera;
import com.github.noamm9.ui.notification.NotificationManager;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Mutable private float spinningEffectTime;
    @Shadow @Mutable private float spinningEffectSpeed;

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void onBobHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.options.damageTiltStrength().get() == 0) ci.cancel();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    public void onBobView(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (Camera.INSTANCE.enabled && Camera.getDisableNausea().getValue()) ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void onTick(CallbackInfo ci) {
        if (!Camera.INSTANCE.enabled || !Camera.getDisableNausea().getValue()) return;
        Player player = minecraft.player;
        if (player != null && player.hasEffect(MobEffects.NAUSEA)) {
            spinningEffectTime = 0;
            spinningEffectSpeed = 0;
        }
    }

    @ModifyVariable(method = "renderLevel", at = @At("STORE"), name = "nauseaIntensity")
    public float zeroNauseaIntensity(float original) {
        if (Camera.INSTANCE.enabled && Camera.getDisableNausea().getValue()) return 0.0F;
        return original;
    }

    @Inject(method = "extractGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractDeferredSubtitles()V"))
    public void onExtractGui(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci, @Local GuiGraphicsExtractor graphics) {
        NotificationManager.render(graphics);
    }
}
