package noammaddons.mixins.accessor;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("inPortal")
    boolean isInPortal();

    @Accessor("inPortal")
    void setInPortal(boolean newValue);
}
