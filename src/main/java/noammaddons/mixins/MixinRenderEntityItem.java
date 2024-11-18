package noammaddons.mixins;

import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.entity.item.EntityItem;
import noammaddons.events.RenderItemEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.RegisterEvents.postAndCatch;

@Mixin(RenderEntityItem.class)
public class MixinRenderEntityItem {
    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At("HEAD"), cancellable = true)
    private void onDoRenderPre(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (postAndCatch(new RenderItemEntityEvent.Pre(
                entity,
                x, y, z,
                entityYaw,
                partialTicks
        ))) {
            ci.cancel();
        }
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItem;DDDFF)V", at = @At("RETURN"))
    private void onDoRenderPost(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        postAndCatch(new RenderItemEntityEvent.Post(
                entity,
                x, y, z,
                entityYaw,
                partialTicks
        ));
    }
}

