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

import static noammaddons.events.EventDispatcher.postAndCatch;

@Mixin(NetHandlerPlayClient.class)
class MixinNetHandlerPlayClient {

    @Shadow
    private Minecraft gameController;


    /*
    @Shadow
    @Final
    private NetworkManager netManager;

    @Shadow
    private boolean doneLoadingTerrain;*/

    @Inject(method = "handleEntityMetadata", at = @At("RETURN"))
    private void onSetCustomNameTag(S1CPacketEntityMetadata packetIn, CallbackInfo ci) {
        Entity entity = gameController.theWorld.getEntityByID(packetIn.getEntityId());
        if (entity == null) return;
        postAndCatch(new PostEntityMetadataEvent(entity));
    }

    /*
    @Inject(method = "handlePlayerPosLook", at = @At(value = "HEAD"), cancellable = true)
    public void noRotate(S08PacketPlayerPosLook packetIn, CallbackInfo ci) {
        if (!NoRotate.INSTANCE.enabled) return;
        ci.cancel();

        PacketThreadUtil.checkThreadAndEnqueue(packetIn, (NetHandlerPlayClient) (Object) this, this.gameController);

        EntityPlayer entityplayer = this.gameController.thePlayer;
        Set<S08PacketPlayerPosLook.EnumFlags> flags = packetIn.func_179834_f();
        double d0 = packetIn.getX();
        double d1 = packetIn.getY();
        double d2 = packetIn.getZ();
        float f = packetIn.getYaw();
        float f1 = packetIn.getPitch();

        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.X)) d0 += entityplayer.posX;
        else entityplayer.motionX = 0.0;
        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Y)) d1 += entityplayer.posY;
        else entityplayer.motionY = 0.0;
        if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Z)) d2 += entityplayer.posZ;
        else entityplayer.motionZ = 0.0;

        if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X_ROT)) f1 += entityplayer.rotationPitch;
        if (packetIn.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y_ROT)) f += entityplayer.rotationYaw;

        float a = entityplayer.rotationYaw;
        float b = entityplayer.rotationPitch;

        entityplayer.setPositionAndRotation(d0, d1, d2, f, f1);
        this.netManager.sendPacket(new C03PacketPlayer.C06PacketPlayerPosLook(entityplayer.posX, entityplayer.getEntityBoundingBox().minY, entityplayer.posZ, entityplayer.rotationYaw, entityplayer.rotationPitch, false));
        entityplayer.setPositionAndRotation(d0, d1, d2, a, b);

        if (!this.doneLoadingTerrain) {
            this.gameController.thePlayer.prevPosX = this.gameController.thePlayer.posX;
            this.gameController.thePlayer.prevPosY = this.gameController.thePlayer.posY;
            this.gameController.thePlayer.prevPosZ = this.gameController.thePlayer.posZ;
            this.doneLoadingTerrain = true;
            this.gameController.displayGuiScreen(null);
        }
    }*/
}