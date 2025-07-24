package noammaddons.mixins.accessor;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(CommandHandler.class)
public interface AccessorCommandHandler {
    @Accessor("commandMap")
    Map<String, CommandBase> getCommandMap();
}