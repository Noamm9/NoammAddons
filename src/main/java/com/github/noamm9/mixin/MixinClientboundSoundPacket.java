package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import com.github.noamm9.interfaces.IMutedSoundPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSoundPacket.class)
public class MixinClientboundSoundPacket implements IMutedSoundPacket {
    @Unique private boolean noammaddons$muted;

    @Override
    public void noammaddons$mute() {
        noammaddons$muted = true;
    }

    @Inject(method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V", at = @At("HEAD"), cancellable = true)
    private void skipMutedSound(ClientGamePacketListener listener, CallbackInfo ci) {
        if (noammaddons$muted && NoammAddons.getMc().packetProcessor().isSameThread()) ci.cancel();
    }
}