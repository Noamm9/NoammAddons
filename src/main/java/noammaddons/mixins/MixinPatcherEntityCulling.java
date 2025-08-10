package noammaddons.mixins;

import net.minecraft.entity.Entity;
import noammaddons.features.impl.esp.StarMobESP;
import noammaddons.features.impl.esp.WitherESP;
import noammaddons.utils.EspUtils;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "club.sk1er.patcher.util.world.render.culling.EntityCulling", remap = false)
public class MixinPatcherEntityCulling {
    @Dynamic
    @Inject(method = "checkEntity", at = @At("HEAD"), cancellable = true)
    private static void overrideEntityCulling(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (EspUtils.ESPType.getEntries().stream().anyMatch(it -> it.containsEntity(entity))
                || StarMobESP.starMobs.contains(entity) || WitherESP.Wither.Companion.getCurrentWither() == entity
        ) {
            cir.setReturnValue(false);
        }
    }
}
