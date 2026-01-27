package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class VisionGoggleModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(Vision_goggles.MOD_ID, "exo_helmet"), "main");
    public final ModelPart head_bone;

    public VisionGoggleModel(ModelPart root) {
        super(root);
        this.head_bone = root.getChild("head_bone");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 1. HEAD BONE (Helmet Shell)
        PartDefinition head_bone = partdefinition.addOrReplaceChild("head_bone", CubeListBuilder.create()
                .texOffs(74, 26).addBox(-3.0F, -9.5F, -6.4F, 6.0F, 4.0F, 3.0F, new CubeDeformation(-0.6F))
                .texOffs(26, 49).addBox(-5.0F, -9.7F, -5.0F, 2.0F, 3.5F, 10.0F, new CubeDeformation(-0.4F))
                .texOffs(48, 45).addBox(3.0F, -9.7F, -5.0F, 2.0F, 3.5F, 10.0F, new CubeDeformation(-0.4F))
                .texOffs(38, 114).addBox(-6.0F, -7.0F, -6.4F, 12.0F, 1.0F, 13.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Detalle lateral (Parte superior rotada)
        head_bone.addOrReplaceChild("head_detail", CubeListBuilder.create()
                .texOffs(50, 47).addBox(-1.0F, -0.6F, -4.7F, 2.0F, 1.6F, 9.3F, new CubeDeformation(-0.4F)),
                PartPose.offsetAndRotation(0.0F, -9.9F, 0.1F, 0.0F, 1.5708F, 0.0F));

        // 2. VISOR (Main Anchor)
        PartDefinition visor = head_bone.addOrReplaceChild("visor", CubeListBuilder.create()
                .texOffs(24, 12).addBox(-4.0F, -3.0389F, -1.6994F, 8.0F, 2.0F, 3.0F, new CubeDeformation(-0.5F)),
                PartPose.offset(0.0F, -2.3355F, -7.7094F));

        // 3. PIEZAS HIJAS
        // Rota Top
        visor.addOrReplaceChild("rota_top", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-2.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F, new CubeDeformation(-0.5F)),
                PartPose.offsetAndRotation(0.0F, -3.1273F, -0.4844F, 0.3054F, 0.0F, 0.0F));

        // Tubos
        float tY = -1.3819F; 
        float tZ = -0.7191F;

        visor.addOrReplaceChild("tube_left", CubeListBuilder.create()
                .texOffs(0, 79).addBox(-3.9625F, -0.7112F, -2.4819F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.3F))
                .texOffs(64, 13).addBox(-3.9625F, -0.7112F, 0.5181F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(36, 62).addBox(-3.9625F, -0.7112F, -1.4819F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(60, 10).addBox(-3.9625F, -0.7112F, -2.4819F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(0, 16).addBox(-3.9625F, -0.7112F, -2.8993F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.5F)),
                PartPose.offsetAndRotation(0.0F, tY, tZ, 0.088F, 0.1304F, 0.0115F));

        visor.addOrReplaceChild("tube_center", CubeListBuilder.create()
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

        visor.addOrReplaceChild("tube_right", CubeListBuilder.create()
                .texOffs(0, 0).addBox(1.9625F, -0.7112F, -2.8993F, 2.0F, 2.0F, 2.0F, new CubeDeformation(-0.5F))
                .texOffs(18, 44).addBox(1.9625F, -0.7112F, -2.4819F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(12, 44).addBox(1.9625F, -0.7112F, -1.4819F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(38, 17).addBox(1.9625F, -0.7112F, 0.5181F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.2F))
                .texOffs(76, 75).addBox(1.9625F, -0.7112F, -2.4819F, 2.0F, 2.0F, 5.0F, new CubeDeformation(-0.3F)),
                PartPose.offsetAndRotation(0.0F, tY, tZ, 0.088F, -0.1304F, -0.0115F));

        // Mounts
        visor.addOrReplaceChild("mount_lower", CubeListBuilder.create()
                .texOffs(54, 58).addBox(-1.0F, -1.3472F, -3.4137F, 2.0F, 2.0F, 4.0F, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -4.7342F, 1.9708F, 1.0036F, 0.0F, 0.0F));

        visor.addOrReplaceChild("mount_upper", CubeListBuilder.create()
                .texOffs(62, 49).addBox(-2.0F, -1.5F, -2.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(-0.6F)),
                PartPose.offsetAndRotation(0.0F, -5.3645F, 2.7094F, 0.2618F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.head_bone.copyFrom(this.head);
        this.head.visible = false;
        this.hat.visible = false;

        // Flip-Up Logic
        ModelPart visor = this.head_bone.getChild("visor");
        visor.xRot = 0.0F; // Reset rotation

        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            net.minecraft.world.item.ItemStack helmet = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(player);
            if (!helmet.isEmpty() && helmet.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem) {
                boolean isActive = helmet.getOrCreateTag().getBoolean(ModConstants.TAG_ACTIVE);
                
                if (!isActive) {
                    // Flip UP (approx -90 degrees)
                    visor.xRot = -1.57F; 
                }
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        head_bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}