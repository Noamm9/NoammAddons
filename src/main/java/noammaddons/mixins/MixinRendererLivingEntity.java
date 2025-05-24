package noammaddons.mixins;

import kotlin.Pair;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.EntityLivingBase;
import noammaddons.events.PostRenderEntityModelEvent;
import noammaddons.features.impl.esp.ChamNametags;
import noammaddons.features.impl.esp.GlobalEspSettings;
import noammaddons.utils.RenderHelper;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.nio.FloatBuffer;

import static noammaddons.events.EventDispatcher.postAndCatch;
import static noammaddons.utils.EspUtils.chamEntities;
import static org.lwjgl.opengl.GL11.*;


@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {
    @Final
    @Shadow
    private static DynamicTexture textureBrightness;
    @Shadow
    protected ModelBase mainModel;
    @Shadow
    protected FloatBuffer brightnessBuffer;

    @Unique
    private boolean noammAddons$hasCham(EntityLivingBase entity) {
        return chamEntities.stream().anyMatch(it -> it.getFirst() == entity);
    }

    @Unique
    private void noammAddons$removeChamESP(EntityLivingBase entity) {
        chamEntities.removeIf(it -> it.getFirst() == entity);
    }

    @Unique
    private Color noammAddons$getChamColor(EntityLivingBase entity) {
        return chamEntities.stream().filter(it -> it.getFirst() == entity).map(Pair::getSecond).findAny().orElse(null);
    }


    @Inject(method = "renderLayers", at = @At("RETURN"))
    private <T extends EntityLivingBase> void onRenderLayersPost(T entitylivingbaseIn, float p_177093_2_, float p_177093_3_, float partialTicks, float p_177093_5_, float p_177093_6_, float p_177093_7_, float p_177093_8_, CallbackInfo ci) {
        if (noammAddons$hasCham(entitylivingbaseIn)) noammAddons$removeChamESP(entitylivingbaseIn);

        postAndCatch(new PostRenderEntityModelEvent(
                entitylivingbaseIn,
                p_177093_2_, p_177093_3_,
                p_177093_5_, p_177093_6_,
                p_177093_7_, p_177093_8_,
                mainModel
        ));
    }

    @Inject(method = "setBrightness", at = @At(value = "HEAD"), cancellable = true)
    private <T extends EntityLivingBase> void setBrightness(T entity, float partialTicks, boolean combineTextures, CallbackInfoReturnable<Boolean> cir) {
        Color chamColor = noammAddons$getChamColor(entity);
        if (chamColor == null) return;

        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GL11.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.defaultTexUnit);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PRIMARY_COLOR);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.defaultTexUnit);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        GL11.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, OpenGlHelper.GL_INTERPOLATE);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_CONSTANT);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE2_RGB, OpenGlHelper.GL_CONSTANT);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND2_RGB, 770);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
        brightnessBuffer.position(0);
        brightnessBuffer.put(chamColor.getRed() / 255f);
        brightnessBuffer.put(chamColor.getGreen() / 255f);
        brightnessBuffer.put(chamColor.getBlue() / 255f);
        brightnessBuffer.put(GlobalEspSettings.INSTANCE.getFillOpacity().floatValue() / 100f);
        brightnessBuffer.flip();
        GL11.glTexEnv(8960, 8705, brightnessBuffer);
        GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(textureBrightness.getGlTextureId());
        GL11.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_PREVIOUS);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.lightmapTexUnit);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

        cir.setReturnValue(true);
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("HEAD"))
    private <T extends EntityLivingBase> void injectChamsPre(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo callbackInfo) {
        if (noammAddons$hasCham(entity)) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1f, -1000000F);
        }
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("RETURN"))
    private <T extends EntityLivingBase> void injectChamsPost(T entity, double x, double y, double z, float a, float b, CallbackInfo callbackInfo) {
        if (!noammAddons$hasCham(entity)) return;
        glPolygonOffset(1f, 1000000F);
        glDisable(GL_POLYGON_OFFSET_FILL);
    }


    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At("HEAD"))
    private <T extends EntityLivingBase> void injectChamsPre(T entity, double x, double y, double z, CallbackInfo ci) {
        if (!ChamNametags.INSTANCE.enabled) return;
        RenderHelper.enableChums(Color.WHITE);
    }

    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At("RETURN"))
    private <T extends EntityLivingBase> void injectChamsPost(T entity, double x, double y, double z, CallbackInfo ci) {
        if (!ChamNametags.INSTANCE.enabled) return;
        RenderHelper.disableChums();
    }
}

