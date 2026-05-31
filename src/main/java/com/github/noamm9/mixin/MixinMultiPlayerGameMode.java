package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.ContainerEvent;
import com.github.noamm9.features.impl.dungeon.BreakerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void onHandleSlotClick(int containerId, int slotNum, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        if (minecraft.screen == null) return;
        if (!(minecraft.screen instanceof AbstractContainerScreen<?>)) return;
        if (EventBus.post(new ContainerEvent.SlotClick(minecraft.screen, slotNum, buttonNum, containerInput))) {
            ci.cancel();
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void onBlockHit(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BreakerHelper.onHitBlock(pos);
    }
}