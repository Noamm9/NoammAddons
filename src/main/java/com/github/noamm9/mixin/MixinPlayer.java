package com.github.noamm9.mixin;

import com.github.noamm9.NoammAddons;
import com.github.noamm9.features.impl.misc.ArrowFix;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MixinPlayer extends LivingEntity {
    protected MixinPlayer(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fixPullBack(CallbackInfo ci) {
        if (!ArrowFix.INSTANCE.enabled) return;
        if ((Player) (Object) this != NoammAddons.mc.player) return;
        if (useItem.isEmpty()) return;
        if (ArrowFix.isShortbow(useItem)) {
            useItem = ItemStack.EMPTY;
            useItemRemaining = 0;
        }
    }
}
