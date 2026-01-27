package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.registry.ModDataComponents;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VisionWorldRenderer {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
    private static final List<BlockPos> HOT_BLOCKS = new ArrayList<>();
    private static int scanTick = 0;

    public static void render(PoseStack poseStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        if (helmet.isEmpty()) return;

        boolean isActive = Objects.requireNonNullElse(helmet.get(ModDataComponents.ACTIVE.get()), false);
        int mode = Objects.requireNonNullElse(helmet.get(ModDataComponents.MODE.get()), 0);
        if (!isActive || mode != 1) return;

        if (scanTick++ % 20 == 0) updateHotBlocks(mc);
        if (HOT_BLOCKS.isEmpty()) return;

        Vec3 cameraPos = camera.getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer blockConsumer = bufferSource.getBuffer(RenderType.eyes(WHITE));

        for (BlockPos pos : HOT_BLOCKS) {
            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            
            float s = 1.005f;
            poseStack.translate((1-s)/2, (1-s)/2, (1-s)/2);
            poseStack.scale(s, s, s);
            
            renderCube(poseStack.last().pose(), blockConsumer, 0.0f, 1.0f, 1.0f, 1.0f);
            poseStack.popPose();
        }
    }

    private static void updateHotBlocks(Minecraft mc) {
        HOT_BLOCKS.clear();
        BlockPos p = mc.player.blockPosition();
        int r = 12;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = p.offset(x, y, z);
                    BlockState s = mc.level.getBlockState(pos);
                    if (s.is(Blocks.LAVA) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) {
                        HOT_BLOCKS.add(pos);
                    }
                }
            }
        }
    }

    private static void renderCube(Matrix4f matrix, VertexConsumer builder, float r, float g, float b, float a) {
        addV(matrix, builder, 0,0,0, r,g,b,a, 0,0,-1); addV(matrix, builder, 0,1,0, r,g,b,a, 0,0,-1); addV(matrix, builder, 1,1,0, r,g,b,a, 0,0,-1); addV(matrix, builder, 1,0,0, r,g,b,a, 0,0,-1);
        addV(matrix, builder, 0,0,1, r,g,b,a, 0,0,1); addV(matrix, builder, 1,0,1, r,g,b,a, 0,0,1); addV(matrix, builder, 1,1,1, r,g,b,a, 0,0,1); addV(matrix, builder, 0,1,1, r,g,b,a, 0,0,1);
        addV(matrix, builder, 0,0,0, r,g,b,a, -1,0,0); addV(matrix, builder, 0,0,1, r,g,b,a, -1,0,0); addV(matrix, builder, 0,1,1, r,g,b,a, -1,0,0); addV(matrix, builder, 0,1,0, r,g,b,a, -1,0,0);
        addV(matrix, builder, 1,0,0, r,g,b,a, 1,0,0); addV(matrix, builder, 1,1,0, r,g,b,a, 1,0,0); addV(matrix, builder, 1,1,1, r,g,b,a, 1,0,0); addV(matrix, builder, 1,0,1, r,g,b,a, 1,0,0);
        addV(matrix, builder, 0,1,0, r,g,b,a, 0,1,0); addV(matrix, builder, 0,1,1, r,g,b,a, 0,1,0); addV(matrix, builder, 1,1,1, r,g,b,a, 0,1,0); addV(matrix, builder, 1,1,0, r,g,b,a, 0,1,0);
        addV(matrix, builder, 0,0,0, r,g,b,a, 0,-1,0); addV(matrix, builder, 1,0,0, r,g,b,a, 0,-1,0); addV(matrix, builder, 1,0,1, r,g,b,a, 0,-1,0); addV(matrix, builder, 0,0,1, r,g,b,a, 0,-1,0);
    }

    private static void addV(Matrix4f m, VertexConsumer b, float x, float y, float z, float r, float g, float bl, float a, float nx, float ny, float nz) {
        b.addVertex(m, x, y, z).setColor(r, g, bl, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(nx, ny, nz);
    }
}
