package noammaddons.mixins;


import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.boss.IBossDisplayData;
import noammaddons.events.BossbarUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.EventDispatcher.postAndCatch;

@Mixin(BossStatus.class)
abstract class MixinBossStatus {
    @Shadow
    public static int statusBarTime;

    @Inject(method = "setBossStatus", at = @At("HEAD"), cancellable = true)
    private static void onBossbarUpdate(IBossDisplayData displayData, boolean hasColorModifierIn, CallbackInfo ci) {
        String bossName = displayData.getDisplayName().getFormattedText();
        float maxHealth = displayData.getMaxHealth();
        float health = displayData.getHealth();
        float healthScale = health / maxHealth;
        float healthPresent = healthScale * 100;
        boolean cancel = postAndCatch(new BossbarUpdateEvent.Pre(bossName, maxHealth, health, healthScale, healthPresent));
        if (cancel) ci.cancel();
    }

    @Inject(method = "setBossStatus", at = @At("RETURN"))
    private static void onBossbarUpdatePost(IBossDisplayData displayData, boolean hasColorModifierIn, CallbackInfo ci) {
        String bossName = displayData.getDisplayName().getFormattedText();
        float maxHealth = displayData.getMaxHealth();
        float health = displayData.getHealth();
        float healthScale = health / maxHealth;
        float healthPresent = healthScale * 100;
        postAndCatch(new BossbarUpdateEvent.Post(bossName, maxHealth, health, healthScale, healthPresent));
    }

    @Inject(method = "setBossStatus", at = @At("TAIL"))
    private static void onSetBossStatus(IBossDisplayData displayData, boolean hasColorModifierIn, CallbackInfo ci) {
        statusBarTime = 1000;
    }
}