package com.apexballistics.client;

import com.apexballistics.block.SystemType;
import com.apexballistics.blockentity.DefenseSystemBlockEntity;
import com.apexballistics.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class DefenseSystemRenderer implements BlockEntityRenderer<DefenseSystemBlockEntity> {
    private final ItemRenderer itemRenderer;

    public DefenseSystemRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DefenseSystemBlockEntity system, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        SystemType type = system.systemType();
        if (type != SystemType.CIWS && type != SystemType.LASER_DEFENSE) {
            return;
        }
        float time = system.getLevel() == null
                ? partialTick
                : system.getLevel().getGameTime() + partialTick;
        poseStack.pushPose();
        if (type == SystemType.CIWS) {
            poseStack.translate(0.5, 1.22, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 4.6f));
            poseStack.mulPose(Axis.XP.rotationDegrees(-10.0f + Mth.sin(time * 0.08f) * 8.0f));
            poseStack.scale(1.20f, 1.20f, 1.20f);
            itemRenderer.renderStatic(new ItemStack(ModItems.CIWS_TURRET_COMPONENT.get()),
                    ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, buffers, system.getLevel(), (int) system.getBlockPos().asLong());
        } else {
            poseStack.translate(0.5, 0.70, 0.48);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(time * 0.045f) * 42.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(time * 0.033f) * 16.0f));
            itemRenderer.renderStatic(new ItemStack(ModItems.LASER_HEAD_COMPONENT.get()),
                    ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, buffers, system.getLevel(), (int) system.getBlockPos().asLong() + 1);
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(DefenseSystemBlockEntity blockEntity) {
        return true;
    }
}
