package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.client.GogglesFlipAnimator;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Animates the player's arms reaching up to the forehead while equipping goggles
 * in 3rd-person/multiplayer, and animates the main arm during visor flip-down / flip-up.
 */
@Mixin(HumanoidModel.class)
public abstract class MixinHumanoidModel<T extends LivingEntity> {

    @Shadow
    public ModelPart rightArm;

    @Shadow
    public ModelPart leftArm;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void visionGoggles$animateTacticalFlip(
        T entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        CallbackInfo ci
    ) {
        if (!(entity instanceof Player player)) {
            return;
        }

        // PlayerRenderer#renderHand invokes setupAnim(player, 0, 0, 0, 0, 0) and only resets xRot;
        // skip pose modifications in that synthetic call so bare first-person hands stay unaffected.
        if (ageInTicks == 0.0f && limbSwing == 0.0f && limbSwingAmount == 0.0f && netHeadYaw == 0.0f && headPitch == 0.0f) {
            return;
        }

        float headPitchRad = headPitch * ((float) Math.PI / 180.0f);

        boolean isLocal = (player == Minecraft.getInstance().player);
        if (dev.itsrealperson.vision_goggles.client.StationWorkAnimator.isEntityWorking(player.getUUID(), isLocal)) {
            float wave = Mth.sin(ageInTicks * 0.65f) * 0.14f;
            visionGoggles$lerpArmPose(this.leftArm, HumanoidArm.LEFT, -1.05f + headPitchRad * 0.35f, 0.22f, -0.10f, 0.90f);
            visionGoggles$lerpArmPose(this.rightArm, HumanoidArm.RIGHT, -1.00f + headPitchRad * 0.35f + wave, 0.24f, 0.10f, 0.95f);
            return;
        }

        if (player.isUsingItem() && player.getUseItem().getItem() instanceof VisionGogglesItem) {
            ItemStack useStack = player.getUseItem();
            int duration = Math.max(1, useStack.getUseDuration());
            float rawProgress = Mth.clamp(
                    (float) (duration - player.getUseItemRemainingTicks()) / (float) duration,
                    0.0f,
                    1.0f
            );
            float t = Mth.clamp(rawProgress / 0.65f, 0.0f, 1.0f);
            float reach = t * t * (3.0f - 2.0f * t);

            InteractionHand usedHand = player.getUsedItemHand();
            HumanoidArm mainArm = player.getMainArm();
            HumanoidArm usedArmSide = (usedHand == InteractionHand.MAIN_HAND) ? mainArm : mainArm.getOpposite();
            ModelPart usedArm = (usedArmSide == HumanoidArm.RIGHT) ? this.rightArm : this.leftArm;
            visionGoggles$lerpArmToBrow(usedArm, usedArmSide, headPitchRad, reach);

            InteractionHand oppositeHand = (usedHand == InteractionHand.MAIN_HAND)
                    ? InteractionHand.OFF_HAND
                    : InteractionHand.MAIN_HAND;
            if (player.getItemInHand(oppositeHand).isEmpty()) {
                HumanoidArm oppositeArmSide = usedArmSide.getOpposite();
                ModelPart oppositeArm = (oppositeArmSide == HumanoidArm.RIGHT) ? this.rightArm : this.leftArm;
                visionGoggles$lerpArmToBrow(oppositeArm, oppositeArmSide, headPitchRad, reach * 0.90f);
            }
            return;
        }

        ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
        if (helmet.isEmpty() || !(helmet.getItem() instanceof VisionGogglesItem)) {
            return;
        }

        float reach = GogglesFlipAnimator.getEntityArmReach(player.getUUID(), isLocal);
        if (reach <= 0.002f) {
            return;
        }
        HumanoidArm mainArm = player.getMainArm();
        ModelPart arm = (mainArm == HumanoidArm.RIGHT) ? this.rightArm : this.leftArm;
        visionGoggles$lerpArmPose(arm, mainArm, -2.05f + (headPitchRad * 0.45f), -0.35f, -0.22f, reach);
    }

    @Unique
    private static void visionGoggles$lerpArmToBrow(ModelPart arm, HumanoidArm armSide, float headPitchRad, float weight) {
        visionGoggles$lerpArmPose(arm, armSide, -2.20f + (headPitchRad * 0.55f), -0.38f, -0.18f, weight);
    }

    @Unique
    private static void visionGoggles$lerpArmPose(
            ModelPart arm,
            HumanoidArm armSide,
            float targetXRot,
            float targetYBase,
            float targetZBase,
            float weight
    ) {
        float sideSign = (armSide == HumanoidArm.RIGHT) ? 1.0f : -1.0f;
        arm.xRot = Mth.lerp(weight, arm.xRot, targetXRot);
        arm.yRot = Mth.lerp(weight, arm.yRot, targetYBase * sideSign);
        arm.zRot = Mth.lerp(weight, arm.zRot, targetZBase * sideSign);
    }
}
