package dev.itsrealperson.vision_goggles.client.lighting;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public final class VisionShadowMapRenderer {
    public static final VisionShadowMapRenderer INSTANCE = new VisionShadowMapRenderer();
    public static boolean enabled = true;
    public boolean active = false;

    private static final int MAX_LIGHTS = FlashlightUniforms.GLSL_MAX;
    private int tileRes = 512;
    private int atlasCols = 4;
    private int atlasRows = 4;
    private int atlasW = 2048;
    private int atlasH = 2048;

    private TextureTarget fbo;
    public final Matrix4f[] lightVP = new Matrix4f[MAX_LIGHTS];
    public final float[] lightVPFlat = new float[MAX_LIGHTS * 16];
    public final float[] lightNear = new float[MAX_LIGHTS];
    public final float[] lightFar = new float[MAX_LIGHTS];
    public final float[] lightDirFlat = new float[MAX_LIGHTS * 3];
    public final float[] texelWorldArr = new float[MAX_LIGHTS];
    public int shadowCount = 0;

    private final Matrix4f scratchProj = new Matrix4f();
    private final Matrix4f scratchView = new Matrix4f();

    private VisionShadowMapRenderer() {
        for (int i = 0; i < MAX_LIGHTS; i++) {
            lightVP[i] = new Matrix4f();
        }
    }

    private void ensureAtlas(int n) {
        int tile = 512;
        int cols = Math.max(1, (int) Math.ceil(Math.sqrt(n)));
        int rows = Math.max(1, (int) Math.ceil((double) n / (double) cols));
        int w = cols * tile;
        int h = rows * tile;

        if (fbo != null && (atlasW != w || atlasH != h)) {
            fbo.destroyBuffers();
            fbo = null;
        }
        if (fbo == null) {
            fbo = new TextureTarget(w, h, true, Minecraft.ON_OSX);
            fbo.setClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        this.tileRes = tile;
        this.atlasCols = cols;
        this.atlasRows = rows;
        this.atlasW = w;
        this.atlasH = h;
    }

    public void bindTexture() {
        if (fbo != null) {
            RenderSystem.setShaderTexture(5, fbo.getDepthTextureId());
        }
    }

    public static void renderShadowPass(LevelRenderer lr, double camX, double camY, double camZ) {
        INSTANCE.doRenderShadowPass(lr, camX, camY, camZ);
    }

    public void doRenderShadowPass(LevelRenderer lr, double camX, double camY, double camZ) {
        if (!enabled || lr == null) {
            active = false;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!RenderSystem.isOnRenderThread()) {
            active = false;
            return;
        }

        int n = FlashlightUniforms.currentCount;
        if (n <= 0) {
            active = false;
            return;
        }

        n = Math.min(n, MAX_LIGHTS);
        ensureAtlas(n);
        if (fbo == null) {
            active = false;
            return;
        }

        RenderTarget mainFbo = mc.getMainRenderTarget();
        fbo.clear(Minecraft.ON_OSX);
        fbo.bindWrite(true);

        for (int i = 0; i < n; i++) {
            renderOneSource(mc, lr, i, camX, camY, camZ);
        }

        mainFbo.bindWrite(true);
        this.shadowCount = n;
        this.active = true;
    }

    private void renderOneSource(Minecraft mc, LevelRenderer lr, int idx, double camX, double camY, double camZ) {
        int offset = idx * 3;
        float px = FlashlightUniforms.posArray[offset];
        float py = FlashlightUniforms.posArray[offset + 1];
        float pz = FlashlightUniforms.posArray[offset + 2];

        float dx = FlashlightUniforms.dirArray[offset];
        float dy = FlashlightUniforms.dirArray[offset + 1];
        float dz = FlashlightUniforms.dirArray[offset + 2];

        float range = FlashlightUniforms.rangeArray[idx];
        if (range <= 0.1f) return;

        float fovDeg = 90.0f;
        float near = 0.05f;
        float far = Math.max(10.0f, range + 2.0f);

        Matrix4f proj = scratchProj.setPerspective((float) Math.toRadians(fovDeg), 1.0f, near, far);

        Vector3f dir = new Vector3f(dx, dy, dz).normalize();
        Vector3f up = new Vector3f(0, 1, 0);
        if (Math.abs(dir.y) > 0.95f) {
            up.set(0, 0, 1);
        }
        Vector3f right = new Vector3f(dir).cross(up).normalize();
        up.set(right).cross(dir).normalize();

        Matrix4f view = scratchView.setLookAlong(dir, up);

        Matrix4f vp = new Matrix4f(proj).mul(view);
        lightVP[idx].set(vp);
        vp.get(lightVPFlat, idx * 16);

        lightNear[idx] = near;
        lightFar[idx] = far;
        lightDirFlat[offset] = dx;
        lightDirFlat[offset + 1] = dy;
        lightDirFlat[offset + 2] = dz;
        texelWorldArr[idx] = (far - near) / (float) tileRes;

        int tileCol = idx % atlasCols;
        int tileRow = idx / atlasCols;
        int vx = tileCol * tileRes;
        int vy = tileRow * tileRes;

        GL11.glViewport(vx, vy, tileRes, tileRes);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
    }

    public TextureTarget getFbo() {
        return fbo;
    }
}
