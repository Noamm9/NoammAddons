package noammaddons.mixins;

import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiIngame.class)
public interface AccessorGuiIngame {

    @Accessor("displayedTitle")
    String getDisplayTitle();

    @Accessor("displayedSubTitle")
    String getDisplaySubTitle();
}
