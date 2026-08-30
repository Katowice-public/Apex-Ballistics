package com.apexballistics.client;

import com.apexballistics.entity.MissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class MissileRenderer extends EntityRenderer<MissileEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "apexballistics", "textures/entity/missile.png");
    private final MissileModel model;

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new MissileModel(context.bakeLayer(MissileModel.LAYER));
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
        float scale = switch (entity.getKind()) {
            case ICBM -> 0.19f;
            case SLBM -> 0.175f;
            case SRBM -> 0.15f;
            case ALCM, CRUISE -> 0.13f;
            case SAM, AAM -> 0.11f;
        };
        poseStack.scale(scale, scale, scale);
        model.setupAnim(entity, 0.0f, 0.0f, entity.tickCount + partialTicks, 0.0f, 0.0f);
        int color = 0xFF000000 | entity.getKind().trailColor();
        int light = entity.tickCount < 14 ? LightTexture.FULL_BRIGHT : packedLight;
        model.renderToBuffer(poseStack, buffer.getBuffer(model.renderType(TEXTURE)),
                light, OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity entity) {
        return TEXTURE;
    }
}
