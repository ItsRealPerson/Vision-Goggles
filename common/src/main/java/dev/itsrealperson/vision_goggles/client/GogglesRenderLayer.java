package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GogglesRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private VisionGoggleModel<T> model;

    private static final ResourceLocation NVG_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/nvg_texture.png");
    private static final ResourceLocation NVG_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/nvg_glow.png");

    private static final ResourceLocation THERMAL_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/thermal_vision.png");
    private static final ResourceLocation THERMAL_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/thermal_glow.png");

    private static final ResourceLocation HYDRO_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/hidro_vision.png");
    private static final ResourceLocation HYDRO_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/hidro_glow.png");

    private static final ResourceLocation BIO_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/bio_vision.png");
    private static final ResourceLocation BIO_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/bio_glow.png");

    public GogglesRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack matrixStack, MultiBufferSource buffer, int packedLight, T livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(livingEntity instanceof Player player)) return;

        ItemStack stack = PlatformMethods.getEquippedHelmet(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof VisionGogglesItem goggles)) return;

        if (this.model == null) {
            this.model = new VisionGoggleModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(VisionGoggleModel.LAYER_LOCATION));
        }

        matrixStack.pushPose();

        if (this.getParentModel() instanceof HumanoidModel<?> baseModel) {
            baseModel.head.translateAndRotate(matrixStack);
        }

        this.model.head_bone.xRot = 0;
        this.model.head_bone.yRot = 0;
        this.model.head_bone.zRot = 0;

        ResourceLocation baseTex = NVG_TEXTURE;
        ResourceLocation glowTex = NVG_GLOW;

        VisionMode mode = null;

        if (goggles instanceof ModularGogglesItem modular) {
            List<VisionMode> modes = modular.getModes(stack);
            if (!modes.isEmpty()) {
                // Priority: Thermal > Bio > Hydro > NVG
                for (VisionMode m : modes) {
                    if (m == VisionMode.THERMAL) { mode = m; break; }
                    if (m == VisionMode.BIOMETRIC) { mode = m; break; }
                    if (m == VisionMode.HYDRO) { mode = m; }
                }
                if (mode == null) mode = modes.get(0);
            }
        } else {
            mode = goggles.getVisionMode();
        }

        if (mode == VisionMode.THERMAL) {
            baseTex = THERMAL_TEXTURE;
            glowTex = THERMAL_GLOW;
        } else if (mode == VisionMode.HYDRO) {
            baseTex = HYDRO_TEXTURE;
            glowTex = HYDRO_GLOW;
        } else if (mode == VisionMode.BIOMETRIC) {
            baseTex = BIO_TEXTURE;
            glowTex = BIO_GLOW;
        }

        VertexConsumer baseConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(baseTex));
        this.model.head_bone.render(matrixStack, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        VertexConsumer glowConsumer = buffer.getBuffer(RenderType.eyes(glowTex));
        this.model.head_bone.render(matrixStack, glowConsumer, 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        matrixStack.popPose();
    }
}
