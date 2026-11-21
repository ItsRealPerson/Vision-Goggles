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
            renderAssemblyConstruction(station, partialTick, poseStack, buffer, light);
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

    private void renderAssemblyConstruction(
            ModificationStationBlockEntity station,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light
    ) {
        ItemStack result = station.getAssemblingResult();
        List<ItemStack> parts = station.getAssemblingParts();
        float rawProgress = 1.0f - ((float) station.getAssemblingTicks() - partialTick)
                / (float) ModificationStationBlockEntity.ASSEMBLY_DURATION_TICKS;
        float p = Mth.clamp(rawProgress, 0.0f, 1.0f);

        if (parts.isEmpty()) {
            float hover = (float) Math.sin(p * Math.PI) * 0.045f;
            if (result.getItem() instanceof VisionGogglesItem) {
                renderUprightGoggles(station, result, 0.0f, 1.02f + hover, 0.02f, 0.44f, (float) Math.sin(p * Math.PI * 2.0f) * 8.0f, poseStack, buffer, light);
            } else {
                renderFlatItem(station, result, 0.0f, 0.955f + hover, 0.02f, 0.42f, 90.0f, poseStack, buffer, light);
            }
            return;
        }

        // Compute starting mat positions matching renderOrderedStagedItems
        java.util.List<ItemStack> flatList = new java.util.ArrayList<>();
        java.util.List<float[]> flatStarts = new java.util.ArrayList<>();
        ItemStack baseGoggles = ItemStack.EMPTY;

        java.util.Map<net.minecraft.world.item.Item, java.util.List<ItemStack>> flatGroups = new java.util.LinkedHashMap<>();
        for (ItemStack s : parts) {
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof VisionGogglesItem && baseGoggles.isEmpty()) {
                baseGoggles = s;
            } else {
                flatGroups.computeIfAbsent(s.getItem(), k -> new java.util.ArrayList<>()).add(s);
            }
        }

        boolean hasGoggles = !baseGoggles.isEmpty();
        int groupCount = flatGroups.size();
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
                flatList.add(group.get(stackIdx));
                flatStarts.add(new float[]{x, 0.952f + (stackIdx * 0.014f), z});
            }
            groupIdx++;
        }

        // Choose base part index from flatList if no goggles were staged
        int baseFlatIdx = -1;
        if (!hasGoggles && !flatList.isEmpty()) {
            baseFlatIdx = 0;
            for (int i = 0; i < flatList.size(); i++) {
                String key = flatList.get(i).getItem().getDescriptionId();
                if (key.contains("frame") || key.contains("chassis")) {
                    baseFlatIdx = i;
                    break;
                }
            }
        }

        // Collect the remaining fly-in parts
        java.util.List<ItemStack> flyParts = new java.util.ArrayList<>();
        java.util.List<float[]> flyStarts = new java.util.ArrayList<>();
        for (int i = 0; i < flatList.size(); i++) {
            if (i != baseFlatIdx) {
                flyParts.add(flatList.get(i));
                flyStarts.add(flatStarts.get(i));
            }
        }

        // Stage A (0.00..0.25): Base chassis rises to center
        if (p < 0.25f) {
            float t = smoothstep(p / 0.25f);
            if (hasGoggles) {
                float y = Mth.lerp(t, 1.00f, 1.03f);
                float z = Mth.lerp(t, -0.07f, 0.02f);
                float sc = Mth.lerp(t, 0.36f, 0.38f);
                renderUprightGoggles(station, baseGoggles, 0.0f, y, z, sc, 0.0f, poseStack, buffer, light);
            } else if (baseFlatIdx >= 0) {
                float[] st = flatStarts.get(baseFlatIdx);
                float x = Mth.lerp(t, st[0], 0.0f);
                float y = Mth.lerp(t, st[1], 1.03f) + (float) Math.sin(t * Math.PI) * 0.03f;
                float z = Mth.lerp(t, st[2], 0.02f);
                float pitch = Mth.lerp(t, 90.0f, 0.0f);
                float sc = Mth.lerp(t, 0.21f, 0.30f);
                renderFlatItem(station, flatList.get(baseFlatIdx), x, y, z, sc, pitch, poseStack, buffer, light);
            }
            // Other parts lift slightly to ready position
            for (int i = 0; i < flyParts.size(); i++) {
                float[] st = flyStarts.get(i);
                float y = Mth.lerp(t, st[1], 0.99f);
                renderFlatItem(station, flyParts.get(i), st[0], y, st[2], 0.21f, 90.0f, poseStack, buffer, light);
            }
            return;
        }

        // Stage B & C (0.25..1.00): Sequential snap-in into central result + final lock
        float buildFrac = Mth.clamp((p - 0.25f) / 0.57f, 0.0f, 1.0f);
        float snapJolt = 0.0f;
        int flyCount = flyParts.size();

        for (int i = 0; i < flyCount; i++) {
            float winStart = 0.25f + (0.57f * i) / Math.max(1, flyCount);
            float winEnd = 0.25f + (0.57f * (i + 1)) / Math.max(1, flyCount);
            if (p < winStart) {
                float[] st = flyStarts.get(i);
                float bob = (float) Math.sin((p * 18.0f) + i) * 0.006f;
                renderFlatItem(station, flyParts.get(i), st[0], 0.99f + bob, st[2], 0.21f, 90.0f, poseStack, buffer, light);
            } else if (p < winEnd) {
                float localT = smoothstep((p - winStart) / Math.max(0.001f, winEnd - winStart));
                float[] st = flyStarts.get(i);
                String desc = flyParts.get(i).getItem().getDescriptionId();
                float targetX = (desc.contains("battery") || desc.contains("expansion")) ? (i % 2 == 0 ? -0.05f : 0.05f) : 0.0f;
                float targetZ = (desc.contains("lens") || desc.contains("module")) ? 0.06f : 0.02f;

                float x = Mth.lerp(localT, st[0], targetX);
                float y = Mth.lerp(localT, 0.99f, 1.03f) + (float) Math.sin(localT * Math.PI) * 0.065f;
                float z = Mth.lerp(localT, st[2], targetZ);
                float pitch = Mth.lerp(localT, 90.0f, 0.0f);
                float sc = Mth.lerp(localT, 0.21f, 0.07f);
                renderFlatItem(station, flyParts.get(i), x, y, z, sc, pitch, poseStack, buffer, light);
                if (localT > 0.85f) {
                    snapJolt = Math.max(snapJolt, (1.0f - (localT - 0.85f) / 0.15f) * 0.012f);
                }
            }
        }

        float settleT = (p > 0.82f) ? smoothstep((p - 0.82f) / 0.18f) : 0.0f;
        float yawSettle = (p > 0.82f) ? (1.0f - settleT) * 14.0f : (float) Math.sin(buildFrac * Math.PI * 3.0f) * 4.0f;
        float centerY = 1.025f + snapJolt + ((p > 0.82f) ? (float) Math.sin(settleT * Math.PI) * 0.025f : 0.0f);

        if (result.getItem() instanceof VisionGogglesItem) {
            float sc = Mth.lerp(buildFrac, 0.30f, 0.44f);
            renderUprightGoggles(station, result, 0.0f, centerY, 0.02f, sc, yawSettle, poseStack, buffer, light);
        } else {
            float sc = Mth.lerp(buildFrac, 0.28f, 0.42f);
            float pitch = (p > 0.82f) ? Mth.lerp(settleT, 20.0f, 90.0f) : 20.0f;
            renderFlatItem(station, result, 0.0f, centerY - 0.03f, 0.02f, sc, pitch, poseStack, buffer, light);
        }
    }

    private static float smoothstep(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
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
