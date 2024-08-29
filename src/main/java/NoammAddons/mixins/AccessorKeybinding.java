package NoammAddons.mixins;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyBinding.class)
public interface AccessorKeybinding {
    @Invoker("unpressKey")
    void invokeUnpressKey();
}

