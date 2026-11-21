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

import java.util.List;

/**
 * Renders staged parts, goggles, and assembly progress in real-time 3D on the
 * Modification Station's green cutting mat.
 */
public class ModificationStationRenderer implements BlockEntityRenderer<ModificationStationBlockEntity> {

    private static final float[][] MAT_OFFSETS = {
            {-0.18f, -0.10f},
            { 0.18f, -0.10f},
            {-0.18f,  0.12f},
            { 0.18f,  0.12f},
            { 0.00f, -0.16f},
            { 0.00f,  0.15f},
            {-0.08f,  0.02f},
            { 0.08f,  0.02f},
            { 0.00f,  0.00f}
    };

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

            if (assembling.getItem() instanceof VisionGogglesItem) {
                float scale = 0.44f * Mth.lerp(Math.min(1.0f, p * 2.2f), 0.70f, 1.0f);
                renderUprightGoggles(station, assembling, 0.0f, 1.02f + hover, 0.02f, scale, (float) Math.sin(p * Math.PI * 2.0f) * 8.0f, poseStack, buffer, light);
            } else {
                float scale = 0.42f * Mth.lerp(Math.min(1.0f, p * 2.2f), 0.70f, 1.0f);
                renderFlatItem(station, assembling, 0.0f, 0.955f + hover, 0.02f, scale, 90.0f, poseStack, buffer, light);
            }
        } else {
            List<ItemStack> staged = station.getStagedItems();
            if (!staged.isEmpty()) {
                int flatIdx = 0;
                for (ItemStack s : staged) {
                    if (s.isEmpty()) continue;
                    if (s.getItem() instanceof VisionGogglesItem) {
                        renderUprightGoggles(station, s, 0.0f, 1.01f, 0.02f, 0.42f, 0.0f, poseStack, buffer, light);
                    } else {
                        float[] off = MAT_OFFSETS[flatIdx % MAT_OFFSETS.length];
                        renderFlatItem(station, s, off[0], 0.946f + (flatIdx * 0.002f), off[1], 0.28f, 90.0f, poseStack, buffer, light);
                        flatIdx++;
                    }
                }
            } else {
                // Modify / Repair tab slots preview on the mat
                ItemStack slot0 = station.getItem(0);
                ItemStack slot1 = station.getItem(1);
                ItemStack slot2 = station.getItem(2);

                if (!slot2.isEmpty() && slot2.getItem() instanceof VisionGogglesItem) {
                    renderUprightGoggles(station, slot2, 0.0f, 1.01f, 0.02f, 0.44f, 0.0f, poseStack, buffer, light);
                } else {
                    if (!slot0.isEmpty()) {
                        if (slot0.getItem() instanceof VisionGogglesItem) {
                            renderUprightGoggles(station, slot0, -0.06f, 1.01f, 0.02f, 0.42f, 0.0f, poseStack, buffer, light);
                        } else {
                            renderFlatItem(station, slot0, -0.14f, 0.948f, 0.02f, 0.30f, 90.0f, poseStack, buffer, light);
                        }
                    }
                    if (!slot1.isEmpty()) {
                        if (slot1.getItem() instanceof VisionGogglesItem) {
                            renderUprightGoggles(station, slot1, 0.08f, 1.01f, 0.02f, 0.40f, 0.0f, poseStack, buffer, light);
                        } else {
                            renderFlatItem(station, slot1, 0.19f, 0.948f, 0.02f, 0.28f, 90.0f, poseStack, buffer, light);
                        }
                    }
                }
            }
        }

        poseStack.popPose();
    }

    private void renderUprightGoggles(
            ModificationStationBlockEntity station,
            ItemStack stack,
            float x,
            float y,
            float z,
            float scale,
            float yawOffset,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f + yawOffset));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, station.getLevel(), 0);
        poseStack.popPose();
    }

    private void renderFlatItem(
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
