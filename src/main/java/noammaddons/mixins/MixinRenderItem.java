package noammaddons.mixins;

import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.features.general.DrawItemRarity.onSlotDraw;

@Mixin(RenderItem.class)
public abstract class MixinRenderItem {
    @Inject(method = "renderItemIntoGUI", at = @At("HEAD"))
    private void renderRarity(ItemStack stack, int x, int y, CallbackInfo ci) {
        onSlotDraw(stack, x, y);
    }
}