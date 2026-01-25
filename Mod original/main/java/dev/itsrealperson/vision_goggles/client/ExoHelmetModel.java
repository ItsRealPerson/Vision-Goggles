package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class ExoHelmetModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(VisionGoggles.MODID, "exo_helmet"), "main");
    public final ModelPart head_bone;

    public ExoHelmetModel(ModelPart root) {
        super(root);
        this.head_bone = root.getChild("head_bone");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 1. HEAD BONE (Helmet Shell)
        // Inflate ajustado (-0.xxx9F) para evitar Z-fighting
        PartDefinition head_bone = partdefinition.addOrReplaceChild("head_bone", CubeListBuilder.create()
                .texOffs(74, 26).addBox(-3.0F, -9.0F, -6.4F, 6.0F, 4.0F, 3.0F, new CubeDeformation(-0.599F)) // Frontal
                .texOffs(26, 49).addBox(-5.0F, -9.0F, -5.0F, 2.0F, 3.5F, 10.0F, new CubeDeformation(-0.399F)) // Lateral Izq
                .texOffs(48, 45).addBox(3.0F, -9.0F, -5.0F, 2.0F, 3.5F, 10.0F, new CubeDeformation(-0.399F)) // Lateral Der
                .texOffs(37, 114).addBox(-6.4F, -7.6F, -6.4F, 12.8F, 2.0F, 12.9F, new CubeDeformation(-0.499F)) // Rim
                .texOffs(24, 20).addBox(-4.0F, -8.7F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.2F))
                .texOffs(0, 112).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Detalle lateral (Parte superior rotada)
        head_bone.addOrReplaceChild("head_detail", CubeListBuilder.create()
                .texOffs(50, 47).addBox(-1.0F, -1.0F, -4.7F, 2.0F, 1.6F, 9.3F, new CubeDeformation(-0.399F)),
                PartPose.offsetAndRotation(0.0F, -8.7F, 0.1F, 0.0F, 1.5708F, 0.0F));

        // 2. VISOR (Main Anchor)
        PartDefinition visor = head_bone.addOrReplaceChild("visor", CubeListBuilder.create()
                .texOffs(24, 12).addBox(-4.0F, -1.04F, -1.7F, 8.0F, 2.0F, 3.0F, new CubeDeformation(-0.499F)),
                PartPose.offset(0.0F, -2.3355F, -7.7094F));

        // 3. PIEZAS HIJAS
        // Rota Top
        visor.addOrReplaceChild("rota_top", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-2.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F, new CubeDeformation(-0.499F)),
                PartPose.offsetAndRotation(0.0F, -2.127F, -0.484F, 0.3054F, 0.0F, 0.0F));

        // Tubos
        float tY = -0.8F; 
        float tZ = -0.719F;

        visor.addOrReplaceChild("tube_left", CubeListBuilder.create()
                .texOffs(0, 79).addBox(-3.962F, -1.29F, -2.48F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.299F))
                .texOffs(64, 13).addBox(-3.962F, -1.29F, 0.52F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(36, 62).addBox(-3.962F, -1.29F, -1.48F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(60, 10).addBox(-3.962F, -1.29F, -2.48F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(0, 16).addBox(-3.962F, -1.29F, -2.9F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.499F)),
                PartPose.offsetAndRotation(0.0F, tY, tZ, 0.088F, 0.1304F, 0.0115F));

        visor.addOrReplaceChild("tube_center", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-2.0F, -1.29F, -2.51F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.499F))
                .texOffs(22, 59).addBox(-2.0F, -1.29F, -2.09F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(58, 42).addBox(-2.0F, -1.29F, -1.09F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(40, 55).addBox(-2.0F, -1.29F, 0.91F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(27, 78).addBox(-2.0F, -1.29F, -2.09F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.299F))
                .texOffs(0, 4).addBox(0.0F, -1.29F, -2.51F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.499F))
                .texOffs(36, 48).addBox(0.0F, -1.29F, -2.09F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(48, 25).addBox(0.0F, -1.29F, -1.09F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(46, 36).addBox(0.0F, -1.29F, 0.91F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(55, 77).addBox(0.0F, -1.29F, -2.09F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.299F)),
                PartPose.offsetAndRotation(0.0F, tY, tZ, 0.0873F, 0.0F, 0.0F));

        visor.addOrReplaceChild("tube_right", CubeListBuilder.create()
                .texOffs(0, 0).addBox(1.962F, -1.29F, -2.91F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.499F))
                .texOffs(18, 44).addBox(1.962F, -1.29F, -2.49F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(12, 44).addBox(1.962F, -1.29F, -1.49F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(38, 17).addBox(1.962F, -1.29F, 0.51F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.199F))
                .texOffs(76, 75).addBox(1.962F, -1.29F, -2.48F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.299F)),
                PartPose.offsetAndRotation(0.0F, tY, tZ, 0.088F, -0.1304F, -0.0115F));

        // Mounts
        visor.addOrReplaceChild("mount_lower", CubeListBuilder.create()
                .texOffs(54, 58).addBox(-1.0F, -0.65F, -3.42F, 2.0F, 2.0F, 4.0F, new CubeDeformation(-0.599F)),
                PartPose.offsetAndRotation(0.0F, -1.8F, 1.971F, 1.003F, 0.0F, 0.0F));

        visor.addOrReplaceChild("mount_upper", CubeListBuilder.create()
                .texOffs(62, 49).addBox(-2.0F, -0.8F, -2.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(-0.599F)),
                PartPose.offsetAndRotation(0.0F, -3.3F, 2.709F, 0.2618F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.head_bone.copyFrom(this.head);
        this.head.visible = false;
        this.hat.visible = false;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        head_bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}