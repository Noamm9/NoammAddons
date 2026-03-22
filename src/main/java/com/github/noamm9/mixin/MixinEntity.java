package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.CheckEntityGlowEvent;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.noamm9.NoammAddons.mc;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Unique private int customGlowColor = 0xFFFFFF;
    @Unique private boolean glowForced = false;

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        var entity = (Entity) (Object) this;
        var event = new CheckEntityGlowEvent(entity);
        EventBus.post(event);

        //#if CHEAT
        glowForced = event.getShouldGlow();
        customGlowColor = event.getColor().getRGB();
        //#else
        glowForced = event.getShouldGlow() && mc.player.hasLineOfSight(entity) && !entity.isInvisibleTo(mc.player);
        customGlowColor = event.getColor().getRGB();
        //#endif

        if (this.glowForced) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        if (this.glowForced) cir.setReturnValue(this.customGlowColor);
    }
}