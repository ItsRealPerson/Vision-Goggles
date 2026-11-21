package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;

/**
 * Renders both of the player's 3D arms + sleeves in first-person reaching onto
 * the Modification Station's green cutting mat during blueprint assembly.
 */
public final class FirstPersonStationWorkRenderer {

    private static final float ARM_SCALE = 0.86f;

    private static PlayerModel<AbstractClientPlayer> wideArmModel;
    private static PlayerModel<AbstractClientPlayer> slimArmModel;

    private FirstPersonStationWorkRenderer() {}

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

    public static void render(
            AbstractClientPlayer player,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight
    ) {
        if (player == null || player.isInvisible()) return;

        float t = StationWorkAnimator.getProgress();
        float reachIn = smoothstep01(t / 0.18f);
        float retract = smoothstep01((t - 0.84f) / 0.16f);
        float offscreen = Mth.clamp((1.0f - reachIn) + retract, 0.0f, 1.0f);

        // Work cycle active between 0.16 and 0.84 (3 rhythmic tactical clicks/adjustments)
        float workWeight = smoothstep01((t - 0.14f) / 0.10f) * (1.0f - smoothstep01((t - 0.80f) / 0.10f));
        float workWave = (float) Math.sin(t * Math.PI * 6.0f) * workWeight;
        float steadyWave = (float) Math.sin(t * Math.PI * 3.0f) * workWeight;

        // Left hand: steadies goggles frame on the green cutting mat
        renderWorkArm(
                player, HumanoidArm.LEFT, poseStack, buffer, combinedLight,
                -0.30f - offscreen * 0.22f,
                -0.32f - offscreen * 0.44f + steadyWave * 0.015f,
                -0.58f + offscreen * 0.24f,
                -24.0f,
                -46.0f + steadyWave * 4.0f,
                180.0f + 22.0f
        );

        // Right hand: calibrates & tightens optical tubes on the mat
        renderWorkArm(
                player, HumanoidArm.RIGHT, poseStack, buffer, combinedLight,
                0.30f + offscreen * 0.22f - workWave * 0.03f,
                -0.27f - offscreen * 0.44f + Math.abs(workWave) * 0.045f,
                -0.56f + offscreen * 0.24f - workWave * 0.035f,
                22.0f - workWave * 8.0f,
                -42.0f + workWave * 14.0f,
                180.0f - 24.0f
        );
    }

    private static void renderWorkArm(
            AbstractClientPlayer player,
            HumanoidArm armSide,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            float x,
            float y,
            float z,
            float rotZ,
            float rotX,
            float rotY
    ) {
        boolean slim = "slim".equals(player.getModelName());
        boolean isRight = (armSide == HumanoidArm.RIGHT);
        PlayerModel<AbstractClientPlayer> armModel = getArmModel(slim);
        ModelPart armPart = isRight ? armModel.rightArm : armModel.leftArm;
        ModelPart sleevePart = isRight ? armModel.rightSleeve : armModel.leftSleeve;

        armPart.setPos(0.0f, 0.0f, 0.0f);
        armPart.setRotation(0.0f, 0.0f, 0.0f);
        armPart.visible = true;

        sleevePart.setPos(0.0f, 0.0f, 0.0f);
        sleevePart.setRotation(0.0f, 0.0f, 0.0f);
        sleevePart.visible = player.isModelPartShown(isRight ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE);

        float centerOffsetX = (isRight ? 1.0f : -1.0f) * (slim ? 0.5f : 1.0f) / 16.0f;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.scale(ARM_SCALE, ARM_SCALE * 1.45f, ARM_SCALE);
        poseStack.translate(centerOffsetX, -9.0f / 16.0f, 0.0f);

        ResourceLocation skin = player.getSkinTextureLocation();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(skin));
        armPart.render(poseStack, consumer, combinedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        if (sleevePart.visible) {
            sleevePart.render(poseStack, consumer, combinedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        poseStack.popPose();
    }

    private static float smoothstep01(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }
}
