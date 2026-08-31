package com.apexballistics.client;

import com.apexballistics.blockentity.MissileShowcaseBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ShowcaseRenderer implements BlockEntityRenderer<MissileShowcaseBlockEntity> {
    private final ItemRenderer itemRenderer;

    public ShowcaseRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(MissileShowcaseBlockEntity showcase, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ItemStack stack = showcase.getMissile();
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 2.15, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(showcase.rotation() + partialTick * 1.4f));
        poseStack.scale(0.48f, 0.48f, 0.48f);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, showcase.getLevel(), (int) showcase.getBlockPos().asLong());
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MissileShowcaseBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }
}
