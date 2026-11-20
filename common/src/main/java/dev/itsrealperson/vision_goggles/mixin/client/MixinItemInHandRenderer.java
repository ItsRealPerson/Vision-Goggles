package dev.itsrealperson.vision_goggles.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_goggles.client.FirstPersonEquipRenderer;
import dev.itsrealperson.vision_goggles.client.GogglesEquipAnimator;
import dev.itsrealperson.vision_goggles.client.GogglesFlipAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Shadow
    public abstract void renderItem(
            LivingEntity entity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int seed
    );

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
            this.mainHandHeight = 0.0f;
            this.oMainHandHeight = 0.0f;
            ci.cancel();
            return;
        }

        if (player.isScoping() || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }

        GogglesEquipAnimator.syncFromRender(player, partialTicks);

        if (GogglesEquipAnimator.shouldRenderCustomEquip(hand)) {
            FirstPersonEquipRenderer.render(
                    player,
                    hand,
                    arm,
                    stack,
                    equippedProgress,
                    poseStack,
                    buffer,
                    combinedLight,
                    this::renderItem
            );
            ci.cancel();
            return;
        }

        if (GogglesEquipAnimator.shouldHideOppositeEmptyHand(hand, stack)) {
            ci.cancel();
        }
    }
}
