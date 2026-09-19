package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.visual.Animations;
import com.github.noamm9.utils.items.ItemUtils;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MixinItemInHandRenderer {
    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void onBeforeRenderItem(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float f, float g, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (!Animations.INSTANCE.enabled) return;
        if (itemStack.isEmpty()) return;

        float sign = hand == InteractionHand.MAIN_HAND ? 1.0f : -1.0f;

        poseStack.translate(
            Animations.getMainHandX().getValue().floatValue() * sign,
            Animations.getMainHandY().getValue().floatValue(),
            Animations.getMainHandZ().getValue().floatValue()
        );
    }

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private float modifySwingProgress(float attack, @Local(argsOnly = true) FirstPersonHandsAndItemsRenderState state) {
        if (!Animations.INSTANCE.enabled) return attack;
        if (Animations.getDisableSwingAnimation().getValue()) {
            boolean isTerminator = ItemUtils.INSTANCE.getSkyblockId(state.mainHandItem).equals("TERMINATOR");
            if (Animations.getTerminatorOnly().getValue()) {
                if (isTerminator) return 1f;
            } else return 1f;
        }

        return attack;
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void onRenderItem(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (!Animations.INSTANCE.enabled) return;

        var s = (1.0f + Animations.getMainHandItemScale().getValue().floatValue());
        poseStack.rotate(Axis.XP.rotationDegrees(Animations.getMainHandPositiveX().getValue().floatValue()));
        poseStack.rotate(Axis.YP.rotationDegrees(Animations.getMainHandPositiveY().getValue().floatValue()));
        poseStack.rotate(Axis.ZP.rotationDegrees(Animations.getMainHandPositiveZ().getValue().floatValue()));
        poseStack.scale(s, s, s);
    }

    @WrapOperation(method = "swingArm", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void onSwingArmTranslate(PoseStack instance, float xo, float yo, float zo, Operation<Void> original) {
        if (!Animations.INSTANCE.enabled) {
            original.call(instance, xo, yo, zo);
            return;
        }

        float xMult = Animations.getSwingX().getValue().floatValue();
        float yMult = Animations.getSwingY().getValue().floatValue();
        float zMult = Animations.getSwingZ().getValue().floatValue();
        instance.translate(xo * xMult, yo * yMult, zo * zMult);
    }

    @WrapWithCondition(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V"))
    private boolean disableHandMove(PoseStack instance, Axis axis, float angle) {
        return !(Animations.INSTANCE.enabled && Animations.getDisableHandMove().getValue());
    }
}