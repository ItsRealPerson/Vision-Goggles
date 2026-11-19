package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the 4-phase first-person 3D tactical equip animation for Vision Goggles
 * directly inside {@code ItemInHandRenderer#renderArmWithItem}.
 */
public final class FirstPersonEquipRenderer {

    @FunctionalInterface
    public interface ItemRenderCallback {
        void render(
                LivingEntity entity,
                ItemStack stack,
                ItemDisplayContext displayContext,
                boolean leftHand,
                PoseStack poseStack,
                MultiBufferSource buffer,
                int combinedLight
        );
    }

    private static final float VISUAL_PIVOT_Z = 9.0f / 16.0f;
    private static final float ARM_SCALE = 0.84f;

    private static PlayerModel<AbstractClientPlayer> wideArmModel;
    private static PlayerModel<AbstractClientPlayer> slimArmModel;

    private FirstPersonEquipRenderer() {}

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
            InteractionHand hand,
            HumanoidArm arm,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            ItemRenderCallback itemRenderCallback
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (GogglesEquipAnimator.consumeStartSound()) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_LEATHER, 1.35F, 0.35F));
        }
        if (GogglesEquipAnimator.consumeStrapSound()) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_LEATHER, 1.05F, 0.50F));
        }

        // Phase 4: Hide stale held goggles after finishUsingItem and smoothly retract empty arm(s)
        if (GogglesEquipAnimator.isCompleting()) {
            if (!player.isInvisible()) {
                float retract = smoothstep01(GogglesEquipAnimator.getRetractProgress());
                renderRetractingArm(player, arm, poseStack, buffer, combinedLight, retract);
                if (GogglesEquipAnimator.isTwoHanded()) {
                    renderRetractingArm(player, arm.getOpposite(), poseStack, buffer, combinedLight, retract);
                }
            }
            return;
        }

        // Phases 1-3 (plus smooth cancel ease-out)
        float t = GogglesEquipAnimator.getProgress();
        float p1 = smoothstep01(t / 0.38f);
        float p2 = smoothstep01((t - 0.34f) / 0.54f);
        float p3 = smoothstep01((t - 0.86f) / 0.14f);

        boolean leftHand = (arm == HumanoidArm.LEFT);
        float sideSign = leftHand ? -1.0f : 1.0f;
        ItemDisplayContext displayContext = leftHand
                ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

        // Match vanilla ItemInHandRenderer#applyItemArmTransform at t = 0 so transition starts seamlessly
        float idleX = sideSign * 0.56f;
        float idleY = -0.52f - 0.6f * equippedProgress;
        float idleZ = -0.72f;

        float x = Mth.lerp(p1, idleX, 0.0f);
        float y = Mth.lerp(p1, idleY, -0.24f) + 0.68f * p2 - 0.05f * (float) Math.sin(p3 * Math.PI);
        float z = Mth.lerp(p1, idleZ, -1.14f) + 0.24f * p2 - 0.05f * (float) Math.sin(p2 * Math.PI);

        float rotX = -34.0f * p1 - 18.0f * p2 + 4.0f * (float) Math.sin(p3 * Math.PI);
        float rotY = sideSign * 6.0f * p1 * (1.0f - p2);
        float rotZ = -sideSign * 5.0f * p1 * (1.0f - p2);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.translate(0.0f, 0.0f, VISUAL_PIVOT_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));

        if (!player.isInvisible() && p1 > 0.02f) {
            float primaryOffscreen = 1.0f - smoothstep01((t - 0.04f) / 0.30f);
            renderGrippingArm(player, arm, poseStack, buffer, combinedLight, p2, primaryOffscreen);

            if (GogglesEquipAnimator.isTwoHanded()) {
                float oppOffscreen = 1.0f - smoothstep01((t - 0.08f) / 0.30f);
                renderGrippingArm(player, arm.getOpposite(), poseStack, buffer, combinedLight, p2, oppOffscreen);
            }
        }

        poseStack.translate(0.0f, 0.0f, -VISUAL_PIVOT_Z);
        itemRenderCallback.render(player, stack, displayContext, leftHand, poseStack, buffer, combinedLight);
        poseStack.popPose();
    }

    private static void renderGrippingArm(
            AbstractClientPlayer player,
            HumanoidArm armSide,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            float liftProgress,
            float offscreen
    ) {
        float s = (armSide == HumanoidArm.RIGHT) ? 1.0f : -1.0f;
        float anchorX = s * (0.34f + offscreen * 0.24f);
        float anchorY = -0.04f - offscreen * 0.45f;
        float anchorZ = 0.02f + offscreen * 0.20f;

        poseStack.pushPose();
        poseStack.translate(anchorX, anchorY, anchorZ);
        poseStack.mulPose(Axis.ZP.rotationDegrees(s * (26.0f - liftProgress * 6.0f)));
        poseStack.mulPose(Axis.XP.rotationDegrees(-42.0f + liftProgress * 12.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - s * 16.0f));
        drawPlayerArm(player, armSide, poseStack, buffer, combinedLight, ARM_SCALE);
        poseStack.popPose();
    }

    private static void renderRetractingArm(
            AbstractClientPlayer player,
            HumanoidArm armSide,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            float retract
    ) {
        float s = (armSide == HumanoidArm.RIGHT) ? 1.0f : -1.0f;
        float rx = s * Mth.lerp(retract, 0.36f, 0.58f);
        float ry = Mth.lerp(retract, 0.30f, -0.62f);
        float rz = Mth.lerp(retract, -0.38f, -0.22f);

        poseStack.pushPose();
        poseStack.translate(rx, ry, rz);
        poseStack.mulPose(Axis.ZP.rotationDegrees(s * (20.0f + retract * 14.0f)));
        poseStack.mulPose(Axis.XP.rotationDegrees(-30.0f - retract * 22.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - s * 16.0f));
        drawPlayerArm(player, armSide, poseStack, buffer, combinedLight, ARM_SCALE);
        poseStack.popPose();
    }

    private static void drawPlayerArm(
            AbstractClientPlayer player,
            HumanoidArm armSide,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            float scale
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
        poseStack.scale(scale, scale, scale);
        poseStack.translate(centerOffsetX, -9.0f / 16.0f, 0.0f);

        ResourceLocation skin = player.getSkinTextureLocation();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(skin));
        armPart.render(poseStack, consumer, combinedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        if (sleevePart.visible) {
            sleevePart.render(poseStack, consumer, combinedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static float smoothstep01(float t) {
        float c = Mth.clamp(t, 0.0f, 1.0f);
        return c * c * (3.0f - 2.0f * c);
    }
}
