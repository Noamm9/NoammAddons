package noammaddons.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import noammaddons.features.misc.SmoothSneaking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static noammaddons.noammaddons.config;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {
    @Unique
    private SmoothSneaking noammAddons$smoothSneaking = new SmoothSneaking();

    @Inject(method = "getEyeHeight", at = @At(value = "RETURN"), cancellable = true)
    public void getEyeHeight(CallbackInfoReturnable<Float> cir) {
        float returnValue = cir.getReturnValue();
        if (!config.getSmoothSneak()) {
            cir.setReturnValue(returnValue);
            return;
        }

        boolean isSneaking = ((Entity) (Object) this).isSneaking();
        if (isSneaking) returnValue += 0.08F;
        cir.setReturnValue(returnValue + noammAddons$smoothSneaking.getSneakingHeightOffset(isSneaking));
    }
}