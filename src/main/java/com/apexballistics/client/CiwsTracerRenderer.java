package com.apexballistics.client;

import com.apexballistics.entity.CiwsTracerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class CiwsTracerRenderer extends EntityRenderer<CiwsTracerEntity> {
    public CiwsTracerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(CiwsTracerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Vec3 vel = entity.getDeltaMovement();
        poseStack.pushPose();
        if (vel.lengthSqr() > 1.0E-6) {
            float yRot = (float) (Mth.atan2(vel.x, vel.z) * (180F / Math.PI));
            float xRot = (float) (Mth.atan2(vel.y, vel.horizontalDistance()) * (180F / Math.PI));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(-xRot));
        }
        float length = 2.8f;
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        streak(consumer, matrix, 0.08f, length, 1.00f, 0.42f, 0.06f, 0.95f);
        streak(consumer, matrix, 0.032f, length + 0.45f, 1.00f, 0.88f, 0.42f, 1.00f);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void streak(VertexConsumer consumer, Matrix4f matrix, float radius, float length,
                               float r, float g, float b, float a) {
        float z0 = -length * 0.15f;
        float z1 = length;
        quad(consumer, matrix, -radius, -radius, z0, radius, -radius, z0, radius, -radius, z1, -radius, -radius, z1, r, g, b, a);
        quad(consumer, matrix, -radius, radius, z0, -radius, -radius, z0, -radius, -radius, z1, -radius, radius, z1, r, g, b, a);
        quad(consumer, matrix, radius, radius, z0, -radius, radius, z0, -radius, radius, z1, radius, radius, z1, r, g, b, a);
        quad(consumer, matrix, radius, -radius, z0, radius, radius, z0, radius, radius, z1, radius, -radius, z1, r, g, b, a);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float r, float g, float b, float a) {
        consumer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
    }

    @Override
    public ResourceLocation getTextureLocation(CiwsTracerEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
