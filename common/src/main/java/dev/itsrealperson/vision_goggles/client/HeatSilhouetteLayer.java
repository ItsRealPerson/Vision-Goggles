package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.registry.ModDataComponents;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.EntityTypeTags;

import java.util.Objects;

public class HeatSilhouetteLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation BLANK = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

    public HeatSilhouetteLayer(LivingEntityRenderer<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        if (helmet.isEmpty()) return;

        boolean isActive = Objects.requireNonNullElse(helmet.get(ModDataComponents.ACTIVE.get()), false);
        if (!isActive) return;

        int mode = Objects.requireNonNullElse(helmet.get(ModDataComponents.MODE.get()), 0);
        // Only render player if in third person
        boolean isSelf = (entity == mc.player);
        if (isSelf && mc.options.getCameraType().isFirstPerson()) return;

        boolean isThermal = (mode == 1 && entity.getType().is(EntityTypeTags.UNDEAD) == false);
        boolean isSonar = VisionRenderer.isSonarActive() && (isSelf || mc.player.distanceToSqr(entity) < 625);

        if (!isThermal && !isSonar) return;

        poseStack.pushPose();

        float scale = 1.03f;
        poseStack.translate(0, 0.01, 0);
        poseStack.scale(scale, scale, scale);

        M model = this.getParentModel();
        model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // In 1.21.1, renderToBuffer takes PoseStack, VertexConsumer, int packedLight, int packedOverlay, int color
        if (isThermal) {
            VertexConsumer thermalConsumer = bufferSource.getBuffer(RenderType.eyes(BLANK));
            model.renderToBuffer(poseStack, thermalConsumer, 15728880, OverlayTexture.NO_OVERLAY, 0xFF00FFFF); // Cyan ARGB
        }
        
        if (isSonar) {
            VertexConsumer sonarConsumer = bufferSource.getBuffer(RenderType.eyes(BLANK));
            model.renderToBuffer(poseStack, sonarConsumer, 15728880, OverlayTexture.NO_OVERLAY, 0xFF007FFF); // Light Blue ARGB
        }

        poseStack.popPose();
    }
}