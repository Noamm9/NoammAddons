package noammaddons.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class TestMixin extends AbstractClientPlayer {
    @Shadow
    @Final
    public NetHandlerPlayClient sendQueue;
    @Shadow
    protected Minecraft mc;

    public TestMixin(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    @Shadow
    public abstract void closeScreenAndDropStack();

    @Inject(method = "closeScreen", at = @At("HEAD"), cancellable = true)
    public void closeScreenOverride(CallbackInfo ci) {
        if (mc.isCallingFromMinecraftThread()) return;
        ci.cancel();
        mc.addScheduledTask(() -> {
            this.sendQueue.addToSendQueue(new C0DPacketCloseWindow(this.openContainer.windowId));
            this.closeScreenAndDropStack();
        });
    }
}
