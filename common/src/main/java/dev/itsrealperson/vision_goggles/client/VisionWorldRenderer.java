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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

public class VisionWorldRenderer {
    private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");
    private static final List<BlockPos> HOT_BLOCKS = new ArrayList<>();
    private static final List<BlockPos> SPAWN_BLOCKS = new ArrayList<>();
    private static final List<BlockPos> PENDING_SPAWN_BLOCKS = new ArrayList<>();
    private static final int RADIUS_SPAWN_XZ = 24;
    private static final int RADIUS_SPAWN_Y = 8;
    private static int currentSpawnY = -RADIUS_SPAWN_Y;
    private static BlockPos spawnCenter = null;
    private static int hotBlocksTick = 0;
    private static boolean spawnSecurityEnabled = true;

    public static boolean isSpawnSecurityEnabled() {
        return spawnSecurityEnabled;
    }

    public static void toggleSpawnSecurity(Minecraft mc) {
        spawnSecurityEnabled = !spawnSecurityEnabled;
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable(spawnSecurityEnabled ? "message.vision_goggles.spawn_security_on" : "message.vision_goggles.spawn_security_off"), true);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(spawnSecurityEnabled ? ModSounds.VISOR_ON.get() : ModSounds.VISOR_OFF.get(), 1.4F));
        }
    }

    private static final List<DebugBox> debugBoxes = new ArrayList<>();

    private static class DebugBox {
        Vector3f center; Vector3f halfExtents; Quaternionf orientation;
        int entityId; String partName; int ticksLeft;
        
        DebugBox(Vector3f c, Vector3f h, Quaternionf q, int e, String p) {
            this.center = c; this.halfExtents = h; this.orientation = q;
            this.entityId = e; this.partName = p; this.ticksLeft = 40; // 2 seconds
        }
    }

    public static void addDebugBox(float cx, float cy, float cz, float hx, float hy, float hz, float qx, float qy, float qz, float qw, int entityId, String partName) {
        debugBoxes.add(new DebugBox(new Vector3f(cx, cy, cz), new Vector3f(hx, hy, hz), new Quaternionf(qx, qy, qz, qw), entityId, partName));
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Render Debug Boxes
        if (!debugBoxes.isEmpty()) {
            poseStack.pushPose();
            Vec3 camPos = camera.getPosition();
            poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

            VertexConsumer builder = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());
            
            Iterator<DebugBox> it = debugBoxes.iterator();
            while (it.hasNext()) {
                DebugBox box = it.next();
                
                poseStack.pushPose();
                poseStack.translate(box.center.x(), box.center.y(), box.center.z());
                poseStack.mulPose(box.orientation);
                
                // Draw wireframe AABB locally
                float hx = box.halfExtents.x();
                float hy = box.halfExtents.y();
                float hz = box.halfExtents.z();
                
                Matrix4f pose = poseStack.last().pose();
                org.joml.Matrix3f normal = poseStack.last().normal();
                
                // 12 lines for a box. Red=0, Green=255, Blue=0 (Neon Green)
                int r=0, g=255, b=0, a=255;
                
                // Bottom
                drawLine(builder, pose, normal, -hx, -hy, -hz, hx, -hy, -hz, r,g,b,a);
                drawLine(builder, pose, normal, hx, -hy, -hz, hx, -hy, hz, r,g,b,a);
                drawLine(builder, pose, normal, hx, -hy, hz, -hx, -hy, hz, r,g,b,a);
                drawLine(builder, pose, normal, -hx, -hy, hz, -hx, -hy, -hz, r,g,b,a);
                // Top
                drawLine(builder, pose, normal, -hx, hy, -hz, hx, hy, -hz, r,g,b,a);
                drawLine(builder, pose, normal, hx, hy, -hz, hx, hy, hz, r,g,b,a);
                drawLine(builder, pose, normal, hx, hy, hz, -hx, hy, hz, r,g,b,a);
                drawLine(builder, pose, normal, -hx, hy, hz, -hx, hy, -hz, r,g,b,a);
                // Pillars
                drawLine(builder, pose, normal, -hx, -hy, -hz, -hx, hy, -hz, r,g,b,a);
                drawLine(builder, pose, normal, hx, -hy, -hz, hx, hy, -hz, r,g,b,a);
                drawLine(builder, pose, normal, hx, -hy, hz, hx, hy, hz, r,g,b,a);
                drawLine(builder, pose, normal, -hx, -hy, hz, -hx, hy, hz, r,g,b,a);

                poseStack.popPose();
                
                box.ticksLeft--;
                if (box.ticksLeft <= 0) it.remove();
            }
            mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
            poseStack.popPose();
        }

        if (mc.player == null) return;
        
        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        if (helmet.isEmpty()) return;

        boolean isThermal = VisionRenderer.isVisionModeActive() && VisionRenderer.getCurrentVisionMode() == dev.itsrealperson.vision_goggles.util.VisionMode.THERMAL;
        boolean hasSpawnSecurity = false;

        if (helmet.getItem() instanceof ModularGogglesItem modular) {
            hasSpawnSecurity = modular.hasModule(helmet, ModConstants.ID_SPAWN_SECURITY);
        }


        if (!isThermal && (!hasSpawnSecurity || !spawnSecurityEnabled)) return;

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

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Ticked once per client tick (20 TPS) from VisionRenderer.onClientTick.
     * Slices the spawn danger scan across ticks to avoid micro-stutters on the render thread.
     */
    public static void clientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        boolean hasHelmet = !helmet.isEmpty();
        boolean isThermal = VisionRenderer.isVisionModeActive() && VisionRenderer.getCurrentVisionMode() == dev.itsrealperson.vision_goggles.util.VisionMode.THERMAL;
        boolean hasSpawnSecurity = false;

        if (hasHelmet && helmet.getItem() instanceof ModularGogglesItem modular) {
            hasSpawnSecurity = modular.hasModule(helmet, ModConstants.ID_SPAWN_SECURITY);
        }

        // Spawn security check: sliced across ticks (tactical 24x8x24 radius)
        if (hasSpawnSecurity && spawnSecurityEnabled) {
            BlockPos playerPos = mc.player.blockPosition();
            if (spawnCenter == null || playerPos.distSqr(spawnCenter) > 64.0) {
                spawnCenter = playerPos;
                currentSpawnY = -RADIUS_SPAWN_Y;
                PENDING_SPAWN_BLOCKS.clear();
            }

            int endY = Math.min(RADIUS_SPAWN_Y, currentSpawnY + 2);
            scanSpawnSlice(mc, spawnCenter, currentSpawnY, endY);
            currentSpawnY += 3;

            if (currentSpawnY > RADIUS_SPAWN_Y) {
                SPAWN_BLOCKS.clear();
                SPAWN_BLOCKS.addAll(PENDING_SPAWN_BLOCKS);
                PENDING_SPAWN_BLOCKS.clear();
                currentSpawnY = -RADIUS_SPAWN_Y;
                spawnCenter = mc.player.blockPosition();
            }
        } else {
            if (!SPAWN_BLOCKS.isEmpty()) SPAWN_BLOCKS.clear();
            if (!PENDING_SPAWN_BLOCKS.isEmpty()) PENDING_SPAWN_BLOCKS.clear();
            currentSpawnY = -RADIUS_SPAWN_Y;
            spawnCenter = null;
        }

        // Thermal hot blocks: update once per second (every 20 ticks)
        if (isThermal) {
            if (hotBlocksTick++ % 20 == 0) {
                updateHotBlocks(mc);
            }
        } else {
            if (!HOT_BLOCKS.isEmpty()) HOT_BLOCKS.clear();
            hotBlocksTick = 0;
        }
    }

    private static void scanSpawnSlice(Minecraft mc, BlockPos center, int startY, int endY) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int r = RADIUS_SPAWN_XZ;
        int rSq = r * r;
        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();

        for (int y = startY; y <= endY; y++) {
            int worldY = cy + y;
            if (worldY < mc.level.getMinBuildHeight() || worldY >= mc.level.getMaxBuildHeight() - 1) continue;

            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z <= rSq) {
                        mutPos.set(cx + x, worldY, cz + z);
                        BlockState state = mc.level.getBlockState(mutPos);
                        if (state.isRedstoneConductor(mc.level, mutPos) && state.isSolidRender(mc.level, mutPos)) {
                            abovePos.set(cx + x, worldY + 1, cz + z);
                            BlockState stateAbove = mc.level.getBlockState(abovePos);
                            if (stateAbove.getFluidState().isEmpty() && stateAbove.getCollisionShape(mc.level, abovePos).isEmpty()) {
                                if (mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, abovePos) == 0) {
                                    PENDING_SPAWN_BLOCKS.add(abovePos.immutable());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void updateHotBlocks(Minecraft mc) {
        HOT_BLOCKS.clear();
        if (mc.player == null || mc.level == null) return;
        BlockPos p = mc.player.blockPosition();
        int r = 12;
        int rSq = r * r;
        int px = p.getX(), py = p.getY(), pz = p.getZ();
        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                int worldY = py + y;
                if (worldY < mc.level.getMinBuildHeight() || worldY >= mc.level.getMaxBuildHeight()) continue;
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z <= rSq) {
                        mutPos.set(px + x, worldY, pz + z);
                        BlockState s = mc.level.getBlockState(mutPos);
                        if (s.is(Blocks.LAVA) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) {
                            HOT_BLOCKS.add(mutPos.immutable());
                        }
                    }
                }
            }
        }
    }

    public static void updateSpawnBlocks(Minecraft mc) {
        SPAWN_BLOCKS.clear();
        if (mc.player == null || mc.level == null) return;
        BlockPos p = mc.player.blockPosition();
        int cx = p.getX(), cy = p.getY(), cz = p.getZ();
        int r = RADIUS_SPAWN_XZ;
        int rSq = r * r;
        BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();

        for (int y = -RADIUS_SPAWN_Y; y <= RADIUS_SPAWN_Y; y++) {
            int worldY = cy + y;
            if (worldY < mc.level.getMinBuildHeight() || worldY >= mc.level.getMaxBuildHeight() - 1) continue;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z <= rSq) {
                        mutPos.set(cx + x, worldY, cz + z);
                        BlockState state = mc.level.getBlockState(mutPos);
                        if (state.isRedstoneConductor(mc.level, mutPos) && state.isSolidRender(mc.level, mutPos)) {
                            abovePos.set(cx + x, worldY + 1, cz + z);
                            BlockState stateAbove = mc.level.getBlockState(abovePos);
                            if (stateAbove.getFluidState().isEmpty() && stateAbove.getCollisionShape(mc.level, abovePos).isEmpty()) {
                                if (mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, abovePos) == 0) {
                                    SPAWN_BLOCKS.add(abovePos.immutable());
                                }
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
    
    private static void drawLine(VertexConsumer builder, Matrix4f pose, org.joml.Matrix3f normal, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        builder.vertex(pose, x1, y1, z1).color(r, g, b, a).normal(normal, 0, 1, 0).endVertex();
        builder.vertex(pose, x2, y2, z2).color(r, g, b, a).normal(normal, 0, 1, 0).endVertex();
    }
}