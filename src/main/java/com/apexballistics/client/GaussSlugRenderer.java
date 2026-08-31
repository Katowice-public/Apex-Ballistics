package com.apexballistics.client;

import com.apexballistics.entity.GaussSlugEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

public class GaussSlugRenderer extends EntityRenderer<GaussSlugEntity> {
    private final ItemRenderer itemRenderer;

    public GaussSlugRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.15f;
    }

    @Override
    public void render(GaussSlugEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(entity.heavy() ? 1.1f : 0.7f, entity.heavy() ? 1.1f : 0.7f, entity.heavy() ? 1.1f : 0.7f);
        this.itemRenderer.renderStatic(entity.getRenderStack(), ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GaussSlugEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
