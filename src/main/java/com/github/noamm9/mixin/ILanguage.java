package com.github.noamm9.mixin;

import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Language.class)
public interface ILanguage {
    @Invoker
    static Language invokeLoadDefault() {
        throw new UnsupportedOperationException();
    }
}

