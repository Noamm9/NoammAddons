package noammaddons.mixins;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import noammaddons.NoammAddons;
import noammaddons.features.impl.misc.ArrowFix;
import noammaddons.features.impl.misc.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {
    @Shadow
    public InventoryPlayer inventory;
    @Shadow
    private ItemStack itemInUse;
    @Shadow
    private int itemInUseCount;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void fixPullBack(CallbackInfo ci) {
        if (!ArrowFix.INSTANCE.enabled) return;
        if (itemInUse == null || inventory == null) return;
        if ((Object) this != NoammAddons.mc.thePlayer) return;
        ItemStack itemStack = inventory.getCurrentItem();
        if (!ArrowFix.isShortbow(itemStack)) return;
        itemInUse = null;
        itemInUseCount = 0;
    }

    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    public void getEyeHeightHook(CallbackInfoReturnable<Float> cir) {
        if (!Camera.INSTANCE.enabled) return;
        if (!Camera.smoothSneak.getValue()) return;
        EntityPlayer player = (EntityPlayer) (Object) this;
        float sneakingOffset = Camera.SmoothSneak.getEyeHeightHook(player);
        float newHeight = player.getDefaultEyeHeight() + sneakingOffset;
        player.eyeHeight = newHeight;
        cir.setReturnValue(newHeight);
    }
}
