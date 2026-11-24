package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Captures living thermal entities into an offscreen mask texture ({@link #getMaskTextureId()})
 * using camera-invariant world-up gradients, world-depth pixel occlusion for direct sightlines,
 * and strict OpenGL state restoration so thermal post-processing never depends on world RGB key colors.
 */
public class ThermalEntityPainter {

    /** Blocks in this tag do NOT block thermal vision (planks, glass, leaves, etc.) */
    private static final TagKey<Block> THERMAL_TRANSPARENT = TagKey.create(
            Registries.BLOCK,
            new ResourceLocation(Vision_goggles.MOD_ID, "thermal_transparent")
    );

    // Offscreen capture FBO (with world-compatible depth attachment) & clean entity mask FBO
    private static int captureFbo = 0;
    private static int colorTexture = 0;
    private static int depthRenderbuffer = 0;
    private static int captureDepthFormat = GL14.GL_DEPTH_COMPONENT24;
    private static int captureDepthAttachment = GL30.GL_DEPTH_ATTACHMENT;

    private static int maskFbo = 0;
    private static int maskTexture = 0;
    private static int emptyMaskTexture = 0;

    private static int fboWidth = 0;
    private static int fboHeight = 0;

    private static volatile boolean maskValidThisFrame = false;
    private static boolean depthBlitSupported = true;
    private static boolean depthBlitRetried = false;

    // Shader resources
    private static int programId = 0;
    private static int vertexShaderId = 0;
    private static int fragmentShaderId = 0;

    // Fullscreen quad
    private static int quadVAO = -1;
    private static int quadVBO = -1;

    // Entity buffer
    private static BufferBuilder maskBuffer = null;
    private static MultiBufferSource.BufferSource maskBufferSource = null;

    // True no-op consumer so held items, emissive eyes, glints, capes, elytras, nametags, and leashes are discarded cleanly
    private static final VertexConsumer DISCARD_VERTEX_CONSUMER = new VertexConsumer() {
        @Override public VertexConsumer vertex(double x, double y, double z) { return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() {}
        @Override public void defaultColor(int r, int g, int b, int a) {}
        @Override public void unsetDefaultColor() {}
    };

    private static boolean initialized = false;
    private static boolean hasPerEntityCustomColor = false;

    public static boolean hasPerEntityCustomColor() {
        return hasPerEntityCustomColor;
    }

    public static void setHasPerEntityCustomColor(boolean value) {
        hasPerEntityCustomColor = value;
    }

    public static int getMaskTextureId() {
        if (maskValidThisFrame && maskTexture != 0) {
            return maskTexture;
        }
        int empty = ensureEmptyMaskTexture();
        return (empty > 0) ? empty : -1;
    }

    public static void invalidateFrameMask() {
        maskValidThisFrame = false;
    }

    public static Integer resolveEntityThermalColor(LivingEntity entity) {
        String entityTypeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        boolean isHostile = entity instanceof net.minecraft.world.entity.monster.Enemy || !entity.getType().getCategory().isFriendly();
        return dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge.hookThermalColor(entityTypeId, isHostile);
    }

    public static void init() {
        if (initialized) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            int w = Math.max(1, mc.getWindow().getWidth());
            int h = Math.max(1, mc.getWindow().getHeight());
            createFBO(w, h, mc);
            ensureEmptyMaskTexture();
            createShader();
            createQuad();
            maskBuffer = new BufferBuilder(262144);
            maskBufferSource = MultiBufferSource.immediate(maskBuffer);

            initialized = true;
            maskValidThisFrame = false;
            depthBlitSupported = true;
            depthBlitRetried = false;
        } catch (Exception e) {
            cleanup();
            Vision_goggles.LOGGER.error("[ThermalEntityPainter] Failed to initialize: " + e.getMessage());
        }
    }

    public static void cleanup() {
        if (captureFbo != 0) { GlStateManager._glDeleteFramebuffers(captureFbo); captureFbo = 0; }
        if (colorTexture != 0) { GlStateManager._deleteTexture(colorTexture); colorTexture = 0; }
        if (depthRenderbuffer != 0) { GL30.glDeleteRenderbuffers(depthRenderbuffer); depthRenderbuffer = 0; }
        if (maskFbo != 0) { GlStateManager._glDeleteFramebuffers(maskFbo); maskFbo = 0; }
        if (maskTexture != 0) { GlStateManager._deleteTexture(maskTexture); maskTexture = 0; }
        if (emptyMaskTexture != 0) { GlStateManager._deleteTexture(emptyMaskTexture); emptyMaskTexture = 0; }
        if (programId != 0) { GL20.glDeleteProgram(programId); programId = 0; }
        if (vertexShaderId != 0) { GL20.glDeleteShader(vertexShaderId); vertexShaderId = 0; }
        if (fragmentShaderId != 0) { GL20.glDeleteShader(fragmentShaderId); fragmentShaderId = 0; }
        if (quadVAO != -1) { GL30.glDeleteVertexArrays(quadVAO); GL30.glDeleteBuffers(quadVBO); quadVAO = -1; quadVBO = -1; }
        fboWidth = 0;
        fboHeight = 0;
        initialized = false;
        maskValidThisFrame = false;
        depthBlitSupported = true;
        depthBlitRetried = false;
    }

    private static class ThermalRenderEntry {
        final LivingEntity entity;
        final double dx;
        final double dy;
        final double dz;
        final float heat;
        final double distSq;

        ThermalRenderEntry(LivingEntity entity, double dx, double dy, double dz, float heat, double distSq) {
            this.entity = entity;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.heat = heat;
            this.distSq = distSq;
        }
    }

    public static boolean emitsThermalHeat(LivingEntity living) {
        if (living == null) return false;
        // Undead: no body heat
        if (living.getMobType() == MobType.UNDEAD
                || living instanceof net.minecraft.world.entity.monster.Zombie
                || living.getType().getDescriptionId().toLowerCase(Locale.ROOT).contains("zombie")) {
            return false;
        }
        // Cold-blooded / constructs / spirits
        if (living instanceof net.minecraft.world.entity.decoration.ArmorStand) return false; // Wooden construct, no heat
        if (living instanceof Slime && !(living instanceof MagmaCube)) return false; // Slime = cold, MagmaCube = hot
        if (living instanceof Spider) return false;      // Arthropod, cold-blooded
        if (living instanceof Silverfish) return false;  // Insect, cold-blooded
        if (living instanceof Endermite) return false;   // Cold-blooded parasite
        if (living instanceof Shulker) return false;     // Shell creature, no warm blood
        if (living instanceof Vex) return false;         // Spirit, no physical body
        if (living instanceof Guardian) return false;    // Fish, cold-blooded
        if (living instanceof Squid) return false;       // Cephalopod, cold-blooded
        if (living instanceof IronGolem) return false;   // Metal construct, no organic heat
        if (living instanceof SnowGolem) return false;   // Made of snow
        return true;
    }

    /**
     * Evaluates thermal visibility (0.0 = blocked/out of range, 1.0 = clear sightline,
     * intermediate = attenuated through THERMAL_TRANSPARENT wood/glass/leaves).
     */
    public static float computeEntityThermalHeat(LivingEntity entity, Vec3 camPos, float partialTick, net.minecraft.world.level.Level level) {
        if (entity == null || level == null || camPos == null || !emitsThermalHeat(entity)) {
            return 0.0f;
        }
        double lerpX = entity.xOld + (entity.getX() - entity.xOld) * partialTick;
        double lerpY = entity.yOld + (entity.getY() - entity.yOld) * partialTick;
        double lerpZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTick;

        double dx = lerpX - camPos.x;
        double dy = lerpY - camPos.y;
        double dz = lerpZ - camPos.z;
        if (dx * dx + dy * dy + dz * dz > 4096.0) {
            return 0.0f;
        }

        // Interpolate bounding box with partialTick and slightly inset Y to avoid floor-block false hits
        AABB bb = entity.getBoundingBox().move(lerpX - entity.getX(), lerpY - entity.getY(), lerpZ - entity.getZ());
        double minY = Math.min(bb.maxY, bb.minY + 0.08);
        double maxY = Math.max(minY, bb.maxY - 0.05);
        Vec3[] points = new Vec3[]{
            new Vec3(lerpX, lerpY + entity.getBbHeight() * 0.5, lerpZ),
            new Vec3(bb.minX, minY, bb.minZ),
            new Vec3(bb.minX, minY, bb.maxZ),
            new Vec3(bb.minX, maxY, bb.minZ),
            new Vec3(bb.minX, maxY, bb.maxZ),
            new Vec3(bb.maxX, minY, bb.minZ),
            new Vec3(bb.maxX, minY, bb.maxZ),
            new Vec3(bb.maxX, maxY, bb.minZ),
            new Vec3(bb.maxX, maxY, bb.maxZ)
        };

        float maxHeat = 0.0f;
        for (Vec3 pt : points) {
            float ptHeat = getThermalAttenuation(camPos, pt, level);
            if (ptHeat > maxHeat) maxHeat = ptHeat;
            if (maxHeat >= 1.0f) break;
        }
        return maxHeat;
    }

    /** Call from RenderLevelStageEvent.AFTER_BLOCK_ENTITIES / WorldRenderEvents.AFTER_ENTITIES */
    public static void captureAndComposite(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            maskValidThisFrame = false;
            return;
        }

        boolean isThermalMode = VisionRenderer.isVisionModeActive()
                && (VisionRenderer.getCurrentVisionMode() == VisionMode.THERMAL
                    || (VisionRenderer.getCurrentVisionMode() == null && VisionShaderManager.isThermalShaderActive()));
        if (!isThermalMode) {
            maskValidThisFrame = false;
            return;
        }

        if (!initialized) {
            init();
            if (!initialized) {
                maskValidThisFrame = false;
                return;
            }
        }

        // Capture full GL state BEFORE resizeFBO so resized FBOs never overwrite prevDrawFbo/prevProgram
        int mainW = Math.max(1, mc.getMainRenderTarget().width);
        int mainH = Math.max(1, mc.getMainRenderTarget().height);

        int prevDrawFbo = GL11C.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = GL11C.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] prevViewport = new int[4];
        GL11C.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);
        float[] prevClearColor = new float[4];
        GL11C.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, prevClearColor);
        int prevProgram = GL11C.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevActiveTex = GL11C.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        forceActiveTexture(GL13.GL_TEXTURE0);
        int prevTex0 = GL11C.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        boolean oldShadow = mc.options.entityShadows().get();

        try {
            maskValidThisFrame = false;

            if (mainW != fboWidth || mainH != fboHeight) {
                try {
                    resizeFBO(mainW, mainH, mc);
                } catch (Exception e) {
                    Vision_goggles.LOGGER.error("[ThermalEntityPainter] Failed to resize FBOs: " + e.getMessage());
                    cleanup();
                    return;
                }
            }

            float partialTick = mc.getFrameTime();
            Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

            Integer defaultLuaColor = dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge.hookThermalColor("general", false);
            int defaultRgb = (defaultLuaColor != null) ? (defaultLuaColor & 0x00FFFFFF) : 0x00FFFF;

            Map<Integer, List<ThermalRenderEntry>> throughWallBuckets = new LinkedHashMap<>();
            Map<Integer, List<ThermalRenderEntry>> directBuckets = new LinkedHashMap<>();

            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity living)) continue;
                if (entity == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON) continue;

                double lerpX = entity.xOld + (entity.getX() - entity.xOld) * partialTick;
                double lerpY = entity.yOld + (entity.getY() - entity.yOld) * partialTick;
                double lerpZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTick;

                double dx = lerpX - camPos.x;
                double dy = lerpY - camPos.y;
                double dz = lerpZ - camPos.z;
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > 4096.0) continue; // 64 block limit

                float maxHeat = computeEntityThermalHeat(living, camPos, partialTick, mc.level);
                if (maxHeat <= 0.11f) continue; // Too weak or fully blocked

                Integer customColor = resolveEntityThermalColor(living);
                int bucketRgb = (customColor != null) ? (customColor & 0x00FFFFFF) : defaultRgb;
                ThermalRenderEntry entry = new ThermalRenderEntry(living, dx, dy, dz, maxHeat, distSq);
                if (maxHeat < 0.99f) {
                    throughWallBuckets.computeIfAbsent(bucketRgb, k -> new ArrayList<>()).add(entry);
                } else {
                    directBuckets.computeIfAbsent(bucketRgb, k -> new ArrayList<>()).add(entry);
                }
            }

            hasPerEntityCustomColor = false;

            // Pre-compute camera world-up direction in view space once per frame
            Vector3f worldUpView = new Vector3f(0.0f, 1.0f, 0.0f);
            poseStack.last().pose().transformDirection(0.0f, 1.0f, 0.0f, worldUpView);
            float upLen = worldUpView.length();
            if (upLen > 1e-5f) {
                worldUpView.div(upLen);
            } else {
                worldUpView.set(0.0f, 1.0f, 0.0f);
            }

            // Always clear maskFbo first so stale silhouettes from previous frames never persist
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, maskFbo);
            GlStateManager._viewport(0, 0, fboWidth, fboHeight);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.clearColor(0f, 0f, 0f, 0f);
            RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT, false);

            if (throughWallBuckets.isEmpty() && directBuckets.isEmpty()) {
                maskValidThisFrame = false;
                return;
            }

            // NOTE: Never call mc.renderBuffers().bufferSource().endBatch() here!
            // That shared bufferSource holds pending item-frame items, sign text, banners, etc.
            // Flushing it while maskFbo (which has no depth buffer) is bound draws X-ray items and glyphs into ThermalMask.
            dispatcher.setRenderShadow(false);

            // Phase 1: Through-wall thermal signatures (behind THERMAL_TRANSPARENT wood/glass/leaves)
            if (!throughWallBuckets.isEmpty()) {
                for (List<ThermalRenderEntry> group : throughWallBuckets.values()) {
                    group.sort(Comparator.comparingDouble((ThermalRenderEntry e) -> e.distSq).reversed());
                }

                GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
                GlStateManager._viewport(0, 0, fboWidth, fboHeight);
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.clearColor(0f, 0f, 0f, 0f);
                RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);

                if (throughWallBuckets.size() > 1) {
                    RenderSystem.colorMask(false, false, false, false);
                    for (List<ThermalRenderEntry> group : throughWallBuckets.values()) {
                        for (ThermalRenderEntry entry : group) {
                            renderEntityMask(dispatcher, poseStack, entry, partialTick, worldUpView);
                        }
                    }
                    RenderSystem.colorMask(true, true, true, true);
                }

                for (Map.Entry<Integer, List<ThermalRenderEntry>> bucket : throughWallBuckets.entrySet()) {
                    GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
                    GlStateManager._viewport(0, 0, fboWidth, fboHeight);
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(GL11.GL_LEQUAL);
                    RenderSystem.colorMask(true, true, true, true);
                    RenderSystem.clearColor(0f, 0f, 0f, 0f);
                    RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT, false);

                    for (ThermalRenderEntry entry : bucket.getValue()) {
                        renderEntityMask(dispatcher, poseStack, entry, partialTick, worldUpView);
                    }

                    compositeIntoMask(bucket.getKey(), prevProgram);
                }
            }

            // Phase 2: Direct-sightline entities occluded pixel-for-pixel by the world's depth buffer
            if (!directBuckets.isEmpty()) {
                int srcDepthFbo = (prevDrawFbo > 0) ? prevDrawFbo : mc.getMainRenderTarget().frameBufferId;
                boolean useWorldDepth = depthBlitSupported;

                if (useWorldDepth && directBuckets.size() > 1) {
                    // Verify world depth blit once before looping buckets
                    useWorldDepth = blitWorldDepthToCapture(mc, srcDepthFbo, mainW, mainH);
                }

                if (useWorldDepth) {
                    boolean firstBlitDone = (directBuckets.size() > 1);
                    for (Map.Entry<Integer, List<ThermalRenderEntry>> bucket : directBuckets.entrySet()) {
                        if (firstBlitDone) {
                            firstBlitDone = false;
                        } else if (!blitWorldDepthToCapture(mc, srcDepthFbo, mainW, mainH)) {
                            useWorldDepth = false;
                            break;
                        }

                        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
                        GlStateManager._viewport(0, 0, fboWidth, fboHeight);
                        RenderSystem.enableDepthTest();
                        RenderSystem.depthFunc(GL11.GL_LEQUAL);
                        RenderSystem.colorMask(true, true, true, true);
                        RenderSystem.clearColor(0f, 0f, 0f, 0f);
                        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT, false);

                        RenderSystem.polygonOffset(-1.0f, -2.0f);
                        RenderSystem.enablePolygonOffset();
                        try {
                            for (ThermalRenderEntry entry : bucket.getValue()) {
                                renderEntityMask(dispatcher, poseStack, entry, partialTick, worldUpView);
                            }
                        } finally {
                            RenderSystem.disablePolygonOffset();
                            RenderSystem.polygonOffset(0.0f, 0.0f);
                        }

                        compositeIntoMask(bucket.getKey(), prevProgram);
                    }
                }

                if (!useWorldDepth) {
                    // Safe fallback when world depth blit is unsupported by current pipeline
                    GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
                    GlStateManager._viewport(0, 0, fboWidth, fboHeight);
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(GL11.GL_LEQUAL);
                    RenderSystem.depthMask(true);
                    RenderSystem.colorMask(true, true, true, true);
                    RenderSystem.clearColor(0f, 0f, 0f, 0f);
                    RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);

                    if (directBuckets.size() > 1) {
                        RenderSystem.colorMask(false, false, false, false);
                        for (List<ThermalRenderEntry> group : directBuckets.values()) {
                            for (ThermalRenderEntry entry : group) {
                                renderEntityMask(dispatcher, poseStack, entry, partialTick, worldUpView);
                            }
                        }
                        RenderSystem.colorMask(true, true, true, true);
                    }

                    for (Map.Entry<Integer, List<ThermalRenderEntry>> bucket : directBuckets.entrySet()) {
                        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
                        GlStateManager._viewport(0, 0, fboWidth, fboHeight);
                        RenderSystem.enableDepthTest();
                        RenderSystem.depthFunc(GL11.GL_LEQUAL);
                        RenderSystem.colorMask(true, true, true, true);
                        RenderSystem.clearColor(0f, 0f, 0f, 0f);
                        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT, false);

                        for (ThermalRenderEntry entry : bucket.getValue()) {
                            renderEntityMask(dispatcher, poseStack, entry, partialTick, worldUpView);
                        }

                        compositeIntoMask(bucket.getKey(), prevProgram);
                    }
                }
            }

            maskValidThisFrame = true;
        } catch (Throwable t) {
            maskValidThisFrame = false;
            Vision_goggles.LOGGER.debug("[ThermalEntityPainter] Suppressed frame render error: {}", t.getMessage());
        } finally {
            RenderSystem.disablePolygonOffset();
            RenderSystem.polygonOffset(0.0f, 0.0f);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.clearColor(prevClearColor[0], prevClearColor[1], prevClearColor[2], prevClearColor[3]);
            dispatcher.setRenderShadow(oldShadow);

            GlStateManager._glUseProgram(prevProgram);
            forceActiveTexture(GL13.GL_TEXTURE0);
            forceBindTexture2D(prevTex0);
            forceActiveTexture(prevActiveTex);

            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
            GlStateManager._viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        }
    }

    private static void forceActiveTexture(int unit) {
        GL13.glActiveTexture(unit);
        GlStateManager._activeTexture(unit);
    }

    private static void forceBindTexture2D(int textureId) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GlStateManager._bindTexture(textureId);
    }

    private static boolean blitWorldDepthToCapture(Minecraft mc, int srcDepthFbo, int srcW, int srcH) {
        if (!depthBlitSupported || srcDepthFbo <= 0 || captureFbo <= 0) {
            return false;
        }
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {}

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcDepthFbo);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, captureFbo);
        GL30.glBlitFramebuffer(
                0, 0, srcW, srcH,
                0, 0, fboWidth, fboHeight,
                GL11.GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST
        );
        int err = GL11.glGetError();
        if (err != GL11.GL_NO_ERROR) {
            if (!depthBlitRetried) {
                depthBlitRetried = true;
                Vision_goggles.LOGGER.warn("[ThermalEntityPainter] Depth blit failed (GL error {}), recreating capture FBO with detected world depth format...", err);
                recreateCaptureFBO(fboWidth, fboHeight, mc);
                while (GL11.glGetError() != GL11.GL_NO_ERROR) {}
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcDepthFbo);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, captureFbo);
                GL30.glBlitFramebuffer(
                        0, 0, srcW, srcH,
                        0, 0, fboWidth, fboHeight,
                        GL11.GL_DEPTH_BUFFER_BIT,
                        GL11.GL_NEAREST
                );
                err = GL11.glGetError();
            }
            if (err != GL11.GL_NO_ERROR) {
                depthBlitSupported = false;
                Vision_goggles.LOGGER.warn("[ThermalEntityPainter] Depth blit unsupported on current pipeline (GL error {}), using raycast-only occlusion fallback.", err);
                return false;
            }
        }
        return true;
    }

    /**
     * Erases ThermalMask pixels occluded by the first-person hand / held weapon.
     * GameRenderer.renderItemInHand() clears the main depth buffer before drawing the hand/gun,
     * so any pixel with main depth < 1.0 right after renderLevel() belongs to the first-person item.
     */
    public static void occludeFirstPersonHand() {
        if (!initialized || !maskValidThisFrame || !depthBlitSupported
                || !VisionShaderManager.isThermalShaderActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getMainRenderTarget() == null || mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        int mainFbo = mc.getMainRenderTarget().frameBufferId;
        int mainW = Math.max(1, mc.getMainRenderTarget().width);
        int mainH = Math.max(1, mc.getMainRenderTarget().height);
        if (mainFbo <= 0 || captureFbo <= 0 || maskFbo <= 0 || mainW != fboWidth || mainH != fboHeight) {
            return;
        }

        int prevDrawFbo = GL11C.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = GL11C.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] prevViewport = new int[4];
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, prevViewport);
        int prevProgram = GL11C.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevActiveTex = GL11C.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        forceActiveTexture(GL13.GL_TEXTURE0);
        int prevTex0 = GL11C.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try {
            // Flush any deferred first-person item/weapon buffers before blitting depth
            try {
                mc.renderBuffers().bufferSource().endBatch();
            } catch (Throwable ignored) {}

            int srcDepthFbo = (prevDrawFbo > 0) ? prevDrawFbo : mainFbo;
            if (!blitWorldDepthToCapture(mc, srcDepthFbo, mainW, mainH)) {
                return;
            }

            // Clear captureFbo's color attachment to transparent (0,0,0,0) while preserving the blitted 1P depth buffer
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
            GlStateManager._viewport(0, 0, fboWidth, fboHeight);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.clearColor(0f, 0f, 0f, 0f);
            RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT, false);

            // Draw current maskTexture at NDC z = 1.0 (window depth 1.0) with GL_EQUAL depth test:
            // passes ONLY where depth == 1.0 (background untouched by 1P hand/gun),
            // leaving 1P hand/gun pixels (depth < 1.0) at transparent (0,0,0,0).
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_EQUAL);
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();

            GlStateManager._glUseProgram(programId);
            forceActiveTexture(GL13.GL_TEXTURE0);
            forceBindTexture2D(maskTexture);
            GL20.glUniform1i(GL20.glGetUniformLocation(programId, "EntityMask"), 0);
            GL20.glUniform4f(GL20.glGetUniformLocation(programId, "TintColor"), 1.0f, 1.0f, 1.0f, 1.0f);
            int useVertexColorLoc = GL20.glGetUniformLocation(programId, "UseVertexColor");
            if (useVertexColorLoc != -1) {
                GL20.glUniform1f(useVertexColorLoc, 1.0f);
            }

            GL30.glBindVertexArray(quadVAO);
            GL30.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            GL30.glBindVertexArray(0);
            BufferUploader.invalidate();

            // Copy occluded mask back into maskFbo
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, captureFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, maskFbo);
            GL30.glBlitFramebuffer(
                    0, 0, fboWidth, fboHeight,
                    0, 0, fboWidth, fboHeight,
                    GL11.GL_COLOR_BUFFER_BIT,
                    GL11.GL_NEAREST
            );
        } catch (Throwable ignored) {
        } finally {
            GlStateManager._glUseProgram(prevProgram);
            forceActiveTexture(GL13.GL_TEXTURE0);
            forceBindTexture2D(prevTex0);
            forceActiveTexture(prevActiveTex);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
            GlStateManager._viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        }
    }

    private static void renderEntityMask(EntityRenderDispatcher dispatcher, PoseStack poseStack, ThermalRenderEntry entry, float partialTick, Vector3f worldUpView) {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
        GlStateManager._viewport(0, 0, fboWidth, fboHeight);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        PoseStack entityPoseStack = new PoseStack();
        entityPoseStack.last().pose().set(poseStack.last().pose());
        entityPoseStack.last().normal().set(poseStack.last().normal());
        entityPoseStack.translate(entry.dx, entry.dy, entry.dz);

        // Entity foot origin in view space (after camera rotation + entity translation)
        Vector3f footView = new Vector3f();
        entityPoseStack.last().pose().transformPosition(0.0f, 0.0f, 0.0f, footView);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        final float finalHeat = entry.heat;
        final float entityHeight = entry.entity.getBbHeight();

        try {
            float lerpYaw = entry.entity.yRotO + (entry.entity.getYRot() - entry.entity.yRotO) * partialTick;

            final float[] slotFactors = computeSlotFactors(entry.entity);
            MultiBufferSource wrappedSource = type -> {
                if (shouldDiscardRenderType(type)) {
                    return DISCARD_VERTEX_CONSUMER;
                }

                // Organic body/fur/robe layers carry full heat; armor emits no heat: drawn at minimum alpha (cold) so it still occludes body by depth
                boolean isArmor = isArmorRenderType(type);
                int baseAlpha = Math.round(finalHeat * 255.0f);
                return new AlphaOverrideVertexConsumer(
                        maskBufferSource.getBuffer(type),
                        baseAlpha,
                        footView.x, footView.y, footView.z,
                        worldUpView.x, worldUpView.y, worldUpView.z,
                        entityHeight,
                        !isArmor,
                        isArmor ? slotFactors : null
                );
            };

            dispatcher.render(entry.entity, 0, 0, 0, lerpYaw, partialTick, entityPoseStack, wrappedSource, 15728880);
        } catch (Throwable ignored) {
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
            GlStateManager._viewport(0, 0, fboWidth, fboHeight);
            try {
                maskBufferSource.endBatch();
            } catch (Throwable ignored) {
            }
            if (maskBuffer != null && maskBuffer.building()) {
                try { maskBuffer.discard(); } catch (Throwable ignored) {}
            }
        }
    }

    private static boolean shouldDiscardRenderType(net.minecraft.client.renderer.RenderType type) {
        if (type == net.minecraft.client.renderer.RenderType.leash()
                || type == net.minecraft.client.renderer.RenderType.lines()
                || type == net.minecraft.client.renderer.RenderType.lineStrip()
                || type == net.minecraft.client.renderer.RenderType.lightning()
                || type == net.minecraft.client.renderer.RenderType.waterMask()
                || type == net.minecraft.client.renderer.RenderType.endPortal()
                || type == net.minecraft.client.renderer.RenderType.endGateway()) {
            return true;
        }
        String raw = type.toString().toLowerCase(Locale.ROOT);
        int colonIdx = raw.indexOf(':');
        boolean hasRtName = raw.startsWith("rendertype[") && colonIdx > 11;
        String rtName = hasRtName ? raw.substring(11, colonIdx) : raw;

        if ((hasRtName && (rtName.contains("text") || rtName.contains("font")))
                || rtName.startsWith("text") || rtName.equals("lines") || rtName.equals("line_strip")
                || rtName.equals("leash") || rtName.equals("lightning") || rtName.equals("beacon_beam")
                || rtName.equals("entity_shadow") || rtName.equals("crumbling")
                || rtName.equals("end_portal") || rtName.equals("end_gateway") || rtName.equals("water_mask")
                || rtName.contains("glint") || rtName.equals("eyes") || rtName.startsWith("item_")
                || rtName.contains("item_entity") || rtName.contains("item_layered")) {
            return true;
        }

        return raw.contains("textures/atlas/blocks.png")
                || (raw.contains("textures/item/") && !raw.contains("/armor/") && !raw.contains("hazmat") && !raw.contains("mekasuit") && !raw.contains("space_suit") && !raw.contains("diving"))
                || raw.contains("textures/font/")
                || raw.contains("font/")
                || raw.contains("glyph")
                || raw.contains("textures/misc/enchanted")
                || raw.contains("/cape") || raw.contains("cape") || raw.contains("cloak")
                || raw.contains("cape.png")
                || raw.contains("elytra")
                || raw.contains("_eyes.png");
    }

    private static boolean isArmorRenderType(net.minecraft.client.renderer.RenderType type) {
        String raw = type.toString().toLowerCase(Locale.ROOT);
        int colonIdx = raw.indexOf(':');
        String rtName = (raw.startsWith("rendertype[") && colonIdx > 11) ? raw.substring(11, colonIdx) : raw;
        return rtName.contains("armor") || raw.contains("textures/models/armor/") || raw.contains("/armor/") || rtName.contains("armor_") || raw.contains("hazmat") || raw.contains("mekasuit") || raw.contains("space_suit") || raw.contains("diving");
    }

    private static void compositeIntoMask(int rgbColor, int restoreProgram) {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, maskFbo);
        GlStateManager._viewport(0, 0, fboWidth, fboHeight);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();

        GlStateManager._glUseProgram(programId);

        forceActiveTexture(GL13.GL_TEXTURE0);
        forceBindTexture2D(colorTexture);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "EntityMask"), 0);

        float tintR = ((rgbColor >> 16) & 0xFF) / 255.0f;
        float tintG = ((rgbColor >> 8) & 0xFF) / 255.0f;
        float tintB = (rgbColor & 0xFF) / 255.0f;

        // Alpha = 1.0f (unpremultiplied); thermal_overlay.fsh discards pixels with mask alpha < 0.01
        GL20.glUniform4f(GL20.glGetUniformLocation(programId, "TintColor"), tintR, tintG, tintB, 1.0f);
        int useVertexColorLoc = GL20.glGetUniformLocation(programId, "UseVertexColor");
        if (useVertexColorLoc != -1) {
            GL20.glUniform1f(useVertexColorLoc, 0.0f);
        }

        GL30.glBindVertexArray(quadVAO);
        GL30.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
        BufferUploader.invalidate();

        // Restore caller's shader program so Blaze3D ShaderInstance.lastProgramId cache stays consistent
        GlStateManager._glUseProgram(restoreProgram);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    // ── FBO & Resource Internals ───────────────────────────────────────────

    private static int[] detectWorldDepthFormat(Minecraft mc) {
        int format = GL14.GL_DEPTH_COMPONENT24;
        int attachment = GL30.GL_DEPTH_ATTACHMENT;
        int prevActiveTex = GL11C.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        forceActiveTexture(GL13.GL_TEXTURE0);
        int prevTex = GL11C.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            if (mc != null && mc.getMainRenderTarget() != null) {
                int depthTex = mc.getMainRenderTarget().getDepthTextureId();
                if (depthTex > 0) {
                    forceBindTexture2D(depthTex);
                    int texFmt = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);

                    if (texFmt == GL30.GL_DEPTH24_STENCIL8 || texFmt == GL30.GL_DEPTH32F_STENCIL8) {
                        format = texFmt;
                        attachment = GL30.GL_DEPTH_STENCIL_ATTACHMENT;
                    } else if (texFmt == GL14.GL_DEPTH_COMPONENT16
                            || texFmt == GL14.GL_DEPTH_COMPONENT24
                            || texFmt == GL14.GL_DEPTH_COMPONENT32
                            || texFmt == GL30.GL_DEPTH_COMPONENT32F) {
                        format = texFmt;
                        attachment = GL30.GL_DEPTH_ATTACHMENT;
                    } else if (texFmt == GL11.GL_DEPTH_COMPONENT || texFmt == 0) {
                        format = GL14.GL_DEPTH_COMPONENT24;
                        attachment = GL30.GL_DEPTH_ATTACHMENT;
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            forceBindTexture2D(prevTex);
            forceActiveTexture(prevActiveTex);
        }
        return new int[]{format, attachment};
    }

    private static void createFBO(int w, int h, Minecraft mc) {
        int prevDrawFbo = GL11C.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = GL11C.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevActiveTex = GL11C.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        forceActiveTexture(GL13.GL_TEXTURE0);
        int prevTex = GL11C.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        try {
            allocateCaptureFBO(w, h, mc);

            maskFbo = GlStateManager.glGenFramebuffers();
            maskTexture = GL30.glGenTextures();
            forceBindTexture2D(maskTexture);
            GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, w, h, 0, GL30.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, maskFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, maskTexture, 0);
            int maskStatus = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (maskStatus != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("maskFbo incomplete: status=" + maskStatus);
            }
            fboWidth = w;
            fboHeight = h;
        } finally {
            forceBindTexture2D(prevTex);
            forceActiveTexture(prevActiveTex);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
        }
    }

    private static void allocateCaptureFBO(int w, int h, Minecraft mc) {
        int[] depthInfo = detectWorldDepthFormat(mc);
        captureDepthFormat = depthInfo[0];
        captureDepthAttachment = depthInfo[1];

        captureFbo = GlStateManager.glGenFramebuffers();
        colorTexture = GL30.glGenTextures();
        forceActiveTexture(GL13.GL_TEXTURE0);
        forceBindTexture2D(colorTexture);
        GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, w, h, 0, GL30.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);

        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, captureFbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, colorTexture, 0);

        depthRenderbuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderbuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, captureDepthFormat, w, h);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, captureDepthAttachment, GL30.GL_RENDERBUFFER, depthRenderbuffer);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);

        int capStatus = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (capStatus != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("captureFbo incomplete: status=" + capStatus);
        }
    }

    private static void recreateCaptureFBO(int w, int h, Minecraft mc) {
        int prevDrawFbo = GL11C.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = GL11C.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevActiveTex = GL11C.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        forceActiveTexture(GL13.GL_TEXTURE0);
        int prevTex = GL11C.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int oldCaptureFbo = captureFbo;

        try {
            if (captureFbo != 0) { GlStateManager._glDeleteFramebuffers(captureFbo); captureFbo = 0; }
            if (colorTexture != 0) { GlStateManager._deleteTexture(colorTexture); colorTexture = 0; }
            if (depthRenderbuffer != 0) { GL30.glDeleteRenderbuffers(depthRenderbuffer); depthRenderbuffer = 0; }
            allocateCaptureFBO(w, h, mc);
        } finally {
            forceBindTexture2D(prevTex);
            forceActiveTexture(prevActiveTex);
            int restoreDraw = (prevDrawFbo == oldCaptureFbo) ? 0 : prevDrawFbo;
            int restoreRead = (prevReadFbo == oldCaptureFbo) ? 0 : prevReadFbo;
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, restoreDraw);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, restoreRead);
        }
    }

    private static void resizeFBO(int w, int h, Minecraft mc) {
        if (captureFbo != 0) { GlStateManager._glDeleteFramebuffers(captureFbo); captureFbo = 0; }
        if (colorTexture != 0) { GlStateManager._deleteTexture(colorTexture); colorTexture = 0; }
        if (depthRenderbuffer != 0) { GL30.glDeleteRenderbuffers(depthRenderbuffer); depthRenderbuffer = 0; }
        if (maskFbo != 0) { GlStateManager._glDeleteFramebuffers(maskFbo); maskFbo = 0; }
        if (maskTexture != 0) { GlStateManager._deleteTexture(maskTexture); maskTexture = 0; }
        fboWidth = 0;
        fboHeight = 0;
        maskValidThisFrame = false;
        createFBO(w, h, mc);
    }

    private static int ensureEmptyMaskTexture() {
        if (emptyMaskTexture != 0) return emptyMaskTexture;
        int prevActiveTex = GL11C.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        forceActiveTexture(GL13.GL_TEXTURE0);
        int prevTex = GL11C.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int prevRowLen = GL11C.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int prevSkipPixels = GL11C.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        int prevSkipRows = GL11C.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int prevAlignment = GL11C.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer zero = stack.calloc(4);
            emptyMaskTexture = GL30.glGenTextures();
            forceBindTexture2D(emptyMaskTexture);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, zero);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        } catch (Throwable e) {
            return -1;
        } finally {
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, prevRowLen);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, prevSkipPixels);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, prevSkipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, prevAlignment);
            forceBindTexture2D(prevTex);
            forceActiveTexture(prevActiveTex);
        }
        return emptyMaskTexture;
    }

    private static void createShader() throws IOException {
        String vsSource = loadShader("shaders/thermal/quad.vsh");
        String fsSource = loadShader("shaders/thermal/thermal_overlay.fsh");

        vertexShaderId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShaderId, vsSource);
        GL20.glCompileShader(vertexShaderId);
        if (GL20.glGetShaderi(vertexShaderId, GL20.GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("VS compile error: " + GL20.glGetShaderInfoLog(vertexShaderId));

        fragmentShaderId = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShaderId, fsSource);
        GL20.glCompileShader(fragmentShaderId);
        if (GL20.glGetShaderi(fragmentShaderId, GL20.GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("FS compile error: " + GL20.glGetShaderInfoLog(fragmentShaderId));

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vertexShaderId);
        GL20.glAttachShader(programId, fragmentShaderId);
        GL20.glBindAttribLocation(programId, 0, "Position");
        GL20.glBindAttribLocation(programId, 1, "TexCoord");
        GL20.glLinkProgram(programId);
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0)
            throw new RuntimeException("Shader link error: " + GL20.glGetProgramInfoLog(programId));
    }

    private static String loadShader(String path) throws IOException {
        ResourceLocation loc = new ResourceLocation(Vision_goggles.MOD_ID, path);
        var opt = Minecraft.getInstance().getResourceManager().getResource(loc);
        if (opt.isEmpty() && path.equals("shaders/thermal/thermal_overlay.fsh")) {
            opt = Minecraft.getInstance().getResourceManager().getResource(
                    new ResourceLocation(Vision_goggles.MOD_ID, "shaders/program/thermal_vision.fsh")
            );
        }
        Resource res = opt.orElseThrow();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.open(), StandardCharsets.UTF_8))) {
            String src = reader.lines().collect(Collectors.joining("\n"));
            if (path.equals("shaders/thermal/thermal_overlay.fsh") && !src.contains("UseVertexColor")) {
                src = """
                        #version 150
                        uniform sampler2D EntityMask;
                        uniform vec4 TintColor;
                        uniform float UseVertexColor;
                        in vec2 texCoord;
                        out vec4 fragColor;
                        void main() {
                            vec4 sampleCol = texture(EntityMask, texCoord);
                            float alpha = sampleCol.a;
                            if (alpha < 0.01) discard;
                            vec3 rgb = (UseVertexColor > 0.5) ? sampleCol.rgb : TintColor.rgb;
                            fragColor = vec4(rgb, alpha * TintColor.a);
                        }
                        """;
            }
            return src;
        }
    }

    private static void createQuad() {
        quadVAO = GL30.glGenVertexArrays();
        quadVBO = GL30.glGenBuffers();
        float[] verts = {
                -1f,  1f,  0f, 1f,
                -1f, -1f,  0f, 0f,
                 1f, -1f,  1f, 0f,
                -1f,  1f,  0f, 1f,
                 1f, -1f,  1f, 0f,
                 1f,  1f,  1f, 1f
        };
        GL30.glBindVertexArray(quadVAO);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, quadVBO);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, verts, GL30.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        BufferUploader.invalidate();
    }

    /**
     * Calculates heat attenuation by walking the ray.
     * Returns 1.0f (full heat) if no blocks.
     * Returns 0.0f if a solid block is hit.
     * Multiplies heat by 0.6f for each transparent block (planks, glass) it passes through.
     */
    private static float getThermalAttenuation(Vec3 from, Vec3 to, net.minecraft.world.level.Level level) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.5) return 1.0f;

        // Step size of 0.25 guarantees we won't skip thin blocks like iron bars
        double stepX = (dx / dist) * 0.25;
        double stepY = (dy / dist) * 0.25;
        double stepZ = (dz / dist) * 0.25;
        int steps = (int) (dist / 0.25);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos lastPos = new BlockPos.MutableBlockPos();
        lastPos.set(0, -2000, 0);

        float heat = 1.0f;

        for (int i = 1; i <= steps; i++) {
            pos.set(from.x + stepX * i, from.y + stepY * i, from.z + stepZ * i);

            if (pos.equals(lastPos)) continue;
            lastPos.set(pos);

            BlockState state = level.getBlockState(pos);

            if (state.isAir()) continue;
            if (state.getCollisionShape(level, pos).isEmpty()) continue;

            boolean isTransparent = false;

            if (state.is(THERMAL_TRANSPARENT)) isTransparent = true;

            if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LEAVES) ||
                state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.WOODEN_SLABS) ||
                state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.DOORS) ||
                state.is(BlockTags.TRAPDOORS)) isTransparent = true;

            if (state.is(Blocks.GLASS) || state.getBlock().getDescriptionId().contains("glass")) isTransparent = true;

            if (isTransparent) {
                heat *= 0.6f;
                if (heat < 0.05f) return 0.0f;
                continue;
            }

            return 0.0f;
        }
        return heat;
    }

    public static final TagKey<net.minecraft.world.item.Item> INSULATION_LOW = TagKey.create(Registries.ITEM, new ResourceLocation("vision_goggles", "thermal_insulation_low"));
    public static final TagKey<net.minecraft.world.item.Item> INSULATION_MEDIUM = TagKey.create(Registries.ITEM, new ResourceLocation("vision_goggles", "thermal_insulation_medium"));
    public static final TagKey<net.minecraft.world.item.Item> INSULATION_HIGH = TagKey.create(Registries.ITEM, new ResourceLocation("vision_goggles", "thermal_insulation_high"));
    public static final TagKey<net.minecraft.world.item.Item> INSULATION_FULL = TagKey.create(Registries.ITEM, new ResourceLocation("vision_goggles", "thermal_insulation_full"));

    /** Heat factor: 1.0 = no armor/full heat, 0.75 low, 0.50 medium, 0.25 high, 0.0 full insulation. Tags first, then stat fallback for modded armor. */
    public static float getArmorThermalFactor(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot) {
        if (stack == null || stack.isEmpty()) return 1.0f;
        if (stack.is(INSULATION_FULL)) return 0.0f;
        if (stack.is(INSULATION_HIGH)) return 0.25f;
        if (stack.is(INSULATION_MEDIUM)) return 0.50f;
        if (stack.is(INSULATION_LOW)) return 0.75f;
        if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor) {
            int ironDef = switch (slot) { case CHEST -> 6; case LEGS -> 5; default -> 2; };
            int diaDef = switch (slot) { case CHEST -> 8; case LEGS -> 6; default -> 3; };
            float tough = armor.getToughness();
            int def = armor.getDefense();
            if (tough >= 3.0f && def >= diaDef) return 0.0f;
            if (tough >= 2.0f && def >= diaDef) return 0.25f;
            if (def >= ironDef) return 0.50f;
            return 0.75f;
        }
        return slot == net.minecraft.world.entity.EquipmentSlot.HEAD ? 0.85f : 1.0f;
    }

    private static float[] computeSlotFactors(Entity e) {
        float[] f = {1f, 1f, 1f, 1f};
        if (e instanceof LivingEntity le) {
            net.minecraft.world.entity.EquipmentSlot[] s = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
            for (int i = 0; i < 4; i++) f[i] = getArmorThermalFactor(le.getItemBySlot(s[i]), s[i]);
        }
        return f;
    }

    /**
     * Wraps vertex writes so alpha encodes true world-up height gradient and view-normal Fresnel
     * shading without any dependency on camera pitch, yaw, or distance.
     */
    private static class AlphaOverrideVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final int baseAlpha;
        private final float footX;
        private final float footY;
        private final float footZ;
        private final float upX;
        private final float upY;
        private final float upZ;
        private final float entityHeight;
        private final boolean isBody;
        private final float[] slotFactors; // head, chest, legs, feet; null = not armor

        private double lastX = 0;
        private double lastY = 0;
        private double lastZ = 0;

        AlphaOverrideVertexConsumer(
                VertexConsumer delegate,
                int baseAlpha,
                float footX, float footY, float footZ,
                float upX, float upY, float upZ,
                float entityHeight,
                boolean isBody,
                float[] slotFactors) {
            this.slotFactors = slotFactors;
            this.delegate = delegate;
            this.baseAlpha = Math.max(0, Math.min(255, baseAlpha));
            this.footX = footX;
            this.footY = footY;
            this.footZ = footZ;
            this.upX = upX;
            this.upY = upY;
            this.upZ = upZ;
            this.entityHeight = entityHeight;
            this.isBody = isBody;
        }

        private int computeZonedAlpha(double vx, double vy, double vz, float facing) {
            double localY = (vx - footX) * upX + (vy - footY) * upY + (vz - footZ) * upZ;
            float relY = (entityHeight > 0.01f) ? (float) Math.max(0.0, Math.min(1.0, localY / entityHeight)) : 0.5f;
            float smoothY = relY * relY * (3.0f - 2.0f * relY);
            float zoneLum = isBody ? (0.84f + 0.16f * smoothY) : 0.90f;
            float fresnel = isBody ? (0.88f + 0.12f * facing) : (0.90f + 0.10f * facing);
            // Keep alpha >= 26 (26/255 = 0.102 > 0.10) so heavily-attenuated/armored entities survive vanilla alpha cutout
            float armorFactor = 1.0f;
            if (slotFactors != null) {
                armorFactor = slotFactors[relY >= 0.78f ? 0 : relY >= 0.42f ? 1 : relY >= 0.18f ? 2 : 3];
            }
            return Math.min(255, Math.max(26, Math.round(this.baseAlpha * zoneLum * fresnel * armorFactor)));
        }

        @Override
        public void vertex(float x, float y, float z, float r, float g, float b, float a,
                           float u, float v, int overlay, int light, float nx, float ny, float nz) {
            float vLen = (float) Math.sqrt(x * x + y * y + z * z);
            float nLen = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            float facing;
            if (vLen > 1e-4f && nLen > 1e-4f) {
                facing = Math.min(1.0f, Math.abs((nx * x + ny * y + nz * z) / (vLen * nLen)));
            } else {
                facing = Math.min(1.0f, Math.abs(nz));
            }
            int zonedAlpha = computeZonedAlpha(x, y, z, facing);
            delegate.vertex(x, y, z, 1.0f, 1.0f, 1.0f, zonedAlpha / 255.0f, u, v, overlay, light, nx, ny, nz);
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            int zonedAlpha = computeZonedAlpha(this.lastX, this.lastY, this.lastZ, 0.85f);
            delegate.color(255, 255, 255, zonedAlpha);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) { delegate.uv(u, v); return this; }

        @Override
        public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u, v); return this; }

        @Override
        public VertexConsumer uv2(int u, int v) { delegate.uv2(u, v); return this; }

        @Override
        public VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }

        @Override
        public void endVertex() { delegate.endVertex(); }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(255, 255, 255, this.baseAlpha);
        }

        @Override
        public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}
