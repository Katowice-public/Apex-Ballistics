package com.apexballistics.client;

import com.apexballistics.blockentity.RadarBlockEntity;
import com.apexballistics.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class RadarBlockEntityRenderer implements BlockEntityRenderer<RadarBlockEntity> {
    private final ItemRenderer itemRenderer;

    public RadarBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(RadarBlockEntity radar, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        float time = radar.getLevel() == null
                ? partialTick
                : radar.getLevel().getGameTime() + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.5, 3.42, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.4f));
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.0f));
        poseStack.scale(1.15f, 1.15f, 1.15f);
        itemRenderer.renderStatic(new ItemStack(ModItems.RADAR_DISH_COMPONENT.get()),
                ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, radar.getLevel(), (int) radar.getBlockPos().asLong());
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(RadarBlockEntity blockEntity) {
        return true;
    }
}
