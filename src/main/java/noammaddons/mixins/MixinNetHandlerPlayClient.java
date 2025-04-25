package noammaddons.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import noammaddons.events.PostEntityMetadataEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noammaddons.events.RegisterEvents.postAndCatch;

@Mixin(NetHandlerPlayClient.class)
class MixinNetHandlerPlayClient {

    @Shadow
    private Minecraft gameController;

    @Inject(method = "handleEntityMetadata", at = @At("RETURN"))
    private void onSetCustomNameTag(S1CPacketEntityMetadata packetIn, CallbackInfo ci) {
        Entity entity = gameController.theWorld.getEntityByID(packetIn.getEntityId());
        if (entity == null) return;
        postAndCatch(new PostEntityMetadataEvent(entity));
    }
}