package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.PlayerInteractEvent;
import com.github.noamm9.features.impl.visual.CpsDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Shadow @Nullable public Screen screen;
    @Shadow @Nullable public HitResult hitResult;
    @Shadow public LocalPlayer player;
    @Shadow @Nullable public ClientLevel level;


    @Inject(method = "startAttack", at = @At("HEAD"))
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        CpsDisplay.addLeftClick();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void onStartUseItem(CallbackInfo ci) {
        CpsDisplay.addRightClick();
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
    private void preWhileAttack(boolean leftClickPressed, CallbackInfo ci) {
        if (!leftClickPressed) return;
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
}