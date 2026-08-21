package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.dev.ModHider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.KnownPacksManager;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/**
 * Servers can enumerate your mods from the known-pack list every Fabric mod contributes to.
 * Drop the packs the user has not whitelisted so only vanilla-looking entries survive.
 */
@Mixin(KnownPacksManager.class)
public class MixinKnownPacksManager {
    @WrapOperation(method = "trySelectingPacks", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private <V> V hideModPacks(Map<KnownPack, V> instance, Object key, Operation<V> original) {
        KnownPack pack = (KnownPack) key;

        if (! pack.namespace().equalsIgnoreCase("fabric")) return original.call(instance, pack);
        if (ModHider.isModAllowed(pack.id())) return original.call(instance, pack);

        return null;
    }
}
