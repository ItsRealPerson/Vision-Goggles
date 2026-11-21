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

    private static final float[][] GRID_2X2 = {
            {-0.11f, -0.09f},
            { 0.11f, -0.09f},
            {-0.11f,  0.06f},
            { 0.11f,  0.06f}
    };

    private static final float[][] GRID_3X2 = {
            {-0.14f, -0.09f},
            { 0.00f, -0.09f},
            { 0.14f, -0.09f},
            {-0.14f,  0.06f},
            { 0.00f,  0.06f},
            { 0.14f,  0.06f}
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
                renderOrderedStagedItems(station, staged, poseStack, buffer, light);
            } else {
                // Modify / Repair tab slots preview on the mat
                ItemStack slot0 = station.getItem(0);
                ItemStack slot1 = station.getItem(1);
                ItemStack slot2 = station.getItem(2);

                if (!slot2.isEmpty() && slot2.getItem() instanceof VisionGogglesItem) {
                    renderUprightGoggles(station, slot2, 0.0f, 1.01f, 0.02f, 0.44f, 0.0f, poseStack, buffer, light);
                } else {
                    boolean bothGoggles = !slot0.isEmpty() && !slot1.isEmpty()
                            && slot0.getItem() instanceof VisionGogglesItem
                            && slot1.getItem() instanceof VisionGogglesItem;
                    if (!slot0.isEmpty()) {
                        if (slot0.getItem() instanceof VisionGogglesItem) {
                            renderUprightGoggles(station, slot0, bothGoggles ? -0.12f : -0.04f, 1.01f, 0.02f, bothGoggles ? 0.34f : 0.42f, 0.0f, poseStack, buffer, light);
                        } else {
                            renderFlatItem(station, slot0, -0.12f, 0.952f, 0.00f, 0.24f, 90.0f, poseStack, buffer, light);
                        }
                    }
                    if (!slot1.isEmpty()) {
                        if (slot1.getItem() instanceof VisionGogglesItem) {
                            renderUprightGoggles(station, slot1, bothGoggles ? 0.12f : 0.08f, 1.01f, 0.02f, bothGoggles ? 0.34f : 0.40f, 0.0f, poseStack, buffer, light);
                        } else {
                            renderFlatItem(station, slot1, 0.14f, 0.952f, 0.00f, 0.22f, 90.0f, poseStack, buffer, light);
                        }
                    }
                }
            }
        }

        poseStack.popPose();
    }

    private void renderOrderedStagedItems(
            ModificationStationBlockEntity station,
            List<ItemStack> staged,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light
    ) {
        ItemStack uprightGoggles = ItemStack.EMPTY;
        java.util.Map<net.minecraft.world.item.Item, java.util.List<ItemStack>> flatGroups = new java.util.LinkedHashMap<>();

        for (ItemStack s : staged) {
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof VisionGogglesItem && uprightGoggles.isEmpty()) {
                uprightGoggles = s;
            } else {
                flatGroups.computeIfAbsent(s.getItem(), k -> new java.util.ArrayList<>()).add(s);
            }
        }

        boolean hasGoggles = !uprightGoggles.isEmpty();
        if (hasGoggles) {
            renderUprightGoggles(station, uprightGoggles, 0.0f, 1.00f, -0.07f, 0.36f, 0.0f, poseStack, buffer, light);
        }

        int groupCount = flatGroups.size();
        if (groupCount == 0) return;

        int groupIdx = 0;
        for (java.util.List<ItemStack> group : flatGroups.values()) {
            float x;
            float z;
            if (hasGoggles) {
                float step = (groupCount == 1) ? 0.0f : 0.12f;
                float startX = -step * (groupCount - 1) * 0.5f;
                x = startX + groupIdx * step;
                z = 0.08f;
            } else {
                float[][] grid = (groupCount <= 4) ? GRID_2X2 : GRID_3X2;
                float[] pos = grid[groupIdx % grid.length];
                x = pos[0];
                z = pos[1];
            }

            for (int stackIdx = 0; stackIdx < group.size(); stackIdx++) {
                float y = 0.952f + (stackIdx * 0.014f);
                renderFlatItem(station, group.get(stackIdx), x, y, z, 0.21f, 90.0f, poseStack, buffer, light);
            }
            groupIdx++;
        }
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
