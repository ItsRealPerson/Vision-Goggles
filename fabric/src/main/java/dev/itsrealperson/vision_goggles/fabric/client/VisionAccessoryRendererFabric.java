package dev.itsrealperson.vision_goggles.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.client.VisionGoggleModel;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class VisionAccessoryRendererFabric implements AccessoryRenderer {
    private VisionGoggleModel<LivingEntity> model;
    
    private static final ResourceLocation NVG_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/nvg_texture.png");
    private static final ResourceLocation NVG_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/nvg_glow.png");
    
    private static final ResourceLocation THERMAL_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/thermal_vision.png");
    private static final ResourceLocation THERMAL_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/thermal_glow.png");

    @Override
    public <M extends net.minecraft.world.entity.LivingEntity> void render(net.minecraft.world.item.ItemStack stack, io.wispforest.accessories.api.slot.SlotReference reference, com.mojang.blaze3d.vertex.PoseStack matrixStack, net.minecraft.client.model.EntityModel<M> model, net.minecraft.client.renderer.MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (this.model == null) {
            this.model = new VisionGoggleModel<>(net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(VisionGoggleModel.LAYER_LOCATION));
        }

        matrixStack.pushPose();

        if (model instanceof net.minecraft.client.model.HumanoidModel) {
            ((net.minecraft.client.model.HumanoidModel<?>) model).head.translateAndRotate(matrixStack);
        }

        this.model.head_bone.xRot = 0;
        this.model.head_bone.yRot = 0;
        this.model.head_bone.zRot = 0;

        net.minecraft.resources.ResourceLocation baseTex = NVG_TEXTURE;
        net.minecraft.resources.ResourceLocation glowTex = NVG_GLOW;
        
        if (stack.getItem() == ModItems.THERMAL_GOGGLES.get()) {
            baseTex = THERMAL_TEXTURE;
            glowTex = THERMAL_GLOW;
        }

        com.mojang.blaze3d.vertex.VertexConsumer baseConsumer = renderTypeBuffer.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(baseTex));
        this.model.head_bone.render(matrixStack, baseConsumer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        com.mojang.blaze3d.vertex.VertexConsumer glowConsumer = renderTypeBuffer.getBuffer(net.minecraft.client.renderer.RenderType.eyes(glowTex));
        this.model.head_bone.render(matrixStack, glowConsumer, 15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        matrixStack.popPose();
    }
}


