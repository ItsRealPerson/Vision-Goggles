package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;

public class VisionWorldRenderer {
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");
    private static final List<BlockPos> HOT_BLOCKS = new ArrayList<>();
    private static final List<BlockPos> SPAWN_BLOCKS = new ArrayList<>();
    private static int scanTick = 0;
    private static boolean spawnSecurityEnabled = true;

    public static void render(PoseStack poseStack, Camera camera, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        if (helmet.isEmpty()) return;

        CompoundTag nbt = helmet.getTag();
        if (nbt == null || !nbt.getBoolean(ModConstants.TAG_ACTIVE)) return;

        boolean isThermal = nbt.getInt(ModConstants.TAG_MODE) == 1;
        boolean hasSpawnSecurity = false;
        boolean hasChunkViewer = false;

        if (helmet.getItem() instanceof ModularGogglesItem modular) {
            hasSpawnSecurity = modular.hasModule(helmet, ModConstants.ID_SPAWN_SECURITY);
            hasChunkViewer = modular.hasModule(helmet, ModConstants.ID_CHUNK_VIEWER);
        }

        while (ModKeyMappings.toggleSpawnSecurityKey.consumeClick()) {
            if (hasSpawnSecurity) {
                spawnSecurityEnabled = !spawnSecurityEnabled;
            }
        }

        if (!isThermal && (!hasSpawnSecurity || !spawnSecurityEnabled) && !hasChunkViewer) return;

        if (scanTick++ % 20 == 0) {
            if (isThermal) updateHotBlocks(mc);
            if (hasSpawnSecurity && spawnSecurityEnabled) updateSpawnBlocks(mc);
        }

        Vec3 cameraPos = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        if (isThermal && !HOT_BLOCKS.isEmpty()) {
            RenderSystem.disableDepthTest(); // Thermal cubes should be seen through walls
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (BlockPos pos : HOT_BLOCKS) {
                poseStack.pushPose();
                poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
                
                float s = 1.005f;
                poseStack.translate((1-s)/2, (1-s)/2, (1-s)/2);
                poseStack.scale(s, s, s);
                
                renderCube(poseStack.last().pose(), builder, 1.0f, 0.5f, 0.0f, 0.4f);
                poseStack.popPose();
            }
            tesselator.end();
            RenderSystem.enableDepthTest();
        }

        if (hasSpawnSecurity && spawnSecurityEnabled && !SPAWN_BLOCKS.isEmpty()) {
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (BlockPos pos : SPAWN_BLOCKS) {
                poseStack.pushPose();
                poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y + 0.01, pos.getZ() - cameraPos.z);
                // Usamos amarillo (1.0, 1.0, 0.0) para que tenga mayor luminancia y sea visible con NVG
                renderFlatSquare(poseStack.last().pose(), builder, 1.0f, 1.0f, 0.0f, 0.5f);
                poseStack.popPose();
            }
            tesselator.end();
        }



