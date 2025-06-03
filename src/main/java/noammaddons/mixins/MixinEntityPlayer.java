package noammaddons.mixins;

import net.minecraft.entity.player.EntityPlayer;
import noammaddons.features.impl.misc.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {
    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    public void getEyeHeight(CallbackInfoReturnable<Float> cir) {
        if (!Camera.INSTANCE.enabled) return;
        if (!Camera.smoothSneak.getValue()) return;
        EntityPlayer player = (EntityPlayer) (Object) this;
        float sneakingOffset = Camera.SmoothSneak.getEyeHeightHook(player);
        float newHeight = player.getDefaultEyeHeight() + sneakingOffset;
        player.eyeHeight = newHeight;
        cir.setReturnValue(newHeight);
    }
}
