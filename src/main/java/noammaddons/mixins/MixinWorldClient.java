package noammaddons.mixins;

import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.NoammAddons.mc;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient {
    @Shadow
    public abstract void playSound(double x, double y, double z, String soundName, float volume, float pitch, boolean distanceDelay);

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void onSoundPlay(double x, double y, double z, String soundName, float volume, float pitch, boolean distanceDelay, CallbackInfo ci) {
        if (mc.isCallingFromMinecraftThread()) return;
        ci.cancel();
        mc.addScheduledTask(() -> playSound(x, y, z, soundName, volume, pitch, distanceDelay));
    }
}
