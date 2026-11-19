package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL11C;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders all living entities as flat cyan silhouettes when thermal vision is active.
 * Uses a simple FBO + shader approach - no blur, no outlines, just solid color painting.
 */
public class ThermalEntityPainter {

    /** Blocks in this tag do NOT block thermal vision (planks, glass, leaves, etc.) */
    private static final TagKey<Block> THERMAL_TRANSPARENT = TagKey.create(
            Registries.BLOCK,
            new ResourceLocation(Vision_goggles.MOD_ID, "thermal_transparent")
    );

    // FBO resources
    private static int fboId = 0;
    private static int colorTexture = 0;
    private static int depthRenderbuffer = 0;
    private static int fboWidth = 0;
    private static int fboHeight = 0;

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

    // True no-op consumer so held items, emissive eyes, glints, capes, and elytras are discarded cleanly
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
    private static boolean active = false;
    private static boolean hasPerEntityCustomColor = false;

    // Thermal tint color: cyan
    private static final float TINT_R = 0.0f;
    private static final float TINT_G = 1.0f;
    private static final float TINT_B = 1.0f;
    private static final float TINT_A = 0.85f;

    public static boolean hasPerEntityCustomColor() {
        return hasPerEntityCustomColor;
    }

    public static void setHasPerEntityCustomColor(boolean value) {
        hasPerEntityCustomColor = value;
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
            createFBO(w, h);
            createShader();
            createQuad();
            maskBuffer = new BufferBuilder(262144);
            maskBufferSource = MultiBufferSource.immediate(maskBuffer);

            initialized = true;
        } catch (Exception e) {
            cleanup();
            Vision_goggles.LOGGER.error("[ThermalEntityPainter] Failed to initialize: " + e.getMessage());
        }
    }

    public static void cleanup() {
        if (fboId != 0) { GlStateManager._glDeleteFramebuffers(fboId); fboId = 0; }
        if (colorTexture != 0) { GlStateManager._deleteTexture(colorTexture); colorTexture = 0; }
        if (depthRenderbuffer != 0) { GL30.glDeleteRenderbuffers(depthRenderbuffer); depthRenderbuffer = 0; }
        if (programId != 0) { GL20.glDeleteProgram(programId); programId = 0; }
        if (vertexShaderId != 0) { GL20.glDeleteShader(vertexShaderId); vertexShaderId = 0; }
        if (fragmentShaderId != 0) { GL20.glDeleteShader(fragmentShaderId); fragmentShaderId = 0; }
        if (quadVAO != -1) { GL30.glDeleteVertexArrays(quadVAO); GL30.glDeleteBuffers(quadVBO); quadVAO = -1; }
        initialized = false;
    }

    private static class ThermalRenderEntry {
        final LivingEntity entity;
        final double dx;
        final double dy;
        final double dz;
        final float heat;
        final int colorRgb;

        ThermalRenderEntry(LivingEntity entity, double dx, double dy, double dz, float heat, int colorRgb) {
            this.entity = entity;
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.heat = heat;
            this.colorRgb = colorRgb;
        }
    }

    public static boolean emitsThermalHeat(LivingEntity living) {
        if (living == null) return false;
        // Undead: no body heat
        if (living.getMobType() == MobType.UNDEAD
                || living instanceof net.minecraft.world.entity.monster.Zombie
                || living.getType().getDescriptionId().toLowerCase(java.util.Locale.ROOT).contains("zombie")) {
            return false;
        }
        // Cold-blooded / constructs / spirits
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

    /** Call from RenderLevelStageEvent.AFTER_ENTITIES */
    public static void captureAndComposite(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Check if thermal vision mode is active on the local player
        if (!VisionRenderer.isVisionModeActive() || VisionRenderer.getCurrentVisionMode() != dev.itsrealperson.vision_goggles.util.VisionMode.THERMAL) return;

        // Lazy-init on first use if not already initialized (e.g. on Fabric where client init runs before ResourceManager)
        if (!initialized) {
            init();
            if (!initialized) return;
        }

        float partialTick = mc.getFrameTime();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        Integer defaultLuaColor = dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge.hookThermalColor("general", false);
        int defaultRgb = (defaultLuaColor != null) ? (defaultLuaColor & 0x00FFFFFF) : 0x00FFFF;

        Map<Integer, List<ThermalRenderEntry>> buckets = new LinkedHashMap<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            // Skip non-living, non-thermal mobs (undead, cold-blooded, constructs), and self in first person
            if (!(entity instanceof LivingEntity living)) continue;
            if (!emitsThermalHeat(living)) continue;
            if (entity == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON) continue;

            double lerpX = entity.xOld + (entity.getX() - entity.xOld) * partialTick;
            double lerpY = entity.yOld + (entity.getY() - entity.yOld) * partialTick;
            double lerpZ = entity.zOld + (entity.getZ() - entity.zOld) * partialTick;

            double dx = lerpX - camPos.x;
            double dy = lerpY - camPos.y;
            double dz = lerpZ - camPos.z;
            if (dx * dx + dy * dy + dz * dz > 4096) continue; // 64 block limit

            // Revisar 9 puntos (centro + 8 esquinas del Hitbox) para evitar falsos positivos por huecos
            net.minecraft.world.phys.AABB bb = entity.getBoundingBox();
            Vec3[] points = new Vec3[]{
                new Vec3(lerpX, lerpY + entity.getBbHeight() * 0.5, lerpZ), // Centro
                new Vec3(bb.minX, bb.minY, bb.minZ),
                new Vec3(bb.minX, bb.minY, bb.maxZ),
                new Vec3(bb.minX, bb.maxY, bb.minZ),
                new Vec3(bb.minX, bb.maxY, bb.maxZ),
                new Vec3(bb.maxX, bb.minY, bb.minZ),
                new Vec3(bb.maxX, bb.minY, bb.maxZ),
                new Vec3(bb.maxX, bb.maxY, bb.minZ),
                new Vec3(bb.maxX, bb.maxY, bb.maxZ)
            };

            float maxHeat = 0.0f;
            for (Vec3 pt : points) {
                float ptHeat = getThermalAttenuation(camPos, pt, mc.level);
                if (ptHeat > maxHeat) maxHeat = ptHeat;
                if (maxHeat >= 1.0f) break; // Si ya encontramos un punto 100% visible, no buscar más
            }

            float heat = maxHeat;
            if (heat <= 0.11f) continue; // Too weak or fully blocked

            Integer customColor = resolveEntityThermalColor(living);
            int bucketRgb = (customColor != null) ? (customColor & 0x00FFFFFF) : defaultRgb;
            buckets.computeIfAbsent(bucketRgb, k -> new ArrayList<>())
                   .add(new ThermalRenderEntry(living, dx, dy, dz, heat, bucketRgb));
        }

        hasPerEntityCustomColor = false;
        if (buckets.isEmpty()) {
            return;
        }

        // Resize FBO if window changed
        int mainW = mc.getMainRenderTarget().width;
        int mainH = mc.getMainRenderTarget().height;
        if (mainW != fboWidth || mainH != fboHeight) resizeFBO(mainW, mainH);

        // Save currently-bound FBO & GL clear color (Xenon/Sodium/Iris/Fabulous manages its own FBOs)
        int prevDrawFbo = GL11C.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = GL11C.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        float[] prevClearColor = new float[4];
        GL11C.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, prevClearColor);

        // Flush any previous batches to ensure clean state
        mc.renderBuffers().bufferSource().endBatch();

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        boolean oldShadow = mc.options.entityShadows().get();
        dispatcher.setRenderShadow(false);

        try {
            if (buckets.size() == 1) {
                Map.Entry<Integer, List<ThermalRenderEntry>> single = buckets.entrySet().iterator().next();
                GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
                GlStateManager._viewport(0, 0, fboWidth, fboHeight);
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.clearColor(0f, 0f, 0f, 0f);
                RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);

                for (ThermalRenderEntry entry : single.getValue()) {
                    renderEntityMask(dispatcher, poseStack, entry, partialTick);
                }

                bindCompositeTarget(mc, prevDrawFbo, mainW, mainH);
                doComposite(single.getKey());
            } else {
                // Multi-bucket: 1) depth pre-pass across all thermal entities so occlusion across colors is exact,
                // 2) render & composite each color bucket separately using flat TintColor (UseVertexColor=0)
                //    so skin textures never corrupt thermal RGB ratios.
                GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
                GlStateManager._viewport(0, 0, fboWidth, fboHeight);
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.clearColor(0f, 0f, 0f, 0f);
                RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);

                RenderSystem.colorMask(false, false, false, false);
                for (List<ThermalRenderEntry> group : buckets.values()) {
                    for (ThermalRenderEntry entry : group) {
                        renderEntityMask(dispatcher, poseStack, entry, partialTick);
                    }
                }

                RenderSystem.colorMask(true, true, true, true);
                for (Map.Entry<Integer, List<ThermalRenderEntry>> bucket : buckets.entrySet()) {
                    GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
                    GlStateManager._viewport(0, 0, fboWidth, fboHeight);
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(GL11.GL_LEQUAL);
                    RenderSystem.depthMask(false);
                    RenderSystem.disableBlend();
                    RenderSystem.clearColor(0f, 0f, 0f, 0f);
                    RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT, false);

                    for (ThermalRenderEntry entry : bucket.getValue()) {
                        renderEntityMask(dispatcher, poseStack, entry, partialTick);
                    }

                    bindCompositeTarget(mc, prevDrawFbo, mainW, mainH);
                    doComposite(bucket.getKey());
                }
            }
        } finally {
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.clearColor(prevClearColor[0], prevClearColor[1], prevClearColor[2], prevClearColor[3]);
            dispatcher.setRenderShadow(oldShadow);
            // Restore the previously-bound FBO so Xenon/Sodium/Fabulous can continue with its own pipeline
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFbo);
        }
    }

    private static void bindCompositeTarget(Minecraft mc, int prevDrawFbo, int mainW, int mainH) {
        if (prevDrawFbo > 0) {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevDrawFbo);
            GlStateManager._viewport(0, 0, mainW, mainH);
        } else {
            mc.getMainRenderTarget().bindWrite(false);
            GlStateManager._viewport(0, 0, mainW, mainH);
        }
    }

    private static void renderEntityMask(EntityRenderDispatcher dispatcher, PoseStack poseStack, ThermalRenderEntry entry, float partialTick) {
        // Re-assert fboId before each entity so any custom RenderType output state cannot redirect subsequent entities
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GlStateManager._viewport(0, 0, fboWidth, fboHeight);

        PoseStack entityPoseStack = new PoseStack();
        entityPoseStack.last().pose().set(poseStack.last().pose());
        entityPoseStack.last().normal().set(poseStack.last().normal());
        entityPoseStack.translate(entry.dx, entry.dy, entry.dz);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        final float finalHeat = entry.heat;
        final double finalDy = entry.dy;
        final float entityHeight = entry.entity.getBbHeight();

        try {
            float lerpYaw = entry.entity.yRotO + (entry.entity.getYRot() - entry.entity.yRotO) * partialTick;

            MultiBufferSource wrappedSource = type -> {
                String name = type.toString().toLowerCase(java.util.Locale.ROOT);
                // Discard held items, glowing eyes, glints, capes, elytras, and block overlays completely
                if (name.contains("item") || name.contains("eyes") || name.contains("glint") || name.contains("cape") || name.contains("elytra") || name.contains("blocks")) {
                    return DISCARD_VERTEX_CONSUMER;
                }

                // Organic body/fur/robe layers carry full body heat; armor acts as moderate thermal insulation
                boolean isArmor = name.contains("armor");
                if (!isArmor) {
                    return new AlphaOverrideVertexConsumer(maskBufferSource.getBuffer(type), (int) (finalHeat * 255), finalDy, entityHeight, true, null);
                } else {
                    return new AlphaOverrideVertexConsumer(maskBufferSource.getBuffer(type), (int) (finalHeat * 110), finalDy, entityHeight, false, null);
                }
            };

            dispatcher.render(entry.entity, 0, 0, 0, lerpYaw, partialTick, entityPoseStack, wrappedSource, 15728880);
        } catch (Throwable ignored) {
        } finally {
            try { maskBufferSource.endBatch(); } catch (Throwable ignored) {}
        }
    }

    private static void doComposite(int rgbColor) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );

        GL20.glUseProgram(programId);

        // Bind entity mask texture to unit 0
        RenderSystem.activeTexture(GL20.GL_TEXTURE0);
        GlStateManager._bindTexture(colorTexture);
        GL20.glUniform1i(GL20.glGetUniformLocation(programId, "EntityMask"), 0);

        float tintR = ((rgbColor >> 16) & 0xFF) / 255.0f;
        float tintG = ((rgbColor >> 8) & 0xFF) / 255.0f;
        float tintB = (rgbColor & 0xFF) / 255.0f;
        float tintA = TINT_A;

        GL20.glUniform4f(GL20.glGetUniformLocation(programId, "TintColor"), tintR, tintG, tintB, tintA);
        int useVertexColorLoc = GL20.glGetUniformLocation(programId, "UseVertexColor");
        if (useVertexColorLoc != -1) {
            GL20.glUniform1f(useVertexColorLoc, 0.0f);
        }

        GL30.glBindVertexArray(quadVAO);
        GL30.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
        BufferUploader.invalidate();

        GL20.glUseProgram(0);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        GlStateManager._bindTexture(0);
    }

    // ── Internals ──────────────────────────────────────────────────────────

    private static void createFBO(int w, int h) {
        fboWidth = w; fboHeight = h;
        fboId = GlStateManager.glGenFramebuffers();
        colorTexture = GL30.glGenTextures();
        GlStateManager._bindTexture(colorTexture);
        GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA8, w, h, 0, GL30.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, colorTexture, 0);
        depthRenderbuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderbuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, w, h);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRenderbuffer);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private static void resizeFBO(int w, int h) {
        if (fboId != 0) GlStateManager._glDeleteFramebuffers(fboId);
        if (colorTexture != 0) GlStateManager._deleteTexture(colorTexture);
        if (depthRenderbuffer != 0) GL30.glDeleteRenderbuffers(depthRenderbuffer);
        createFBO(w, h);
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
            // Inject UseVertexColor support if the loaded thermal_overlay.fsh doesn't declare it yet
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
        GL30.glBindVertexArray(0);
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
            
            if (pos.equals(lastPos)) continue; // Only process each block once
            lastPos.set(pos);

            BlockState state = level.getBlockState(pos);

            if (state.isAir()) continue;

            // Allow non-collidable blocks (e.g. tall grass, flowers) without losing heat
            if (state.getCollisionShape(level, pos).isEmpty()) continue;

            boolean isTransparent = false;

            // Allow thermal to pass through whitelisted blocks (custom tag)
            if (state.is(THERMAL_TRANSPARENT)) isTransparent = true;

            // Fallback: check hardcoded vanilla tags
            if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LEAVES) || 
                state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.WOODEN_SLABS) || 
                state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.DOORS) ||
                state.is(BlockTags.TRAPDOORS)) isTransparent = true;
                
            // Check for glass
            if (state.is(Blocks.GLASS) || state.getBlock().getDescriptionId().contains("glass")) isTransparent = true;

            if (isTransparent) {
                heat *= 0.6f; // Drop heat by 40% for each plank/glass wall
                if (heat < 0.05f) return 0.0f; // Too weak to see, consider blocked
                continue;
            }

            return 0.0f; // Solid, non-transparent block — blocks thermal view
        }
        return heat;
    }

    private static class AlphaOverrideVertexConsumer implements com.mojang.blaze3d.vertex.VertexConsumer {
        private final com.mojang.blaze3d.vertex.VertexConsumer delegate;
        private final int baseAlpha;
        private final double dy;
        private final float entityHeight;
        private final boolean isBody;
        private final Integer customColor;
        private double lastY = 0;

        public AlphaOverrideVertexConsumer(com.mojang.blaze3d.vertex.VertexConsumer delegate, int baseAlpha, double dy, float entityHeight, boolean isBody) {
            this(delegate, baseAlpha, dy, entityHeight, isBody, null);
        }

        public AlphaOverrideVertexConsumer(com.mojang.blaze3d.vertex.VertexConsumer delegate, int baseAlpha, double dy, float entityHeight, boolean isBody, Integer customColor) {
            this.delegate = delegate;
            this.baseAlpha = baseAlpha;
            this.dy = dy;
            this.entityHeight = entityHeight;
            this.isBody = isBody;
            this.customColor = customColor;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x, double y, double z) { 
            this.lastY = y;
            delegate.vertex(x, y, z); 
            return this; 
        }
        
        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer color(int r, int g, int b, int a) { 
            double relY = (entityHeight > 0.01f) ? Math.max(0.0, Math.min(1.0, (this.lastY - dy) / entityHeight)) : 0.5;
            float zoneLum = isBody ? (relY < 0.45 ? 0.78f : (relY < 0.80 ? 0.90f : 1.0f)) : 0.65f;
            int zonedAlpha = Math.min(255, Math.max(0, Math.round(this.baseAlpha * zoneLum)));
            if (customColor != null) {
                int cr = (customColor >> 16) & 0xFF;
                int cg = (customColor >> 8) & 0xFF;
                int cb = customColor & 0xFF;
                delegate.color(cr, cg, cb, zonedAlpha);
            } else {
                delegate.color(0, 255, 255, zonedAlpha); 
            }
            return this; 
        }
        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer uv(float u, float v) { delegate.uv(u, v); return this; }
        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u, v); return this; }
        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer uv2(int u, int v) { delegate.uv2(u, v); return this; }
        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer normal(float x, float y, float z) { delegate.normal(x, y, z); return this; }
        @Override
        public void endVertex() { delegate.endVertex(); }
        @Override
        public void defaultColor(int r, int g, int b, int a) {
            if (customColor != null) {
                int cr = (customColor >> 16) & 0xFF;
                int cg = (customColor >> 8) & 0xFF;
                int cb = customColor & 0xFF;
                delegate.defaultColor(cr, cg, cb, this.baseAlpha);
            } else {
                delegate.defaultColor(r, g, b, this.baseAlpha);
            }
        }
        @Override
        public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}
