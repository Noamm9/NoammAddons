package com.github.noamm9.mixin;


import com.github.noamm9.features.impl.dev.Cosmetics;
import com.github.noamm9.features.impl.dev.cosmetics.halo.HaloLayer;
import com.github.noamm9.features.impl.dungeon.TeammateESP;
import com.github.noamm9.features.impl.misc.NameTagTweaks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public class MixinAvatarRenderer {
    @SuppressWarnings("unchecked")
    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;Z)V", at = @At("TAIL"))
    private void addCosmeticLayers(EntityRendererProvider.Context context, boolean slimSteve, CallbackInfo ci) {
        var iSelf = (LivingEntityRendererAccessor<AvatarRenderState, PlayerModel>) (Object) this;
        var self = (RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) this;

        iSelf.noamm$addLayer(new HaloLayer(self));
    }

    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"))
    private void scale(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        Cosmetics.scaleHook(state, poseStack);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("HEAD"))
    private void extractRenderState(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        Cosmetics.extractRenderStateHook(entity, state);
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void shouldShowName(Avatar entity, double distanceToCameraSq, CallbackInfoReturnable<Boolean> cir) {
        if (TeammateESP.shouldHideNametag(entity)) cir.setReturnValue(false);
        else if (NameTagTweaks.shouldShowNametag(entity)) cir.setReturnValue(true);
    }
}