package noammaddons.mixins;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import noammaddons.features.impl.dungeons.DungeonBreaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {
    @Inject(method = "clickBlock", at = @At("HEAD"))
    private void onBlockHit(BlockPos loc, EnumFacing face, CallbackInfoReturnable<Boolean> cir) {
        DungeonBreaker.onHitBlock(loc);
    }
}
