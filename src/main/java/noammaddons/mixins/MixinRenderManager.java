package noammaddons.mixins;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import noammaddons.events.PostRenderEntityEvent;
import noammaddons.events.RenderEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static noammaddons.events.RegisterEvents.postAndCatch;

@Mixin(RenderManager.class)
public class MixinRenderManager {
    @Inject(method = "doRenderEntity", at = @At("HEAD"), cancellable = true)
    public void onPreEntityRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks, boolean p_147939_10_, CallbackInfoReturnable<Boolean> cir) {
        if (postAndCatch(new RenderEntityEvent(entity, x, y, z, partialTicks))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doRenderEntity", at = @At(value = "RETURN", ordinal = 1))
    public void onPostEntityRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks, boolean p_147939_10_, CallbackInfoReturnable<Boolean> cir) {
        postAndCatch(new PostRenderEntityEvent(entity, x, y, z, partialTicks));
    }
}
