package noammaddons.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import noammaddons.features.impl.misc.ArrowFix;
import noammaddons.features.impl.misc.Camera;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static noammaddons.NoammAddons.getMc;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {
    @Shadow
    public InventoryPlayer inventory;
    @Shadow
    private ItemStack itemInUse;
    @Final
    @Shadow
    private GameProfile gameProfile;
    @Shadow
    private int itemInUseCount;

    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    public void getEyeHeight(CallbackInfoReturnable<Float> cir) {
        if (!Camera.INSTANCE.enabled) return;
        if (!Camera.smoothSneak.getValue()) return;
        EntityPlayer player = (EntityPlayer) (Object) this;
        float sneakingOffset = Camera.SmoothSneak.getEyeHeightHook(player);
        float newHeight = player.getDefaultEyeHeight() + sneakingOffset;
        player.eyeHeight = newHeight;
        cir.setReturnValue(newHeight);
    }

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void fixPullBack(CallbackInfo ci) {
        if (!ArrowFix.INSTANCE.enabled || getMc().thePlayer == null || getMc().theWorld == null) return;
        if (itemInUse == null || inventory == null || gameProfile == null) return;
        if (gameProfile != getMc().thePlayer.getGameProfile()) return;
        ItemStack itemStack = inventory.getCurrentItem();
        if (!ArrowFix.isShortbow(itemStack)) return;
        itemInUse = null;
        itemInUseCount = 0;
    }
}
