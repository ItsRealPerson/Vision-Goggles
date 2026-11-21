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
 * Renders both of the player's 3D arms + sleeves in first-person poised over
 * the Modification Station, reaching to place parts and assembling goggles.
 */
public final class FirstPersonStationWorkRenderer {

    private static final float ARM_SCALE = 0.84f;

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

        float open = smoothstep01(StationWorkAnimator.getOpenBlend());
        float offscreen = 1.0f - open;

        // Gentle idle workbench breathing when hands are ready at the table edges
        float idleTime = (System.currentTimeMillis() % 10000L) / 1000.0f;
        float breath = (float) Math.sin(idleTime * 2.2f) * 0.008f;

        // Base poised workbench positions (bottom-left and bottom-right, framing the green mat)
        float lx = -0.34f - offscreen * 0.22f;
        float ly = -0.37f - offscreen * 0.40f + breath;
        float lz = -0.68f + offscreen * 0.20f;
        float lRotZ = -22.0f;
        float lRotX = -62.0f;
        float lRotY = 180.0f + 18.0f;

        float rx = 0.34f + offscreen * 0.22f;
        float ry = -0.37f - offscreen * 0.40f - breath;
        float rz = -0.68f + offscreen * 0.20f;
        float rRotZ = 22.0f;
        float rRotX = -62.0f;
        float rRotY = 180.0f - 18.0f;

        // Single-hand placement reach pulse forward onto the mat edge
        if (StationWorkAnimator.isPlacing()) {
            float p = (float) Math.sin(StationWorkAnimator.getPlaceProgress() * Math.PI);
            if (StationWorkAnimator.isPlacingRightHand()) {
                rx = Mth.lerp(p, rx, 0.20f);
                ry = Mth.lerp(p, ry, -0.30f);
                rz = Mth.lerp(p, rz, -0.76f);
                rRotX -= p * 10.0f;
                rRotZ += p * 6.0f;
            } else {
                lx = Mth.lerp(p, lx, -0.20f);
                ly = Mth.lerp(p, ly, -0.30f);
                lz = Mth.lerp(p, lz, -0.76f);
                lRotX -= p * 10.0f;
                lRotZ -= p * 6.0f;
            }
        }

        // Full 2-hand assembly motion: cradle/align on mat, press/fit parts + lens, then settle
        if (StationWorkAnimator.isAssembling()) {
            float t = StationWorkAnimator.getProgress();
            float w = smoothstep01(t / 0.14f) * (1.0f - smoothstep01((t - 0.84f) / 0.16f));

            if (t < 0.28f) {
                // Stage 1: Both hands reach forward onto the mat to cradle & align the chassis
                float cT = smoothstep01(t / 0.28f);
                lx = Mth.lerp(w, lx, Mth.lerp(cT, -0.29f, -0.25f));
                ly = Mth.lerp(w, ly, Mth.lerp(cT, -0.34f, -0.31f));
                lz = Mth.lerp(w, lz, Mth.lerp(cT, -0.70f, -0.73f));
                lRotZ = Mth.lerp(w, lRotZ, Mth.lerp(cT, -24.0f, -30.0f));
                lRotX = Mth.lerp(w, lRotX, Mth.lerp(cT, -62.0f, -64.0f));
                lRotY = Mth.lerp(w, lRotY, Mth.lerp(cT, 202.0f, 212.0f));

                rx = Mth.lerp(w, rx, Mth.lerp(cT, 0.29f, 0.26f));
                ry = Mth.lerp(w, ry, Mth.lerp(cT, -0.34f, -0.31f));
                rz = Mth.lerp(w, rz, Mth.lerp(cT, -0.70f, -0.72f));
                rRotZ = Mth.lerp(w, rRotZ, 24.0f);
                rRotX = Mth.lerp(w, rRotX, Mth.lerp(cT, -62.0f, -66.0f));
                rRotY = Mth.lerp(w, rRotY, Mth.lerp(cT, 158.0f, 154.0f));
            } else if (t < 0.72f) {
                // Stage 2: Left hand holds chassis steady; Right hand presses parts & front lens along the mat
                float stageT = (t - 0.28f) / 0.44f;
                float pressCycle = (float) Math.abs(Math.sin(stageT * Math.PI * 3.0f));
                float snapJolt = (float) Math.sin(stageT * Math.PI * 6.0f) * 0.008f;

                lx = Mth.lerp(w, lx, -0.25f);
                ly = Mth.lerp(w, ly, -0.31f - snapJolt);
                lz = Mth.lerp(w, lz, -0.73f);
                lRotZ = Mth.lerp(w, lRotZ, -30.0f);
                lRotX = Mth.lerp(w, lRotX, -64.0f);
                lRotY = Mth.lerp(w, lRotY, 212.0f);

                rx = Mth.lerp(w, rx, Mth.lerp(pressCycle, 0.26f, 0.17f));
                ry = Mth.lerp(w, ry, Mth.lerp(pressCycle, -0.31f, -0.26f));
                rz = Mth.lerp(w, rz, Mth.lerp(pressCycle, -0.72f, -0.80f));
                rRotZ = Mth.lerp(w, rRotZ, Mth.lerp(pressCycle, 24.0f, 34.0f));
                rRotX = Mth.lerp(w, rRotX, Mth.lerp(pressCycle, -66.0f, -76.0f));
                rRotY = Mth.lerp(w, rRotY, Mth.lerp(pressCycle, 154.0f, 142.0f));
            } else {
                // Stage 3: Both hands settle back to frame the finished goggles on the mat
                float relT = smoothstep01((t - 0.72f) / 0.28f);
                lx = Mth.lerp(w, lx, Mth.lerp(relT, -0.25f, -0.32f));
                ly = Mth.lerp(w, ly, Mth.lerp(relT, -0.31f, -0.35f));
                lz = Mth.lerp(w, lz, Mth.lerp(relT, -0.73f, -0.70f));
                lRotZ = Mth.lerp(w, lRotZ, Mth.lerp(relT, -30.0f, -22.0f));
                lRotX = Mth.lerp(w, lRotX, Mth.lerp(relT, -64.0f, -62.0f));
                lRotY = Mth.lerp(w, lRotY, Mth.lerp(relT, 212.0f, 198.0f));

                rx = Mth.lerp(w, rx, Mth.lerp(relT, 0.26f, 0.32f));
                ry = Mth.lerp(w, ry, Mth.lerp(relT, -0.31f, -0.35f));
                rz = Mth.lerp(w, rz, Mth.lerp(relT, -0.72f, -0.70f));
                rRotZ = Mth.lerp(w, rRotZ, Mth.lerp(relT, 24.0f, 22.0f));
                rRotX = Mth.lerp(w, rRotX, Mth.lerp(relT, -66.0f, -62.0f));
                rRotY = Mth.lerp(w, rRotY, Mth.lerp(relT, 154.0f, 162.0f));
            }
        }

        renderWorkArm(player, HumanoidArm.LEFT, poseStack, buffer, combinedLight, lx, ly, lz, lRotZ, lRotX, lRotY);
        renderWorkArm(player, HumanoidArm.RIGHT, poseStack, buffer, combinedLight, rx, ry, rz, rRotZ, rRotX, rRotY);
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
