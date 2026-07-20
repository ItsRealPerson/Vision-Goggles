package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    
    // Dummy buffer to discard unwanted rendering (armor, items, etc)
    private static BufferBuilder dummyBuffer = null;
    private static MultiBufferSource.BufferSource dummyBufferSource = null;

    private static boolean initialized = false;
    private static boolean active = false;

    // Thermal tint color: cyan
    private static final float TINT_R = 0.0f;
    private static final float TINT_G = 1.0f;
    private static final float TINT_B = 1.0f;
    private static final float TINT_A = 0.85f;

    public static void init() {
        if (initialized) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();
            createFBO(w, h);
            createShader();
            createQuad();
            maskBuffer = new BufferBuilder(262144);
            maskBufferSource = MultiBufferSource.immediate(maskBuffer);
            
            dummyBuffer = new BufferBuilder(262144);
            dummyBufferSource = MultiBufferSource.immediate(dummyBuffer);
            
            initialized = true;
        } catch (Exception e) {
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

    /** Call from RenderLevelStageEvent.AFTER_BLOCK_ENTITIES */
    public static void captureAndComposite(PoseStack poseStack) {
        if (!initialized) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Check if thermal vision is active on the local player
        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        if (helmet.isEmpty()) return;
        CompoundTag nbt = helmet.getTag();
        if (nbt == null || !nbt.getBoolean(ModConstants.TAG_ACTIVE) || nbt.getInt(ModConstants.TAG_MODE) != 1) return;

        // Resize FBO if window changed
        int w = mc.getMainRenderTarget().width;
        int h = mc.getMainRenderTarget().height;
        if (w != fboWidth || h != fboHeight) resizeFBO(w, h);

        // Flush any previous batches to ensure clean state
        mc.renderBuffers().bufferSource().endBatch();

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        boolean oldShadow = mc.options.entityShadows().get();
        dispatcher.setRenderShadow(false);

        float partialTick = mc.getFrameTime();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        // Asegurar que la máscara de profundidad esté activa ANTES de limpiar el FBO
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL30.GL_LEQUAL);
        RenderSystem.depthMask(true);

        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GlStateManager._viewport(0, 0, fboWidth, fboHeight);
        RenderSystem.clearColor(0f, 0f, 0f, 0f);
        RenderSystem.clear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT, false);

        for (Entity entity : mc.level.entitiesForRendering()) {
            // Skip non-living, undead (they don't emit heat), and self in first person
            if (!(entity instanceof LivingEntity living)) continue;
            // Undead: no body heat
            if (living.getMobType() == MobType.UNDEAD || living instanceof net.minecraft.world.entity.monster.Zombie || living.getType().getDescriptionId().toLowerCase().contains("zombie")) continue;
            // Cold-blooded / constructs / spirits
            if (living instanceof Slime && !(living instanceof MagmaCube)) continue; // Slime = cold, MagmaCube = hot
            if (living instanceof Spider) continue;      // Arthropod, cold-blooded
            if (living instanceof Silverfish) continue;  // Insect, cold-blooded
            if (living instanceof Endermite) continue;   // Cold-blooded parasite
            if (living instanceof Shulker) continue;     // Shell creature, no warm blood
            if (living instanceof Vex) continue;         // Spirit, no physical body
            if (living instanceof Guardian) continue;    // Fish, cold-blooded
            if (living instanceof Squid) continue;       // Cephalopod, cold-blooded
            if (living instanceof IronGolem) continue;   // Metal construct, no organic heat
            if (living instanceof SnowGolem) continue;   // Made of snow
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

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            
            // Set global shader color alpha to 1.0 (we will control alpha per vertex now)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            
            final float finalHeat = heat;
            try {
                float lerpYaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTick;
                
                net.minecraft.resources.ResourceLocation primaryTex = dispatcher.getRenderer(entity).getTextureLocation(entity);
                final String primaryPath = primaryTex != null ? primaryTex.getPath().toLowerCase() : "unknown_texture";
                
                MultiBufferSource wrappedSource = type -> {
                    String name = type.toString().toLowerCase();
                    // Ocultar items (usualmente blocks atlas) y otros accesorios que no deben verse
                    if (name.contains("item") || name.contains("eyes") || name.contains("glint") || name.contains("cape") || name.contains("elytra") || name.contains("blocks")) {
                        return dummyBufferSource.getBuffer(type);
                    }
                    
                    // Usamos una heurística infalible: si la textura es la textura PRINCIPAL de la entidad (su piel), es el cuerpo.
                    // Si es cualquier OTRA textura, es armadura, curios, mochilas, chalecos (mods externos).
                    boolean isBody = primaryTex != null && name.contains(primaryPath);
                    
                    final double finalDy = dy;
                    final float entityHeight = entity.getBbHeight();
                    
                    if (isBody) {
                        // Cuerpo -> Calor con gradiente vertical
                        return new AlphaOverrideVertexConsumer(maskBufferSource.getBuffer(type), (int)(finalHeat * 255), finalDy, entityHeight, true);
                    } else {
                        // Armadura / Custom Mods -> Frío (Aislante térmico, alpha = 38)
                        return new AlphaOverrideVertexConsumer(maskBufferSource.getBuffer(type), 38, finalDy, entityHeight, false);
                    }
                };
                
                dispatcher.render(entity, 0, 0, 0, lerpYaw, partialTick, poseStack, wrappedSource, 15728880);
            } catch (Exception ignored) {}
            
            // Flush immediately so the shader color applies correctly to this entity
            maskBufferSource.endBatch();
            
            // Flush dummy buffer silently
            try { dummyBufferSource.endBatch(); } catch (Exception ignored) {}
            
            poseStack.popPose();
        }

        // Reset shader color for normal rendering
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        dispatcher.setRenderShadow(oldShadow);

        // Restore main framebuffer and composite ONCE at the end
        mc.getMainRenderTarget().bindWrite(false);
        GlStateManager._viewport(0, 0, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
        doComposite();
    }

    private static void doComposite() {
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
        GL20.glUniform4f(GL20.glGetUniformLocation(programId, "TintColor"), TINT_R, TINT_G, TINT_B, TINT_A);

        GL30.glBindVertexArray(quadVAO);
        GL30.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);

        GL20.glUseProgram(0);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        GlStateManager._bindTexture(0);

        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
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
        Resource res = Minecraft.getInstance().getResourceManager().getResource(loc).orElseThrow();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.open(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
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
        private double lastY = 0;

        public AlphaOverrideVertexConsumer(com.mojang.blaze3d.vertex.VertexConsumer delegate, int baseAlpha, double dy, float entityHeight, boolean isBody) {
            this.delegate = delegate;
            this.baseAlpha = baseAlpha;
            this.dy = dy;
            this.entityHeight = entityHeight;
            this.isBody = isBody;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x, double y, double z) { 
            this.lastY = y;
            delegate.vertex(x, y, z); 
            return this; 
        }
        
        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer color(int r, int g, int b, int a) { 
            int finalAlpha = this.baseAlpha;
            if (isBody && entityHeight > 0) {
                // localY is the height of this vertex relative to the entity's feet (0.0 to entityHeight)
                double localY = this.lastY - this.dy;
                float normalizedY = (float) (localY / this.entityHeight);
                normalizedY = Math.max(0.0f, Math.min(1.0f, normalizedY));
                
                // Gradient: 40% heat at feet, 100% heat at head
                float gradient = 0.4f + (0.6f * normalizedY);
                finalAlpha = (int) (this.baseAlpha * gradient);
            }
            delegate.color(r, g, b, finalAlpha); 
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
        public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r, g, b, this.baseAlpha); }
        @Override
        public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}
