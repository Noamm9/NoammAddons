package noammaddons.mixins;

import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Container.class)
public interface AccessorContainer {
    @Accessor("transactionID")
    short getTransactionID();
}