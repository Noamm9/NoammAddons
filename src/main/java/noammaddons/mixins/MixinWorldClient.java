package noammaddons.mixins;
/*
import noammaddons.events.EntityWorldEvent;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(WorldClient.class)
public class MixinWorldClient {

    @Inject(method = "addEntityToWorld", at = @At("HEAD"), cancellable = true)
    public void addEntityToWorld(int entityID, Entity entityToSpawn, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new EntityWorldEvent.Join(entityID, entityToSpawn))) {
            ci.cancel();
        }
    }

    @Inject(method = "removeEntityFromWorld", at = @At("HEAD"))
    public void removeEntityFromWorld(int entityID, CallbackInfoReturnable<Entity> cir) {
        if (MinecraftForge.EVENT_BUS.post(new EntityWorldEvent.Leave(entityID))) {
            cir.cancel();
        }
    }
}*/