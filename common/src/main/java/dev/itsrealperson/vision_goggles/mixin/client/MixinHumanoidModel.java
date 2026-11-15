package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.client.GogglesFlipAnimator;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Animates the player's main arm (and sleeve via PlayerModel.setupAnim) reaching
 * up to the helmet brow/mount during the Vision Goggles flip-down or flip-up transition.
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

        ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
        if (helmet.isEmpty() || !(helmet.getItem() instanceof VisionGogglesItem)) {
            return;
        }

        boolean isLocal = (player == Minecraft.getInstance().player);
        float reach = GogglesFlipAnimator.getEntityArmReach(player.getUUID(), isLocal);
        if (reach <= 0.002f) {
            return;
        }
        HumanoidArm mainArm = player.getMainArm();
        ModelPart arm = (mainArm == HumanoidArm.RIGHT) ? this.rightArm : this.leftArm;
        float sideSign = (mainArm == HumanoidArm.RIGHT) ? 1.0f : -1.0f;

        float targetXRot = -2.05f + (headPitch * ((float) Math.PI / 180.0f) * 0.45f);
        float targetYRot = -0.35f * sideSign;
        float targetZRot = -0.22f * sideSign;

        arm.xRot = Mth.lerp(reach, arm.xRot, targetXRot);
        arm.yRot = Mth.lerp(reach, arm.yRot, targetYRot);
        arm.zRot = Mth.lerp(reach, arm.zRot, targetZRot);
    }
}
