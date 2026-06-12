package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.Animations;
import com.github.noamm9.features.impl.visual.RevertAxes;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {
    @Shadow private float oMainHandHeight;
    @Shadow private float mainHandHeight;

    @Shadow private float oOffHandHeight;
    @Shadow private float offHandHeight;

    @Shadow protected abstract void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm humanoidArm, float f);

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER))
    private void moveHeldItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
        if (!Animations.INSTANCE.enabled) return;
        if (itemStack.isEmpty()) return;
        if (itemStack.has(DataComponents.MAP_ID)) return;

        float sign = interactionHand == InteractionHand.MAIN_HAND ? 1.0f : -1.0f;

        poseStack.translate(
            Animations.INSTANCE.getMainHandX().getValue().floatValue() * sign,
            Animations.INSTANCE.getMainHandY().getValue().floatValue(),
            Animations.INSTANCE.getMainHandZ().getValue().floatValue()
        );

        poseStack.mulPose(Axis.XP.rotationDegrees(Animations.INSTANCE.getMainHandPositiveX().getValue().floatValue()));
        poseStack.mulPose(Axis.YP.rotationDegrees(Animations.INSTANCE.getMainHandPositiveY().getValue().floatValue()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Animations.INSTANCE.getMainHandPositiveZ().getValue().floatValue()));
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void scaleHeldItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
        if (!Animations.INSTANCE.enabled) return;
        if (itemStack.isEmpty()) return;
        if (itemStack.has(DataComponents.MAP_ID)) return;

        float s = Animations.INSTANCE.getMainHandItemScale().getValue().floatValue();
        poseStack.scale(s, s, s);
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void stopSwing(float f, PoseStack poseStack, int i, HumanoidArm humanoidArm, CallbackInfo ci) {
        if (!Animations.INSTANCE.enabled) return;
        if (!Animations.INSTANCE.getDisableSwingAnimation().getValue()) return;

        ci.cancel();
        this.applyItemArmAttackTransform(poseStack, humanoidArm, f);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float keepEquipScale(float original) {
        if (Animations.INSTANCE.enabled && (Animations.INSTANCE.getDisableEquip().getValue() || Animations.INSTANCE.getDisableSwingAnimation().getValue())) return 1f;
        return original;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void keepEquipHeights(CallbackInfo ci) {
        if (Animations.INSTANCE.enabled && Animations.INSTANCE.getDisableEquip().getValue()) {
            oMainHandHeight = 1f;
            mainHandHeight = 1f;
            oOffHandHeight = 1f;
            offHandHeight = 1f;
        }
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void instantItemSwap(ItemStack from, ItemStack to, CallbackInfoReturnable<Boolean> cir) {
        if (Animations.INSTANCE.enabled && Animations.INSTANCE.getDisableEquip().getValue()) {
            cir.setReturnValue(true);
        }
    }

    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true)
    private ItemStack revertAxe(ItemStack original) {
        return RevertAxes.shouldReplace(original);
    }
}
