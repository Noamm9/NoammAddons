package com.github.noamm9.mixin;

import com.github.noamm9.compat.RenderKt;
import com.github.noamm9.features.impl.visual.Animations;
import com.github.noamm9.utils.items.ItemUtils;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <26.3 {
/*import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
*///?} else {
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
//?}

//? if <26.3 {
/*import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
 *///?}

//~ if <26.3 'FirstPersonHandsAndItemsRenderer' -> 'ItemInHandRenderer'
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MixinItemInHandRenderer {
    //? if <26.3 {
    /*@Shadow private ItemStack mainHandItem;

    @Shadow private float oMainHandHeight;
    @Shadow private float mainHandHeight;

    @Shadow private float oOffHandHeight;
    @Shadow private float offHandHeight;
    *///?}

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    //~ if <26.3 'PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, ' -> 'AbstractClientPlayer player, '
    private void onBeforeRenderItem(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float f, float g, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (! Animations.INSTANCE.enabled) return;
        if (itemStack.isEmpty()) return;

        float sign = hand == InteractionHand.MAIN_HAND ? 1.0f : - 1.0f;

        poseStack.translate(
            Animations.getMainHandX().getValue().floatValue() * sign,
            Animations.getMainHandY().getValue().floatValue(),
            Animations.getMainHandZ().getValue().floatValue()
        );
    }

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    //~ if <26.3 '(float attack, @Local(argsOnly = true) FirstPersonHandsAndItemsRenderState state)' -> '(float attack)'
    private float modifySwingProgress(float attack, @Local(argsOnly = true) FirstPersonHandsAndItemsRenderState state) {
        if (! Animations.INSTANCE.enabled) return attack;
        if (Animations.getDisableSwingAnimation().getValue()) {
            //~ if <26.3 '(state.mainHandItem)' -> '(mainHandItem)'
            boolean isTerminator = ItemUtils.INSTANCE.getSkyblockId(state.mainHandItem).equals("TERMINATOR");
            if (Animations.getTerminatorOnly().getValue()) {
                if (isTerminator) return 1f;
            } else return 1f;
        }

        return attack;
    }

    //~ if <26.3 'item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V' -> 'ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V'
    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    //~ if <26.3 'PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, ' -> 'AbstractClientPlayer player, '
    private void onRenderItem(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (! Animations.INSTANCE.enabled) return;

        var s = (1.0f + Animations.getMainHandItemScale().getValue().floatValue());
        RenderKt.rotateCompat(poseStack, Axis.XP.rotationDegrees(Animations.getMainHandPositiveX().getValue().floatValue()));
        RenderKt.rotateCompat(poseStack, Axis.YP.rotationDegrees(Animations.getMainHandPositiveY().getValue().floatValue()));
        RenderKt.rotateCompat(poseStack, Axis.ZP.rotationDegrees(Animations.getMainHandPositiveZ().getValue().floatValue()));
        poseStack.scale(s, s, s);
    }

    @WrapOperation(method = "swingArm", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void onSwingArmTranslate(PoseStack instance, float xo, float yo, float zo, Operation<Void> original) {
        if (! Animations.INSTANCE.enabled) {
            original.call(instance, xo, yo, zo);
            return;
        }

        float xMult = Animations.getSwingX().getValue().floatValue();
        float yMult = Animations.getSwingY().getValue().floatValue();
        float zMult = Animations.getSwingZ().getValue().floatValue();
        instance.translate(xo * xMult, yo * yMult, zo * zMult);
    }

    //? if <26.3 {
    /*@Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void onShouldSkipAnimation(ItemStack currentlyVisibleItem, ItemStack expectedItem, CallbackInfoReturnable<Boolean> cir) {
        if (Animations.INSTANCE.enabled && Animations.getDisableEquip().getValue()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onUpdateHeldItems(CallbackInfo ci) {
        if (Animations.INSTANCE.enabled && Animations.getDisableEquip().getValue()) {
            oMainHandHeight = 1f;
            mainHandHeight = 1f;
            oOffHandHeight = 1f;
            offHandHeight = 1f;
        }
    }
    *///?}

    //~ if <26.3 'rotateDegrees(Lcom/mojang/math/Axis;F)V' -> 'mulPose(Lorg/joml/Quaternionfc;)V'
    @WrapWithCondition(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V"))
    //~ if <26.3 'Axis axis, float angle)' -> 'Quaternionfc by)'
    private boolean disableHandMove(PoseStack instance, Axis axis, float angle) {
        return ! (Animations.INSTANCE.enabled && Animations.getDisableHandMove().getValue());
    }
}