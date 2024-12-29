package noammaddons.mixins;

import net.minecraft.client.entity.EntityPlayerSP;
import noammaddons.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {
    @Inject(method = "pushOutOfBlocks(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void injectPushOutOfBlocks(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (Config.INSTANCE.getNoPushOutOfBlocks()) {
            cir.setReturnValue(false);
        }
    }
}

