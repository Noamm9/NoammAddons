package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.dev.cosmetics.badges.BadgeText;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {
    @WrapOperation(
        method = "extractRenderState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;getNameForDisplay(Lnet/minecraft/client/multiplayer/PlayerInfo;)Lnet/minecraft/network/chat/Component;")
    )
    private Component decoratePlayerName(PlayerTabOverlay overlay, PlayerInfo info, Operation<Component> original) {
        return BadgeText.decorate(original.call(overlay, info), info.getProfile().id());
    }
}
