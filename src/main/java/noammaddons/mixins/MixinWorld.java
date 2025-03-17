package noammaddons.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import noammaddons.events.EntityLeaveWorldEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.RegisterEvents.postAndCatch;

@Mixin(World.class)
public abstract class MixinWorld {
    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void onRemoveEntity(Entity entityIn, CallbackInfo ci) {
        postAndCatch(new EntityLeaveWorldEvent(entityIn, (World) (Object) this));
    }
}