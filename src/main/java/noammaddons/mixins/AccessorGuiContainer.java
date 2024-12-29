package noammaddons.mixins;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface AccessorGuiContainer {
    @Accessor("guiTop")
    int getGuiTop();

    @Accessor("guiLeft")
    int getGuiLeft();

    @Accessor("xSize")
    int getWidth();

    @Accessor("ySize")
    int getHeight();
}

