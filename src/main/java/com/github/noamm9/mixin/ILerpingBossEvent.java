package com.github.noamm9.mixin;

import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LerpingBossEvent.class)
public interface ILerpingBossEvent {
    @Accessor("targetPercent")
    float getTargetPrecent();
}