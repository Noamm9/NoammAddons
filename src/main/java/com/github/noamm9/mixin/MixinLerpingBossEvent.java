package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.BossBarUpdateEvent;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(LerpingBossEvent.class)
public abstract class MixinLerpingBossEvent extends BossEvent {
    public MixinLerpingBossEvent(UUID uuid, Component component, BossBarColor bossBarColor, BossBarOverlay bossBarOverlay) {
        super(uuid, component, bossBarColor, bossBarOverlay);
    }

    @Inject(method = "setProgress", at = @At("HEAD"))
    private void onSetProgress(float newProgress, CallbackInfo ci) {
        EventBus.post(new BossBarUpdateEvent(name, newProgress));
    }
}