package com.apexballistics.client;

import com.apexballistics.block.DroneLauncherBlock;
import com.apexballistics.blockentity.DroneLauncherBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class DroneLauncherRenderer implements BlockEntityRenderer<DroneLauncherBlockEntity> {
    private final ItemRenderer itemRenderer;

    public DroneLauncherRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DroneLauncherBlockEntity launcher, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ItemStack drone = launcher.getDrone();
        if (drone.isEmpty()) {
            return;
        }
        Direction facing = launcher.getBlockState().hasProperty(DroneLauncherBlock.FACING)
                ? launcher.getBlockState().getValue(DroneLauncherBlock.FACING)
                : Direction.NORTH;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(0.45, 1.15, 2.15);
        poseStack.mulPose(Axis.XP.rotationDegrees(-45.0f));
        poseStack.scale(0.72f, 0.72f, 0.72f);
        itemRenderer.renderStatic(drone.copyWithCount(1), ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, launcher.getLevel(),
                (int) launcher.getBlockPos().asLong());
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(DroneLauncherBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
