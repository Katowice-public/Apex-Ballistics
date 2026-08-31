package com.apexballistics.client;

import com.apexballistics.ApexBallistics;
import com.apexballistics.entity.MissileEntity;
import com.apexballistics.item.MissileKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A dependency-free, articulated 3D missile shared by every guided round.
 *
 * <p>The model has three procedural animations: motor ignition recoil, roll
 * stabilization, and guidance-fin actuation. Keeping them procedural means
 * multiplayer clients see smooth animation without extra network packets.</p>
 */
public final class MissileModel extends EntityModel<MissileEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(ApexBallistics.MOD_ID, "missile"), "main");

    private final ModelPart root;
    private final ModelPart airframe;
    private final ModelPart nozzle;
    private final ModelPart northFin;
    private final ModelPart southFin;
    private final ModelPart westFin;
    private final ModelPart eastFin;

    public MissileModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.airframe = root.getChild("airframe");
        this.nozzle = airframe.getChild("nozzle");
        this.northFin = airframe.getChild("north_fin");
        this.southFin = airframe.getChild("south_fin");
        this.westFin = airframe.getChild("west_fin");
        this.eastFin = airframe.getChild("east_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition airframe = root.addOrReplaceChild("airframe", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0f, -20.0f, -2.0f, 4.0f, 16.0f, 4.0f)
                        .texOffs(0, 20).addBox(-1.5f, -23.0f, -1.5f, 3.0f, 3.0f, 3.0f)
                        .texOffs(12, 20).addBox(-1.0f, -25.0f, -1.0f, 2.0f, 2.0f, 2.0f),
                PartPose.offset(0.0f, 12.0f, 0.0f));

        airframe.addOrReplaceChild("nozzle", CubeListBuilder.create()
                        .texOffs(16, 0).addBox(-1.5f, -4.0f, -1.5f, 3.0f, 4.0f, 3.0f),
                PartPose.ZERO);
        airframe.addOrReplaceChild("north_fin", CubeListBuilder.create()
                        .texOffs(28, 0).addBox(-0.5f, -9.0f, -5.0f, 1.0f, 6.0f, 3.0f),
                PartPose.ZERO);
        airframe.addOrReplaceChild("south_fin", CubeListBuilder.create()
                        .texOffs(28, 0).addBox(-0.5f, -9.0f, 2.0f, 1.0f, 6.0f, 3.0f),
                PartPose.ZERO);
        airframe.addOrReplaceChild("west_fin", CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-5.0f, -9.0f, -0.5f, 3.0f, 6.0f, 1.0f),
                PartPose.ZERO);
        airframe.addOrReplaceChild("east_fin", CubeListBuilder.create()
                        .texOffs(36, 0).addBox(2.0f, -9.0f, -0.5f, 3.0f, 6.0f, 1.0f),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(MissileEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root.resetPose();
        airframe.resetPose();
        nozzle.resetPose();
        northFin.resetPose();
        southFin.resetPose();
        westFin.resetPose();
        eastFin.resetPose();

        // Animation 1: the motor settles into full thrust during ignition.
        float ignition = Mth.clamp(ageInTicks / 12.0f, 0.0f, 1.0f);
        airframe.y = 12.0f + (1.0f - ignition) * Mth.sin(ageInTicks * 2.8f) * 0.7f;
        nozzle.xScale = 0.85f + ignition * 0.15f;
        nozzle.zScale = nozzle.xScale;
        nozzle.yScale = 0.75f + ignition * 0.25f
                + Mth.sin(ageInTicks * 1.7f) * 0.04f;

        // Animation 2: ballistic rounds roll for stability; guided rounds make
        // smaller corrections so their silhouette remains readable.
        float rollRate = entity.getKind().profile() == MissileKind.FlightProfile.BALLISTIC
                ? 0.16f : 0.035f;
        airframe.yRot = ageInTicks * rollRate;

        // Animation 3: opposing control fins articulate as the autopilot trims.
        float authority = entity.getKind().profile() == MissileKind.FlightProfile.HOMING_AIR
                ? 0.32f : 0.16f;
        float pitchTrim = Mth.sin(ageInTicks * 0.45f) * authority;
        float yawTrim = Mth.cos(ageInTicks * 0.39f) * authority;
        northFin.xRot = pitchTrim;
        southFin.xRot = -pitchTrim;
        westFin.zRot = yawTrim;
        eastFin.zRot = -yawTrim;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, int packedColor) {
        root.render(poseStack, consumer, packedLight, packedOverlay, packedColor);
    }
}
