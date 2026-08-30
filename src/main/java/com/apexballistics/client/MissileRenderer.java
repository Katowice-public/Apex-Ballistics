package com.apexballistics.client;

import com.apexballistics.entity.MissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
        poseStack.scale(1.6f, 1.6f, 1.6f);
        this.itemRenderer.renderStatic(entity.getRenderStack(), ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
