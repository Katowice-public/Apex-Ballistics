package com.apexballistics.client;

import com.apexballistics.block.LauncherBlock;
import com.apexballistics.block.LauncherType;
import com.apexballistics.blockentity.LauncherBlockEntity;
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
import net.minecraft.world.phys.Vec3;

public final class LauncherBlockEntityRenderer implements BlockEntityRenderer<LauncherBlockEntity> {
    private final ItemRenderer itemRenderer;

    public LauncherBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(LauncherBlockEntity launcher, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ItemStack stored = launcher.getMissile();
        if (stored.isEmpty()) {
            return;
        }
        LauncherType type = launcher.launcherType();
        Vec3[] mounts = type.mountPoints();
        int count = Math.min(stored.getCount(), mounts.length);
        Direction facing = launcher.getBlockState().hasProperty(LauncherBlock.FACING)
                ? launcher.getBlockState().getValue(LauncherBlock.FACING)
                : Direction.NORTH;
        ItemStack vis = stored.copyWithCount(1);
        for (int i = 0; i < count; i++) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            poseStack.translate(-0.5, 0.0, -0.5);
            Vec3 mount = mounts[i];
            poseStack.translate(mount.x, mount.y, mount.z);
            if (type == LauncherType.PAD) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-38.0f));
            } else if (type == LauncherType.MOBILE) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(-18.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(-22.0f));
            }
            float scale = type.mountScale();
            poseStack.scale(scale, scale, scale);
            itemRenderer.renderStatic(vis, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, buffers, launcher.getLevel(), (int) launcher.getBlockPos().asLong() + i);
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(LauncherBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
