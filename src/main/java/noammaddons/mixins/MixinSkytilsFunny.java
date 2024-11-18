package noammaddons.mixins;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "gg.skytils.skytilsmod.features.impl.misc.Funny")
public class MixinSkytilsFunny {
    @Dynamic
    @Inject(method = "joinedSkyblock", at = @At("HEAD"), cancellable = true)
    private void onJoinedSkyblockTroll(@NotNull CallbackInfo ci) {
        ci.cancel();
    }
}
