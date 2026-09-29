package com.github.noamm9.mixin;

import com.github.noamm9.features.impl.dev.ModHider;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//? if <26.3 {
/*import java.util.function.Function;
import java.util.stream.Stream;*/
//? }

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinAbstractSignEditScreen {
    //? if <26.3 {
    /*@Redirect(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;ZZLnet/minecraft/network/chat/Component;)V", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"))
    public Stream<String> init(Stream<Component> instance, Function<Component, String> function) {
        return instance.map(ModHider::getString);*/
    //? } else {
    @Redirect(method = "<init>(Lnet/minecraft/world/level/block/entity/SignBlockEntity;Lnet/minecraft/world/level/block/entity/SignTextSlot;ZLnet/minecraft/network/chat/Component;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;getString()Ljava/lang/String;"))
    private String getSignText(Component component) {
        return ModHider.getString(component);
    //? }
    }
}