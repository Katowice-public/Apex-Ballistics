package com.apexballistics.client;

import com.apexballistics.entity.MissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class MissileRenderer extends EntityRenderer<MissileEntity> {
    private final ItemRenderer itemRenderer;

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.45f;
    }

    @Override
    public void render(MissileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        Vec3 vel = entity.getDeltaMovement();
        if (vel.lengthSqr() > 1.0E-6) {
            float yRot = (float) (Mth.atan2(vel.x, vel.z) * (180F / Math.PI));
            float xRot = (float) (Mth.atan2(vel.y, vel.horizontalDistance()) * (180F / Math.PI));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(-xRot + 90.0f));
        }
        float age = entity.tickCount + partialTicks;
        float ignition = Mth.clamp(age / 12.0f, 0.0f, 1.0f);
        poseStack.translate(0.0, (1.0f - ignition) * Mth.sin(age * 2.8f) * 0.05f, 0.0);
        if (entity.getKind().profile() == com.apexballistics.item.MissileKind.FlightProfile.BALLISTIC) {
            poseStack.mulPose(Axis.YP.rotation(age * 0.055f));
        } else {
            poseStack.mulPose(Axis.ZP.rotation(Mth.sin(age * 0.22f) * 0.035f));
            poseStack.mulPose(Axis.XP.rotation(Mth.cos(age * 0.17f) * 0.025f));
        }
        float scale = switch (entity.getKind()) {
            case ICBM -> 1.30f;
            case SLBM -> 1.20f;
            case SRBM -> 1.05f;
            case ALCM, CRUISE -> 0.90f;
            case SAM, AAM, INTERCEPTOR -> 0.82f;
        };
        int light = entity.tickCount < 14 ? LightTexture.FULL_BRIGHT : packedLight;
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(entity.getRenderStack(), ItemDisplayContext.FIXED,
                light, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
