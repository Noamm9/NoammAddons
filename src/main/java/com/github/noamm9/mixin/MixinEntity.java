package com.github.noamm9.mixin;

import com.github.noamm9.event.EventBus;
import com.github.noamm9.event.impl.CheckEntityGlowEvent;
import com.github.noamm9.features.impl.misc.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.noamm9.NoammAddons.mc;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Unique private int customGlowColor = 0xFFFFFF;
    @Unique private boolean glowForced = false;

    @Shadow
    public abstract float getYRot();

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

    /**
     * Fixes high mouse input delay when riding an entity (MC-206540).
     *
     * @author isXander
     * @license LGPL-3.0
     * @see <a href="https://github.com/isXander/Debugify/blob/11bcb3c53dd6cda7475fe3738df64d3835ebd6d1/src/client/java/dev/isxander/debugify/client/mixins/basic/mc206540/EntityMixin.java">Original Debugify Implementation</a>
     */
    @Inject(method = "onPassengerTurned", at = @At("HEAD"))
    private void fixCameraMovement(Entity passenger, CallbackInfo ci) {
        if (!Camera.INSTANCE.enabled || !Camera.getInputFix().getValue()) return;
        if (!passenger.isAlwaysTicking()) return;
        passenger.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
        float g = Mth.clamp(f, -180.0F, 180.0F);
        passenger.yRotO += g - f;
        passenger.setYRot(passenger.getYRot() + g - f);
        passenger.setYHeadRot(passenger.getYRot());
    }
}