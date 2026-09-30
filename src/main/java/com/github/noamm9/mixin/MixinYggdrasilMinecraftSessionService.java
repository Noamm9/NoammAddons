package com.github.noamm9.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.properties.Property;
//? if <26.3 {
/*import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
*///? } else {
import com.mojang.authlib.services.MinecraftServicesSessionService;
//? }
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if <26.3 {
/*@Mixin(YggdrasilMinecraftSessionService.class)
*///? } else {
@Mixin(MinecraftServicesSessionService.class)
//? }
public class MixinYggdrasilMinecraftSessionService {
    @WrapOperation(method = "getPropertySignatureState", at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/properties/Property;hasSignature()Z"), remap = false)
    private boolean badSignatureFix(Property property, Operation<Boolean> operation) {
        return operation.call(property) && !property.signature().isEmpty();
    }
}