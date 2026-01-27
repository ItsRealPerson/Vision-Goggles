package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;

public class HeatSilhouetteLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation BLANK = new ResourceLocation("minecraft", "textures/misc/white.png");

    public HeatSilhouetteLayer(LivingEntityRenderer<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        if (helmet.isEmpty()) return;

        CompoundTag nbt = helmet.getTag();
        if (nbt == null || !nbt.getBoolean(ModConstants.TAG_ACTIVE)) return;

        int mode = nbt.getInt(ModConstants.TAG_MODE);
        // Only render player if in third person
        boolean isSelf = (entity == mc.player);
        if (isSelf && mc.options.getCameraType().isFirstPerson()) return;

        boolean isThermal = (mode == 1 && entity.getMobType() != MobType.UNDEAD);
        boolean isSonar = VisionRenderer.isSonarActive() && (isSelf || mc.player.distanceToSqr(entity) < 625);

        if (!isThermal && !isSonar) return;

        poseStack.pushPose();

        // 1. Scale and Position Adjustments
        float scale = 1.03f;
        // Shift up 0.01 to cover the back better and avoid floor clipping
        poseStack.translate(0, 0.01, 0);
        poseStack.scale(scale, scale, scale);

        M model = this.getParentModel();
        model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
        model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 2. Render logic
        if (isThermal) {
            // Cyan for Thermal (Normal depth)
            VertexConsumer thermalConsumer = bufferSource.getBuffer(RenderType.eyes(BLANK));
            model.renderToBuffer(poseStack, thermalConsumer, 15728880, OverlayTexture.NO_OVERLAY, 0.0F, 1.0F, 1.0F, 1.0F);
        }
        
        if (isSonar) {
            // Blue for Sonar (Now respects blocks like a normal render)
            VertexConsumer sonarConsumer = bufferSource.getBuffer(RenderType.eyes(BLANK));
            model.renderToBuffer(poseStack, sonarConsumer, 15728880, OverlayTexture.NO_OVERLAY, 0.0F, 0.5F, 1.0F, 1.0F);
        }

        poseStack.popPose();
    }
}