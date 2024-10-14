package noammaddons.mixins;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public class MixinEntity implements EntityAccessor {
    @Shadow protected boolean inPortal;

    @Override
    public void setInPortal(boolean newValue) {
        this.inPortal = newValue;
    }

    @Override
    public boolean isInPortal() {
        return this.inPortal;
    }
}

