package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;

/**
 * Dedicated first-person 3D tactical visor model sharing the exact 128x128 UV layout
 * of {@link VisionGoggleModel} so all variant entity textures (NVG, Thermal, Hydro, Bio)
 * render cleanly without extra assets.
 */
public class FirstPersonVisorModel extends Model {
    public final ModelPart visorPivot;

    public FirstPersonVisorModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.visorPivot = root.getChild("visor_pivot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Main visor pivot centered around the mount hinge above the eyes, with full-width upper brow shroud
        PartDefinition visorPivot = partdefinition.addOrReplaceChild("visor_pivot", CubeListBuilder.create()
                .texOffs(24, 12).addBox(-4.0F, -1.5F, -1.7F, 8.0F, 2.0F, 3.0F, new CubeDeformation(-0.5F))
                .texOffs(24, 12).addBox(-4.5F, -3.2F, -1.7F, 9.0F, 2.4F, 3.0F, new CubeDeformation(-0.3F)),
                PartPose.ZERO);

        // Top rotational bridge detail
        visorPivot.addOrReplaceChild("rota_top", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-2.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F, new CubeDeformation(-0.5F)),
                PartPose.offsetAndRotation(0.0F, -1.6F, -0.5F, 0.3054F, 0.0F, 0.0F));

        // Upper Wilcox mount arm
        visorPivot.addOrReplaceChild("mount_arm", CubeListBuilder.create()
                .texOffs(54, 58).addBox(-1.0F, -2.2F, -1.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(-0.5F)),
                PartPose.offsetAndRotation(0.0F, -1.5F, 1.2F, 0.45F, 0.0F, 0.0F));

        float tY = 0.15F;
        float tZ = -0.72F;

        // Left outer optical tube with lens rings
        visorPivot.addOrReplaceChild("tube_left", CubeListBuilder.create()
                .texOffs(0, 79).addBox(-3.9625F, -0.7112F, -2.4819F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.3F))
                .texOffs(64, 13).addBox(-3.9625F, -0.7112F, 0.5181F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(36, 62).addBox(-3.9625F, -0.7112F, -1.4819F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(60, 10).addBox(-3.9625F, -0.7112F, -2.4819F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(0, 16).addBox(-3.9625F, -0.7112F, -2.8993F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.5F)),
                PartPose.offsetAndRotation(0.0F, tY, tZ, 0.088F, 0.1304F, 0.0115F));

        // Dual central optical tubes with lens rings
        visorPivot.addOrReplaceChild("tube_center", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-2.0F, -0.7112F, -2.5085F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.5F))
                .texOffs(22, 59).addBox(-2.0F, -0.7112F, -2.0911F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(58, 42).addBox(-2.0F, -0.7112F, -1.0911F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(40, 55).addBox(-2.0F, -0.7112F, 0.9089F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(27, 78).addBox(-2.0F, -0.7112F, -2.0911F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.3F))
                .texOffs(0, 4).addBox(0.0F, -0.7112F, -2.5085F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.5F))
                .texOffs(36, 48).addBox(0.0F, -0.7112F, -2.0911F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(48, 25).addBox(0.0F, -0.7112F, -1.0911F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(46, 36).addBox(0.0F, -0.7112F, 0.9089F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(55, 77).addBox(0.0F, -0.7112F, -2.0911F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, tY, tZ, 0.0873F, 0.0F, 0.0F));

        // Right outer optical tube with lens rings
        visorPivot.addOrReplaceChild("tube_right", CubeListBuilder.create()
                .texOffs(0, 0).addBox(1.9625F, -0.7112F, -2.8993F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.5F))
                .texOffs(18, 44).addBox(1.9625F, -0.7112F, -2.4819F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(12, 44).addBox(1.9625F, -0.7112F, -1.4819F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(38, 17).addBox(1.9625F, -0.7112F, 0.5181F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(76, 75).addBox(1.9625F, -0.7112F, -2.4819F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, tY, tZ, 0.088F, -0.1304F, -0.0115F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.visorPivot.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
