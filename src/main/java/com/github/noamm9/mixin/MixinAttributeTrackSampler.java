package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.misc.TimeChanger;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.*;
import net.minecraft.world.timeline.AttributeTrackSampler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AttributeTrackSampler.class)
public class MixinAttributeTrackSampler {
    @Shadow @Final private Holder<WorldClock> clock;
    @Shadow @Final private ClockManager clockManager;

    @ModifyArg(method = "applyTimeBased", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/KeyframeTrackSampler;sample(J)Ljava/lang/Object;"), index = 0)
    private long changeTime(long ticks) {
        return clockManager instanceof ClientClockManager && clock.is(WorldClocks.OVERWORLD) && TimeChanger.extractingSky ? TimeChanger.getTime() : ticks;
    }
}