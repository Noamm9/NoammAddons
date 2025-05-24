package noammaddons.mixins;

import net.minecraft.client.renderer.tileentity.TileEntityChestRenderer;
import net.minecraft.tileentity.TileEntityChest;
import noammaddons.events.RenderChestEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.EventDispatcher.postAndCatch;

@Mixin(TileEntityChestRenderer.class)
public class MixinTileEntityChestRenderer {

    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityChest;DDDFI)V", at = @At("HEAD"))
    private void onDrawChest(TileEntityChest te, double x, double y, double z, float partialTicks, int destroyStage, CallbackInfo ci) {
        postAndCatch(new RenderChestEvent.Pre(te, x, y, z, partialTicks));
    }

    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityChest;DDDFI)V", at = @At("RETURN"))
    private void onDrawChestPost(TileEntityChest te, double x, double y, double z, float partialTicks, int destroyStage, CallbackInfo ci) {
        postAndCatch(new RenderChestEvent.Post(te, x, y, z, partialTicks));
    }
}

