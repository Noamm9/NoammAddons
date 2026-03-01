package com.github.noamm9.mixin;


import com.github.noamm9.features.impl.visual.Animations;
import com.github.noamm9.features.impl.visual.RevertAxes;
import com.github.noamm9.utils.items.ItemUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {
    @Shadow private ItemStack mainHandItem;

    @Shadow private float oMainHandHeight;
    @Shadow private float mainHandHeight;

    @Shadow private float oOffHandHeight;
    @Shadow private float offHandHeight;

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void onBeforeRenderItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
        if (!Animations.INSTANCE.enabled) return;
        if (itemStack.isEmpty()) return;

        float sign = interactionHand == InteractionHand.MAIN_HAND ? 1.0f : -1.0f;

        poseStack.translate(
            Animations.INSTANCE.getMainHandX().getValue().floatValue() * sign,
            Animations.INSTANCE.getMainHandY().getValue().floatValue(),
            Animations.INSTANCE.getMainHandZ().getValue().floatValue()
        );
    }

    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), ordinal = 2, argsOnly = true)
    private float modifySwingProgress(float equipProgress) {
        if (!Animations.INSTANCE.enabled) return equipProgress;
        if (Animations.INSTANCE.getDisableSwingAnimation().getValue()) {
            boolean isTerminator = ItemUtils.INSTANCE.getSkyblockId(mainHandItem).equals("TERMINATOR");
            if (Animations.INSTANCE.getTerminatorOnly().getValue()) {
                if (isTerminator) return 1f;
            } else return 1f;
        }

        return equipProgress;
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void onRenderItem(AbstractClientPlayer abstractClientPlayer, float f, float g, InteractionHand interactionHand, float h, ItemStack itemStack, float i, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int j, CallbackInfo ci) {
        if (!Animations.INSTANCE.enabled) return;

        var s = (1.0f + Animations.INSTANCE.getMainHandItemScale().getValue().floatValue());
        poseStack.mulPose(Axis.XP.rotationDegrees(Animations.INSTANCE.getMainHandPositiveX().getValue().floatValue()));
        poseStack.mulPose(Axis.YP.rotationDegrees(Animations.INSTANCE.getMainHandPositiveY().getValue().floatValue()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Animations.INSTANCE.getMainHandPositiveZ().getValue().floatValue()));
        poseStack.scale(s, s, s);
    }

    @WrapOperation(method = "swingArm", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void onSwingArmTranslate(PoseStack instance, float f, float g, float h, Operation<Void> original) {
        if (!Animations.INSTANCE.enabled) {
            original.call(instance, f, g, h);
            return;
        }

        float xMult = Animations.INSTANCE.getSwingX().getValue().floatValue();
        float yMult = Animations.INSTANCE.getSwingY().getValue().floatValue();
        float zMult = Animations.INSTANCE.getSwingZ().getValue().floatValue();
        instance.translate(f * xMult, g * yMult, h * zMult);
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void onShouldSkipAnimation(ItemStack from, ItemStack to, CallbackInfoReturnable<Boolean> cir) {
        if (Animations.INSTANCE.enabled && Animations.INSTANCE.getDisableEquip().getValue()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onUpdateHeldItems(CallbackInfo ci) {
        if (Animations.INSTANCE.enabled && Animations.INSTANCE.getDisableEquip().getValue()) {
            oMainHandHeight = 1f;
            mainHandHeight = 1f;
            oOffHandHeight = 1f;
            offHandHeight = 1f;
        }
    }

    @ModifyVariable(
        method = "renderArmWithItem",
        at = @At("HEAD"),
        argsOnly = true
    )
    private ItemStack revertAxe(ItemStack original) {
        if (original == null || original.isEmpty()) return original;
        ItemStack replacement = RevertAxes.shouldReplace(original);
        return Objects.requireNonNullElse(replacement, original);
    }
}

