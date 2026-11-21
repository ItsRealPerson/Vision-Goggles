package dev.itsrealperson.vision_goggles.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.itsrealperson.vision_goggles.block.ModificationStationBlock;
import dev.itsrealperson.vision_goggles.block.entity.ModificationStationBlockEntity;
import dev.itsrealperson.vision_goggles.client.StationWorkAnimator;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders goggles, modules, and blueprint assembly in real-time 3D on the
 * Modification Station's green cutting mat.
 */
public class ModificationStationRenderer implements BlockEntityRenderer<ModificationStationBlockEntity> {

    private final ItemRenderer itemRenderer;

    public ModificationStationRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            ModificationStationBlockEntity station,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (station.getLevel() == null) return;

        int light = LevelRenderer.getLightColor(station.getLevel(), station.getBlockPos().above());
        Direction facing = station.getBlockState().hasProperty(ModificationStationBlock.FACING)
                ? station.getBlockState().getValue(ModificationStationBlock.FACING)
                : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        if (station.isAssembling()) {
            if (station.getAssemblingPlayerUuid() != null) {
                StationWorkAnimator.markEntityWorking(station.getAssemblingPlayerUuid());
            }
            ItemStack assembling = station.getAssemblingResult();
            float rawProgress = 1.0f - ((float) station.getAssemblingTicks() - partialTick)
                    / (float) ModificationStationBlockEntity.ASSEMBLY_DURATION_TICKS;
            float p = Mth.clamp(rawProgress, 0.0f, 1.0f);
            float hover = (float) Math.sin(p * Math.PI) * 0.045f;
            float baseScale = (assembling.getItem() instanceof VisionGogglesItem) ? 0.54f : 0.44f;
            float scale = baseScale * Mth.lerp(Math.min(1.0f, p * 2.2f), 0.65f, 1.0f);

            poseStack.pushPose();
            poseStack.translate(0.0f, 0.965f + hover, 0.04f);
            poseStack.mulPose(Axis.XP.rotationDegrees(72.0f - (float) Math.sin(p * Math.PI * 2.0f) * 6.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(p * Math.PI * 4.0f) * 4.0f));
            poseStack.scale(scale, scale, scale);
            itemRenderer.renderStatic(assembling, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, station.getLevel(), 0);
            poseStack.popPose();
        } else {
            ItemStack slot0 = station.getItem(0);
            ItemStack slot1 = station.getItem(1);
            ItemStack slot2 = station.getItem(2);

            if (!slot2.isEmpty() && slot2.getItem() instanceof VisionGogglesItem) {
                renderMatItem(station, slot2, 0.0f, 0.962f, 0.04f, 0.52f, 76.0f, poseStack, buffer, light);
            } else {
                boolean hasBoth = !slot0.isEmpty() && !slot1.isEmpty();
                if (!slot0.isEmpty()) {
                    float scale0 = (slot0.getItem() instanceof VisionGogglesItem) ? 0.50f : 0.36f;
                    float x0 = hasBoth ? -0.13f : 0.0f;
                    renderMatItem(station, slot0, x0, 0.960f, 0.04f, scale0, 82.0f, poseStack, buffer, light);
                }
                if (!slot1.isEmpty()) {
                    float scale1 = (slot1.getItem() instanceof VisionGogglesItem) ? 0.48f : 0.32f;
                    float x1 = hasBoth ? 0.19f : 0.0f;
                    renderMatItem(station, slot1, x1, 0.956f, 0.02f, scale1, 88.0f, poseStack, buffer, light);
                }
            }
        }

        poseStack.popPose();
    }

    private void renderMatItem(
            ModificationStationBlockEntity station,
            ItemStack stack,
            float x,
            float y,
            float z,
            float scale,
            float pitchDeg,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, station.getLevel(), 0);
        poseStack.popPose();
    }
}
