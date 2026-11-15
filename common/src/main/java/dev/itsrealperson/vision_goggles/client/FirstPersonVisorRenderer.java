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
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
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
    private static PlayerModel<AbstractClientPlayer> wideArmModel;
    private static PlayerModel<AbstractClientPlayer> slimArmModel;

    private static FirstPersonVisorModel getModel() {
        if (model == null) {
            model = new FirstPersonVisorModel(FirstPersonVisorModel.createBodyLayer().bakeRoot());
        }
        return model;
    }

    private static PlayerModel<AbstractClientPlayer> getArmModel(boolean slim) {
        if (slim) {
            if (slimArmModel == null) {
                slimArmModel = new PlayerModel<>(
                        LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, true), 64, 64).bakeRoot(),
                        true
                );
            }
            return slimArmModel;
        }
        if (wideArmModel == null) {
            wideArmModel = new PlayerModel<>(
                    LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64).bakeRoot(),
                    false
            );
        }
        return wideArmModel;
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
        RenderSystem.depthMask(true);
        Lighting.setupFor3DItems();

        PoseStack poseStack = gui.pose();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // 1. Render the 3D tactical visor descending from above the viewport onto the eyes
        float yOffset = Mth.lerp(progress, -height * 0.55f, 0.0f);
        float zArc = (float) Math.sin(progress * Math.PI) * 35.0f;
        float eyeZoom = smoothstep01((progress - 0.55f) / 0.45f);
        float scale = height * Mth.lerp(eyeZoom, 0.95f, 1.80f);
        boolean isRightArm = mc.player.getMainArm() == HumanoidArm.RIGHT;
        float sideSign = isRightArm ? 1.0f : -1.0f;

        poseStack.pushPose();
        poseStack.translate(width * 0.5f, height * 0.5f + yOffset, 300.0f + zArc);
        poseStack.scale(scale, scale, scale);
        // Slight tactical 3D angle so the dual-tube underside and side rim read clearly
        poseStack.mulPose(Axis.YP.rotationDegrees(6.0f * sideSign * (1.0f - progress * 0.5f)));
        poseStack.mulPose(Axis.XP.rotationDegrees(14.0f));

        VertexConsumer baseConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(baseTex));
        visorModel.renderToBuffer(poseStack, baseConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);

        if (progress > 0.70f) {
            VertexConsumer glowConsumer = bufferSource.getBuffer(RenderType.eyes(glowTex));
            visorModel.renderToBuffer(poseStack, glowConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        poseStack.popPose();

        // 2. Render the player's main arm + sleeve reaching in to grip the side of the goggles
        renderPlayerArm(mc.player, poseStack, bufferSource, width, height, scale, yOffset, zArc, progress, isRightArm, sideSign);

        bufferSource.endBatch();

        RenderSystem.disableDepthTest();
    }

    private static void renderPlayerArm(
            AbstractClientPlayer player,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int width,
            int height,
            float visorScale,
            float yOffset,
            float zArc,
            float progress,
            boolean isRightArm,
            float sideSign
    ) {
        boolean slim = "slim".equals(player.getModelName());
        PlayerModel<AbstractClientPlayer> armModel = getArmModel(slim);
        ModelPart arm = isRightArm ? armModel.rightArm : armModel.leftArm;
        ModelPart sleeve = isRightArm ? armModel.rightSleeve : armModel.leftSleeve;

        arm.setPos(0.0f, 0.0f, 0.0f);
        arm.setRotation(0.0f, 0.0f, 0.0f);
        arm.visible = true;

        sleeve.setPos(0.0f, 0.0f, 0.0f);
        sleeve.setRotation(0.0f, 0.0f, 0.0f);
        sleeve.visible = player.isModelPartShown(isRightArm ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE);

        // Reach-in (0.0 -> 0.12), grip & pull/push (0.12 -> 0.88), retract (0.88 -> 1.0)
        float reachIn = smoothstep01(progress / 0.12f);
        float retract = smoothstep01((progress - 0.88f) / 0.12f);
        float offscreen = Mth.clamp((1.0f - reachIn) + retract, 0.0f, 1.0f);

        // Anchor the fist directly onto the outer optical tube / side rim of the 3D goggles
        float visorEdgeX = width * 0.5f + sideSign * (visorScale * 0.17f);
        float visorEdgeY = height * 0.5f + yOffset + visorScale * 0.02f;
        float visorEdgeZ = 300.0f + zArc + 18.0f;

        float handX = visorEdgeX + sideSign * (offscreen * width * 0.09f);
        float handY = visorEdgeY + offscreen * height * 0.12f;
        float handZ = visorEdgeZ + 12.0f;

        float armScale = height * 1.45f;

        poseStack.pushPose();
        poseStack.translate(handX, handY, handZ);
        poseStack.scale(armScale, armScale, armScale);

        // Proven orientation: +Y fist points up-left toward the visor edge, shoulder stays off-screen bottom-right
        poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * (140.0f - offscreen * 10.0f - progress * 6.0f)));
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0f + progress * 10.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * (18.0f - offscreen * 8.0f)));

        // Shift so the hand/knuckles (local y = +9/16) sit directly at the grip anchor
        poseStack.translate(0.0f, -9.0f / 16.0f, 0.0f);

        ResourceLocation skin = player.getSkinTextureLocation();
        VertexConsumer armConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(skin));
        arm.render(poseStack, armConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        if (sleeve.visible) {
            sleeve.render(poseStack, armConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        poseStack.popPose();
    }

    private static float smoothstep01(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
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
