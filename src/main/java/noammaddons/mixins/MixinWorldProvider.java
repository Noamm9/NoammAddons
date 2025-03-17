package noammaddons.mixins;

import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static noammaddons.noammaddons.config;

@Mixin(WorldProvider.class)
public class MixinWorldProvider {
    @Unique
    private static final int[] TIME_VALUES = {1000, 6000, 12000, 13000, 18000, 23000};

    @Inject(method = "calculateCelestialAngle", at = @At("HEAD"), cancellable = true)
    public void calculateCelestialAngle(long worldTime, float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (!config.getTimeChanger()) return;
        long overrideTime = TIME_VALUES[config.getTimeChangerMode()];

        int i = (int) (overrideTime % 24000L);
        float f = ((float) i + partialTicks) / 24000.0F - 0.25F;
        if (f < 0.0F) f += 1.0F;
        if (f > 1.0F) f -= 1.0F;

        float f1 = 1.0F - (float) ((Math.cos(f * Math.PI) + 1.0) / 2.0);
        f += (f1 - f) / 3.0F;

        cir.setReturnValue(f);
    }
}
