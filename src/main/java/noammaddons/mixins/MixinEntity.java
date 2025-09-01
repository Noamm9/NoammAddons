package noammaddons.mixins;

import net.minecraft.entity.Entity;
import noammaddons.features.impl.dungeons.FpsBoost;
import noammaddons.utils.LocationUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {
    @Inject(method = "canRenderOnFire", at = @At("HEAD"), cancellable = true)
    private void canRenderOnFireHook(CallbackInfoReturnable<Boolean> cir) {
        if (!FpsBoost.INSTANCE.enabled) return;
        if (!FpsBoost.hideFireOnEntities.getValue()) return;
        if (!LocationUtils.inSkyblock) return;
        cir.setReturnValue(false);
    }
}