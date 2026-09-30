package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.CheckEntityGlowEvent;
import com.github.noamm9.event.impl.PlayerInteractEvent;
//? if <26.2 {
/*import com.github.noamm9.features.impl.general.storageoverlay.StorageOverlay;
*///? }
import com.github.noamm9.features.impl.visual.InfoDisplay;
import com.github.noamm9.interfaces.IGlowingEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//? if <26.2 {
/*import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
*///? } else {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//? }
import com.mojang.authlib.minecraft.UserApiService;
//? if <26.3 {
/*import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
*///? } else {
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
//? }
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
//? if <26.2 {
/*import net.minecraft.client.gui.screens.Screen;
*///? }
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if !cheat {
/*import com.github.noamm9.utils.render.LegitEntityVisibility;
*///? }

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    //? if <26.2 {
    /*@Shadow @Nullable public Screen screen;
    *///? }
    @Shadow @Nullable public HitResult hitResult;
    @Shadow public LocalPlayer player;
    @Shadow @Nullable public ClientLevel level;
    @Shadow @Final private User user;
    //? if >=26.2 {
    @Shadow public abstract User getUser();
    //? }
    @Inject(method = "startAttack", at = @At("HEAD"))
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        InfoDisplay.addLeftClick();
    }

    @Inject(
        method = "handleKeybinds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;startUseItem()V",
            ordinal = 0
        )
    )
    private void onUseClick(CallbackInfo ci) {
        InfoDisplay.addRightClick();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void preUseItem(CallbackInfo ci) {
        handleHitResult(ci, false);
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void preAttack(CallbackInfoReturnable<Boolean> cir) {
        handleHitResult(cir, true);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void preWhileAttack(boolean down, CallbackInfo ci) {
        if (! down) return;
        handleHitResult(ci, true);
    }

    @Unique
    private void handleHitResult(CallbackInfo ci, boolean isLeftClick) {
        if (this.player == null || this.level == null) return;
        ItemStack itemStack = player.getMainHandItem();

        PlayerInteractEvent event;

        if (this.hitResult == null || this.hitResult.getType() == HitResult.Type.MISS) {
            event = isLeftClick
                ? new PlayerInteractEvent.LEFT_CLICK.AIR(itemStack)
                : new PlayerInteractEvent.RIGHT_CLICK.AIR(itemStack);
        } else {
            event = switch (this.hitResult.getType()) {
                case ENTITY -> {
                    Entity entity = ((EntityHitResult) this.hitResult).getEntity();
                    yield isLeftClick
                        ? new PlayerInteractEvent.LEFT_CLICK.ENTITY(itemStack, entity)
                        : new PlayerInteractEvent.RIGHT_CLICK.ENTITY(itemStack, entity);
                }
                case BLOCK -> {
                    BlockPos pos = ((BlockHitResult) this.hitResult).getBlockPos();
                    yield isLeftClick
                        ? new PlayerInteractEvent.LEFT_CLICK.BLOCK(itemStack, pos)
                        : new PlayerInteractEvent.RIGHT_CLICK.BLOCK(itemStack, pos);
                }
                default -> isLeftClick
                    ? new PlayerInteractEvent.LEFT_CLICK.AIR(itemStack)
                    : new PlayerInteractEvent.RIGHT_CLICK.AIR(itemStack);
            };
        }

        if (EventBus.post(event)) ci.cancel();
    }

    //? if <26.2 {
    /*@Inject(method = "setScreen", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci, @Local(argsOnly = true) LocalRef<Screen> screenRef) {
        if (! StorageOverlay.INSTANCE.enabled) return;
        var newScreen = StorageOverlay.onScreenChange(this.screen, screen);
        if (newScreen != null) screenRef.set(newScreen);
    }
    
    *///? }
    // Apply our glow after other mods have changed the vanilla glow state
    @ModifyExpressionValue(
        method = "shouldEntityAppearGlowing",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isCurrentlyGlowing()Z")
    )
    private boolean onShouldEntityAppearGlowing(boolean original, Entity entity) {
        //? if !cheat {
         /*if (this.player ==null) return original;
         if (!LegitEntityVisibility.isVisible(this.player, entity)) {
         ((IGlowingEntity) entity).noammaddons$isGlowing(false);
         return original;
        }
        *///? }

        var event = new CheckEntityGlowEvent(entity);
        if (EventBus.post(event)) return false;

        var glow = (IGlowingEntity) entity;
        glow.noammaddons$isGlowing(event.getShouldGlow());
        glow.noammaddons$glowColor(event.getColor());

        return original || glow.noammaddons$isGlowing();
    }

    @SuppressWarnings("ConstantValue")
    //? if <26.2 {
    /*@Inject(method = "createUserApiService", at = @At("HEAD"), cancellable = true)
    private void onCreateUserApiService(YggdrasilAuthenticationService authService, GameConfig config, CallbackInfoReturnable<UserApiService> cir) {
    *///? } else if <26.3 {
    /*@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;createUserApiService(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Lnet/minecraft/client/main/GameConfig;)Lcom/mojang/authlib/minecraft/UserApiService;"))
    private UserApiService onCreateUserApiService(YggdrasilAuthenticationService authService, GameConfig config, Operation<UserApiService> original) {*/
    //? } else {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;createUserApiService(Lcom/mojang/authlib/services/MinecraftServicesDiscoveryService;Lnet/minecraft/client/main/GameConfig;)Lcom/mojang/authlib/minecraft/UserApiService;"))
    private UserApiService onCreateUserApiService(MinecraftServicesDiscoveryService discoveryService, GameConfig config, Operation<UserApiService> original) {
    //? }
        String token = user.getAccessToken();
        //? if <26.2 {
        /*if (token == null || token.equals("0") || token.equals("FabricMC")) return;
        cir.setReturnValue(authService.createUserApiService(token));
        *///? } else if <26.3 {
        /*if (token == null || token.equals("0") || token.equals("FabricMC")) return original.call(authService, config);
        return authService.createUserApiService(token);*/
        //? } else {
        if (token == null || token.equals("0") || token.equals("FabricMC")) return original.call(discoveryService, config);
        return discoveryService.createUserApiService(token);
        //? }
    }
}