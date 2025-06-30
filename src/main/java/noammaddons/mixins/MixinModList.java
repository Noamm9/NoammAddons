package noammaddons.mixins;

import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import noammaddons.NoammAddons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

import static noammaddons.NoammAddons.getMc;


// full Credit to soshimee
// https://github.com/Noamm9/secretguide/blob/master/src/main/java/com/github/soshimee/secretguide/mixin/MixinModList.java
@Mixin(value = FMLHandshakeMessage.ModList.class, remap = false)
public class MixinModList {
    @Shadow
    private Map<String, String> modTags;

    @Inject(method = "<init>(Ljava/util/List;)V", at = @At("RETURN"))
    private void removeMod(List<ModContainer> modList, CallbackInfo ci) {
        if (!getMc().isIntegratedServerRunning()) {
            modTags.remove(NoammAddons.MOD_ID);
        }
    }
}
