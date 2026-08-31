package com.apexballistics.client;

import com.apexballistics.block.SirenType;
import com.apexballistics.blockentity.SirenBlockEntity;
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

public final class SirenRenderer implements BlockEntityRenderer<SirenBlockEntity> {
    private final ItemRenderer itemRenderer;

    public SirenRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SirenBlockEntity siren, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        SirenType type = siren.sirenType();
        if (type == SirenType.INDUSTRIAL) {
            return;
        }
        float time = siren.getLevel() == null ? partialTick : siren.getLevel().getGameTime() + partialTick;
        float spin = siren.sounding() ? time * 8.5f : time * 1.8f;
        poseStack.pushPose();
        if (type == SirenType.AIR_RAID) {
            poseStack.translate(0.5, 1.38, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            itemRenderer.renderStatic(new ItemStack(ModItems.AIR_RAID_HORN_COMPONENT.get()),
                    ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, buffers, siren.getLevel(), (int) siren.getBlockPos().asLong());
        } else {
            poseStack.translate(0.5, 2.28, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin * 0.35f));
            poseStack.mulPose(Axis.XP.rotationDegrees(-12.0f));
            itemRenderer.renderStatic(new ItemStack(ModItems.NUCLEAR_HORN_COMPONENT.get()),
                    ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, buffers, siren.getLevel(), (int) siren.getBlockPos().asLong() + 3);
        }
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(SirenBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
