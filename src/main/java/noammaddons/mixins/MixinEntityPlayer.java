package noammaddons.mixins;

import net.minecraft.entity.player.EntityPlayer;
import noammaddons.features.misc.SmoothSneaking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static noammaddons.noammaddons.config;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {
    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    public void getEyeHeight(CallbackInfoReturnable<Float> cir) {
        if (!config.getSmoothSneak()) return;
        EntityPlayer player = (EntityPlayer) (Object) this;
        float sneakingOffset = SmoothSneaking.getEyeHeightHook(player);
        float newHeight = player.getDefaultEyeHeight() + sneakingOffset;
        player.eyeHeight = newHeight;
        cir.setReturnValue(newHeight);
    }
}
