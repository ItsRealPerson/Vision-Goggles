package dev.itsrealperson.vision_goggles.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_goggles.client.GogglesFlipAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void vision_goggles$hideHandDuringFlip(
            AbstractClientPlayer player,
            float partialTicks,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight,
            CallbackInfo ci
    ) {
        HumanoidArm arm = (hand == InteractionHand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();
        if (arm == player.getMainArm()
                && GogglesFlipAnimator.isLocalTransitioning()
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            ci.cancel();
        }
    }
}