        if (hasChunkViewer) {
            RenderSystem.disableDepthTest();
            builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            net.minecraft.world.level.ChunkPos center = mc.player.chunkPosition();
            int rChunk = 1;
            for (int cx = center.x - rChunk; cx <= center.x + rChunk; cx++) {
                for (int cz = center.z - rChunk; cz <= center.z + rChunk; cz++) {
                    if (mc.level.getChunkSource().hasChunk(cx, cz)) {
                        float r = cx == center.x && cz == center.z ? 0.0f : 1.0f;
                        float g = cx == center.x && cz == center.z ? 1.0f : 0.5f;
                        float b = cx == center.x && cz == center.z ? 0.0f : 0.0f;
                        
                        float startX = (float)(cx * 16 - cameraPos.x);
                        float startZ = (float)(cz * 16 - cameraPos.z);
                        float endX = startX + 16;
                        float endZ = startZ + 16;
                        
                        // Pilares verticales fijos desde el fondo del mundo hasta el cielo
                        float minY = -64f - (float)cameraPos.y;
                        float maxY = 320f - (float)cameraPos.y;
                        
                        renderLine(poseStack.last().pose(), builder, startX, minY, startZ, startX, maxY, startZ, r, g, b, 0.8f);
                        renderLine(poseStack.last().pose(), builder, endX, minY, startZ, endX, maxY, startZ, r, g, b, 0.8f);
                        renderLine(poseStack.last().pose(), builder, startX, minY, endZ, startX, maxY, endZ, r, g, b, 0.8f);
                        renderLine(poseStack.last().pose(), builder, endX, minY, endZ, endX, maxY, endZ, r, g, b, 0.8f);
                        
                        // Anillos horizontales fijos cada 16 bloques (como F3+G)
                        for (int hY = -64; hY <= 320; hY += 16) {
                            float drawY = hY - (float)cameraPos.y;
                            renderLine(poseStack.last().pose(), builder, startX, drawY, startZ, endX, drawY, startZ, r, g, b, 0.5f);
                            renderLine(poseStack.last().pose(), builder, startX, drawY, endZ, endX, drawY, endZ, r, g, b, 0.5f);
                            renderLine(poseStack.last().pose(), builder, startX, drawY, startZ, startX, drawY, endZ, r, g, b, 0.5f);
                            renderLine(poseStack.last().pose(), builder, endX, drawY, startZ, endX, drawY, endZ, r, g, b, 0.5f);
                        }
                    }
                }
            }
            tesselator.end();
            RenderSystem.enableDepthTest();
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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

    private static void updateSpawnBlocks(Minecraft mc) {
        SPAWN_BLOCKS.clear();
        BlockPos p = mc.player.blockPosition();
        int r = 32;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = p.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.isRedstoneConductor(mc.level, pos) && state.isSolidRender(mc.level, pos)) {
                        BlockPos above = pos.above();
                        BlockState stateAbove = mc.level.getBlockState(above);
                        if (stateAbove.getCollisionShape(mc.level, above).isEmpty()) {
                            if (mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, above) == 0) {
                                SPAWN_BLOCKS.add(above);
                            }
                        }
                    }
                }
            }
        }
    }



    private static void renderFlatSquare(Matrix4f matrix, VertexConsumer builder, float r, float g, float b, float a) {
        float min = 0.0f;
        float max = 1.0f;
        addV(matrix, builder, min, 0, min, r, g, b, a);
        addV(matrix, builder, min, 0, max, r, g, b, a);
        addV(matrix, builder, max, 0, max, r, g, b, a);
        addV(matrix, builder, max, 0, min, r, g, b, a);
    }

    private static void renderCube(Matrix4f matrix, VertexConsumer builder, float r, float g, float b, float a) {
        addV(matrix, builder, 0,0,0, r,g,b,a); addV(matrix, builder, 0,1,0, r,g,b,a); addV(matrix, builder, 1,1,0, r,g,b,a); addV(matrix, builder, 1,0,0, r,g,b,a);
        addV(matrix, builder, 0,0,1, r,g,b,a); addV(matrix, builder, 1,0,1, r,g,b,a); addV(matrix, builder, 1,1,1, r,g,b,a); addV(matrix, builder, 0,1,1, r,g,b,a);
        addV(matrix, builder, 0,0,0, r,g,b,a); addV(matrix, builder, 0,0,1, r,g,b,a); addV(matrix, builder, 0,1,1, r,g,b,a); addV(matrix, builder, 0,1,0, r,g,b,a);
        addV(matrix, builder, 1,0,0, r,g,b,a); addV(matrix, builder, 1,1,0, r,g,b,a); addV(matrix, builder, 1,1,1, r,g,b,a); addV(matrix, builder, 1,0,1, r,g,b,a);
        addV(matrix, builder, 0,1,0, r,g,b,a); addV(matrix, builder, 0,1,1, r,g,b,a); addV(matrix, builder, 1,1,1, r,g,b,a); addV(matrix, builder, 1,1,0, r,g,b,a);
        addV(matrix, builder, 0,0,0, r,g,b,a); addV(matrix, builder, 1,0,0, r,g,b,a); addV(matrix, builder, 1,0,1, r,g,b,a); addV(matrix, builder, 0,0,1, r,g,b,a);
    }

    private static void renderLine(Matrix4f m, VertexConsumer b, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float bl, float a) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a).endVertex();
        b.vertex(m, x2, y2, z2).color(r, g, bl, a).endVertex();
    }

    private static void addV(Matrix4f m, VertexConsumer b, float x, float y, float z, float r, float g, float bl, float a) {
        b.vertex(m, x, y, z).color(r, g, bl, a).endVertex();
    }
}