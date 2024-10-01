package noammaddons.mixins;

import noammaddons.features.General.CustomItemEntity;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.entity.item.EntityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderEntityItem.class)
public class MixinRenderEntityItem  {

    @Inject(method = "doRender*", at = @At("HEAD"), cancellable = true)
    private void onDoRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (CustomItemEntity.INSTANCE.customItemEntity(entity))
            ci.cancel();
    }
}
