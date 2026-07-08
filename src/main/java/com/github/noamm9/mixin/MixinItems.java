package com.github.noamm9.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(Items.class)
public class MixinItems {
    @Inject(method = "registerItem(Ljava/lang/String;Lnet/minecraft/world/item/Item$Properties;)Lnet/minecraft/world/item/Item;", at = @At("HEAD"))
    private static void onRegisterStar(String name, Item.Properties properties, CallbackInfoReturnable<Item> cir) {
        if ("nether_star".equals(name)) properties.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
    }

    @Inject(method = "registerItem(Ljava/lang/String;Ljava/util/function/Function;Lnet/minecraft/world/item/Item$Properties;)Lnet/minecraft/world/item/Item;", at = @At("HEAD"))
    private static void onRegisterXpBottle(String name, Function<Item.Properties, Item> itemFactory, Item.Properties properties, CallbackInfoReturnable<Item> cir) {
        if ("experience_bottle".equals(name)) properties.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
    }
}