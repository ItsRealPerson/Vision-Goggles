package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Renders the 3D first-person tactical visor flipping down over (or up off) the camera
 * during the flip transition driven by {@link GogglesFlipAnimator}.
 */
public class FirstPersonVisorRenderer {
    private static final ResourceLocation NVG_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/nvg_texture.png");
    private static final ResourceLocation NVG_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/nvg_glow.png");

    private static final ResourceLocation THERMAL_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/thermal_vision.png");
    private static final ResourceLocation THERMAL_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/thermal_glow.png");

    private static final ResourceLocation HYDRO_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/hidro_vision.png");
    private static final ResourceLocation HYDRO_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/hidro_glow.png");

    private static final ResourceLocation BIO_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/bio_vision.png");
    private static final ResourceLocation BIO_GLOW = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/bio_glow.png");

    private static FirstPersonVisorModel model;

    private static FirstPersonVisorModel getModel() {
        if (model == null) {
            model = new FirstPersonVisorModel(FirstPersonVisorModel.createBodyLayer().bakeRoot());
        }
        return model;
    }

    public static void render(GuiGraphics gui, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (!GogglesFlipAnimator.isLocalTransitioning()) return;

        ItemStack stack = PlatformMethods.getEquippedHelmet(mc.player);
        if (stack.isEmpty() || !(stack.getItem() instanceof VisionGogglesItem goggles)) return;

        float progress = Mth.clamp(GogglesFlipAnimator.getLocalProgress(), 0.0f, 1.0f);
        int width = gui.guiWidth();
        int height = gui.guiHeight();

        VisionMode mode = resolveMode(stack, goggles);
        ResourceLocation baseTex = resolveBaseTexture(mode);
        ResourceLocation glowTex = resolveGlowTexture(mode);

        FirstPersonVisorModel visorModel = getModel();
        visorModel.visorPivot.xRot = Mth.lerp(progress, -1.15f, 0.05f);
        visorModel.visorPivot.yRot = 0.0f;
        visorModel.visorPivot.zRot = 0.0f;

        gui.flush();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();

        PoseStack poseStack = gui.pose();
        poseStack.pushPose();

        // Descend smoothly from above the top of the viewport down to the center of the camera
        float yOffset = Mth.lerp(progress, -height * 0.65f, 0.0f);
        float zArc = (float) Math.sin(progress * Math.PI) * 35.0f;
        poseStack.translate(width * 0.5f, height * 0.5f + yOffset, 220.0f + zArc);

        // Scale the 1/16th-unit model to span the center viewport cleanly
        float scale = height * 0.92f;
        poseStack.scale(scale, scale, scale);
        // Slight pitch tilt so the dual-tube underside and lens rims read clearly in 3D
        poseStack.mulPose(Axis.XP.rotationDegrees(10.0f));

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer baseConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(baseTex));
        visorModel.renderToBuffer(poseStack, baseConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);

        if (progress > 0.70f) {
            VertexConsumer glowConsumer = bufferSource.getBuffer(RenderType.eyes(glowTex));
            visorModel.renderToBuffer(poseStack, glowConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        }

        bufferSource.endBatch();
        poseStack.popPose();

        RenderSystem.disableDepthTest();
    }

    private static VisionMode resolveMode(ItemStack stack, VisionGogglesItem goggles) {
        if (goggles instanceof ModularGogglesItem modular) {
            List<VisionMode> modes = modular.getModes(stack);
            if (!modes.isEmpty()) {
                VisionMode selected = null;
                // Priority: Thermal > Bio > Hydro > NVG
                for (VisionMode m : modes) {
                    if (m == VisionMode.THERMAL) { selected = m; break; }
                    if (m == VisionMode.BIOMETRIC) { selected = m; break; }
                    if (m == VisionMode.HYDRO) { selected = m; }
                }
                return selected != null ? selected : modes.get(0);
            }
            return null;
        }
        return goggles.getVisionMode();
    }

    private static ResourceLocation resolveBaseTexture(VisionMode mode) {
        if (mode == VisionMode.THERMAL) return THERMAL_TEXTURE;
        if (mode == VisionMode.HYDRO) return HYDRO_TEXTURE;
        if (mode == VisionMode.BIOMETRIC) return BIO_TEXTURE;
        return NVG_TEXTURE;
    }

    private static ResourceLocation resolveGlowTexture(VisionMode mode) {
        if (mode == VisionMode.THERMAL) return THERMAL_GLOW;
        if (mode == VisionMode.HYDRO) return HYDRO_GLOW;
        if (mode == VisionMode.BIOMETRIC) return BIO_GLOW;
        return NVG_GLOW;
    }
}
