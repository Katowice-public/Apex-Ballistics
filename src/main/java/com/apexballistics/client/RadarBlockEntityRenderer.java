package com.apexballistics.client;

import com.apexballistics.blockentity.RadarBlockEntity;
import com.apexballistics.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

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
        BlockPos peer = radar.getCablePeer();
        if (peer != null) {
            renderCable(poseStack, buffers, radar.getBlockPos(), peer);
        }
    }

    private static void renderCable(PoseStack poseStack, MultiBufferSource buffers, BlockPos from, BlockPos to) {
        Vec3 start = new Vec3(0.5, 2.35, 0.5);
        Vec3 end = Vec3.atCenterOf(to).add(0.0, 1.35, 0.0).subtract(Vec3.atLowerCornerOf(from));
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        int segments = Math.max(8, (int) (start.distanceTo(end) * 2.0));
        Vec3 prev = start;
        for (int i = 1; i <= segments; i++) {
            float t = i / (float) segments;
            Vec3 point = start.lerp(end, t);
            double sag = Math.sin(t * Math.PI) * start.distanceTo(end) * 0.14;
            point = point.add(0.0, -sag, 0.0);
            float dx = (float) (point.x - prev.x);
            float dy = (float) (point.y - prev.y);
            float dz = (float) (point.z - prev.z);
            float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0e-4f) {
                length = 1.0f;
            }
            int color = i % 2 == 0 ? 0xFF1EC86A : 0xFF24322C;
            addLine(consumer, poseStack, matrix, prev, point, color, dx / length, dy / length, dz / length);
            prev = point;
        }
    }

    private static void addLine(VertexConsumer consumer, PoseStack poseStack, Matrix4f matrix,
                                Vec3 from, Vec3 to, int argb, float nx, float ny, float nz) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        consumer.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
                .setColor(r, g, b, a)
                .setNormal(poseStack.last(), nx, ny, nz);
        consumer.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
                .setColor(r, g, b, a)
                .setNormal(poseStack.last(), nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(RadarBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }
}
