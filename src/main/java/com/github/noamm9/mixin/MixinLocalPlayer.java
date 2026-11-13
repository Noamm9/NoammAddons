package com.github.noamm9.mixin;

//#if CHEAT

import com.github.noamm9.features.impl.visual.PlayerOpacity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    @ModifyExpressionValue(method = { "pick", "raycastHitResult" }, at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/EntitySelector;CAN_BE_PICKED:Ljava/util/function/Predicate;", opcode = Opcodes.GETSTATIC))
    private static Predicate<Entity> clickThroughTranslucentPlayers(Predicate<Entity> original) {
        return original.and(entity -> ! PlayerOpacity.shouldClickThrough(entity));
    }
}
//#endif