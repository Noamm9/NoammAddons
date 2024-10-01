package noammaddons.mixins;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import noammaddons.features.dungeons.ESP.MobESP;

@Pseudo
@Mixin(targets = "club.sk1er.patcher.util.world.render.culling.EntityCulling", remap = false)
public class MixinPatcherEntityCulling {
    @Dynamic
    @Inject(method = "checkEntity", at = @At("HEAD"), cancellable = true)
    private static void overrideEntityCulling(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (MobESP.INSTANCE.getColor(entity) != null || MobESP.INSTANCE.getStarMobs().contains(entity)) {
            cir.setReturnValue(false);
        }
    }
}
