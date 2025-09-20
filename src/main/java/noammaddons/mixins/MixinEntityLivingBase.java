package noammaddons.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import noammaddons.events.EntityDeathEvent;
import noammaddons.features.impl.misc.Animations;
import noammaddons.features.impl.misc.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static noammaddons.events.EventDispatcher.postAndCatch;

@Mixin(value = EntityLivingBase.class, priority = 9999)
public abstract class MixinEntityLivingBase {

    @Shadow
    public abstract boolean isPotionActive(Potion potionIn);

    @Shadow
    public abstract PotionEffect getActivePotionEffect(Potion potionIn);

    @Inject(method = {"getArmSwingAnimationEnd()I"}, at = @At("HEAD"), cancellable = true)
    public void adjustSwingLength(CallbackInfoReturnable<Integer> cir) {
        if (!Animations.INSTANCE.enabled) return;
        int length = Animations.INSTANCE.getIgnoreHaste() ? 6 : this.isPotionActive(Potion.digSpeed) ?
                6 - (1 + this.getActivePotionEffect(Potion.digSpeed).getAmplifier()) :
                (this.isPotionActive(Potion.digSlowdown) ?
                        6 + (1 + this.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2 : 6);
        cir.setReturnValue(Math.max((int) (length * Math.exp(-Animations.INSTANCE.getSpeed())), 1));
    }

    @Inject(method = "isPotionActive(Lnet/minecraft/potion/Potion;)Z", at = @At("HEAD"), cancellable = true)
    private void isPotionActive(Potion potion, CallbackInfoReturnable<Boolean> cir) {
        if (Camera.noNausea.getValue() && potion == Potion.confusion) cir.setReturnValue(false);
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onLivingDeath(DamageSource cause, CallbackInfo ci) {
        postAndCatch(new EntityDeathEvent((Entity) (Object) this));
    }
}

