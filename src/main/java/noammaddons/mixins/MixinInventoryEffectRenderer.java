package noammaddons.mixins;

import net.minecraft.client.renderer.InventoryEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import noammaddons.utils.LocationUtils;

@Mixin(InventoryEffectRenderer.class)
public class MixinInventoryEffectRenderer {
    @ModifyVariable(method = "updateActivePotionEffects", at = @At(value = "STORE"))
    public boolean updateActivePotionEffects(boolean hasVisibleEffect) {
        if (LocationUtils.INSTANCE.getInSkyblock()) {
            return false;
        } else return hasVisibleEffect;
    }
}
