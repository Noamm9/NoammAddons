package com.github.noamm9.mixin;

import com.github.noamm9.interfaces.IMutedSoundPacket;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

// reason this exists cuz devonian does this stupid shit: https://github.com/Synnerz/devonian/blob/8e5d13b0ba0fdbcf1e98673e156c3bc19678f63b/src/main/kotlin/com/github/synnerz/devonian/api/events/EventBus.kt#L136-L147
@Pseudo
@Mixin(targets = "com.github.synnerz.devonian.api.events.EventBus", remap = false)
public class MixinDevonianEventBus {
    @WrapOperation(
        method = "*",
        at = @At(value = "INVOKE", target = "Lcom/github/synnerz/devonian/api/events/PacketReceivedEvent;cancel()V")
    )
    private static void deferSoundCancellation(@Coerce Object event, Operation<Void> original, @Local(name = "packet") Packet<?> packet) {
        if (packet instanceof ClientboundSoundPacket) {
            // why the fuck does devonian cancel the whole packet just to mute a sound???
            ((IMutedSoundPacket) packet).noammaddons$mute();
        }
        else original.call(event);
    }
}
