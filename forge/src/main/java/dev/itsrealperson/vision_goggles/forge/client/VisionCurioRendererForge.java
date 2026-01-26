package dev.itsrealperson.vision_goggles.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.client.VisionGoggleModel;
import dev.itsrealperson.vision_goggles.registry.ModItems;
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
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class VisionCurioRendererForge implements ICurioRenderer {
    private VisionGoggleModel<LivingEntity> model;
    
    private static final ResourceLocation NVG_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/nvg_texture.png");
    private static final ResourceLocation NVG_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/nvg_glow.png");
    
    private static final ResourceLocation THERMAL_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/thermal_vision.png");
    private static final ResourceLocation THERMAL_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/thermal_glow.png");

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack matrixStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (this.model == null) {
            this.model = new VisionGoggleModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(VisionGoggleModel.LAYER_LOCATION));
        }

        matrixStack.pushPose();

        if (renderLayerParent.getModel() instanceof HumanoidModel<?> baseModel) {
            baseModel.head.translateAndRotate(matrixStack);
        }

        this.model.head_bone.xRot = 0;
        this.model.head_bone.yRot = 0;
        this.model.head_bone.zRot = 0;

        ResourceLocation baseTex = NVG_TEXTURE;
        ResourceLocation glowTex = NVG_GLOW;
        
        if (stack.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem goggles) {
            dev.itsrealperson.vision_goggles.util.VisionMode mode = null;

            if (goggles instanceof dev.itsrealperson.vision_goggles.item.ModularGogglesItem modular) {
                java.util.List<dev.itsrealperson.vision_goggles.util.VisionMode> modes = modular.getModes(stack);
                if (!modes.isEmpty()) {
                    for (dev.itsrealperson.vision_goggles.util.VisionMode m : modes) {
                        if (m == dev.itsrealperson.vision_goggles.util.VisionMode.THERMAL || m == dev.itsrealperson.vision_goggles.util.VisionMode.BIOMETRIC) {
                            mode = m;
                            break;
                        }
                    }
                    if (mode == null) mode = modes.get(0);
                }
            } else {
                mode = goggles.getVisionMode();
            }

            if (mode == dev.itsrealperson.vision_goggles.util.VisionMode.THERMAL || mode == dev.itsrealperson.vision_goggles.util.VisionMode.BIOMETRIC) {
                baseTex = THERMAL_TEXTURE;
                glowTex = THERMAL_GLOW;
            }
        }

        VertexConsumer baseConsumer = renderTypeBuffer.getBuffer(RenderType.entityCutoutNoCull(baseTex));
        this.model.head_bone.render(matrixStack, baseConsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        VertexConsumer glowConsumer = renderTypeBuffer.getBuffer(RenderType.eyes(glowTex));
        this.model.head_bone.render(matrixStack, glowConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        matrixStack.popPose();
    }
}


