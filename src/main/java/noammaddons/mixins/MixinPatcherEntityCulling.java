package noammaddons.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityBlaze;
import noammaddons.features.impl.dungeons.dmap.core.map.Room;
import noammaddons.features.impl.dungeons.dmap.core.map.RoomData;
import noammaddons.features.impl.esp.StarMobESP;
import noammaddons.features.impl.esp.WitherESP;
import noammaddons.utils.EspUtils;
import noammaddons.utils.LocationUtils;
import noammaddons.utils.ScanUtils;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Pseudo
@Mixin(targets = "club.sk1er.patcher.util.world.render.culling.EntityCulling", remap = false)
public class MixinPatcherEntityCulling {
    @Dynamic
    @Inject(method = "checkEntity", at = @At("HEAD"), cancellable = true)
    private static void overrideEntityCulling(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        boolean inEspList = EspUtils.ESPType.getEntries().stream().anyMatch(it -> it.containsEntity(entity));
        boolean isStarMob = StarMobESP.starMobs.contains(entity);
        boolean isWither = WitherESP.Wither.Companion.getCurrentWither() == entity;
        boolean isBlaze = entity instanceof EntityBlaze &&
                LocationUtils.inDungeon && !LocationUtils.inBoss &&
                Optional.ofNullable(ScanUtils.INSTANCE.getEntityRoom(entity))
                        .map(Room::getData).map(RoomData::getName).map("Blaze"::equals).orElse(false);

        if (inEspList || isStarMob || isWither || isBlaze) {
            cir.setReturnValue(false);
        }
    }
}
