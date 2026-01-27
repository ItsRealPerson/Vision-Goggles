package dev.itsrealperson.vision_goggles.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.client.VisionGoggleModel;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class VisionAccessoryRendererFabric implements AccessoryRenderer {
    private VisionGoggleModel<LivingEntity> model;
    
    private static final ResourceLocation NVG_TEXTURE = ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "textures/entities/nvg_texture.png");
    private static final ResourceLocation NVG_GLOW = ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "textures/entities/nvg_glow.png");
    
    private static final ResourceLocation THERMAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "textures/entities/thermal_vision.png");
    private static final ResourceLocation THERMAL_GLOW = ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "textures/entities/thermal_glow.png");

    @Override
    public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, PoseStack matrixStack, EntityModel<M> model, MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (this.model == null) {
            this.model = new VisionGoggleModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(VisionGoggleModel.LAYER_LOCATION));
        }

        matrixStack.pushPose();

        if (model instanceof HumanoidModel<?> baseModel) {
            baseModel.head.translateAndRotate(matrixStack);
        }

        this.model.head_bone.xRot = 0;
        this.model.head_bone.yRot = 0;
        this.model.head_bone.zRot = 0;

        ResourceLocation baseTex = NVG_TEXTURE;
        ResourceLocation glowTex = NVG_GLOW;
        
        if (stack.getItem() instanceof VisionGogglesItem goggles) {
            VisionMode mode = null;
            
            if (goggles instanceof ModularGogglesItem modular) {
                List<VisionMode> modes = modular.getModes(stack);
                if (!modes.isEmpty()) {
                    for (VisionMode m : modes) {
                        if (m == VisionMode.THERMAL || m == VisionMode.BIOMETRIC) {
                            mode = m;
                            break;
                        }
                    }
                    if (mode == null) mode = modes.get(0);
                }
            } else {
                mode = goggles.getVisionMode();
            }

            if (mode == VisionMode.THERMAL || mode == VisionMode.BIOMETRIC) {
                baseTex = THERMAL_TEXTURE;
                glowTex = THERMAL_GLOW;
            }
        }

        VertexConsumer baseConsumer = renderTypeBuffer.getBuffer(RenderType.entityCutoutNoCull(baseTex));
        this.model.head_bone.render(matrixStack, baseConsumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        VertexConsumer glowConsumer = renderTypeBuffer.getBuffer(RenderType.eyes(glowTex));
        this.model.head_bone.render(matrixStack, glowConsumer, 15728880, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        
        matrixStack.popPose();
    }
}