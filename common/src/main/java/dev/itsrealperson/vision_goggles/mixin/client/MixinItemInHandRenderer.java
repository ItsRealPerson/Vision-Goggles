package dev.itsrealperson.vision_goggles.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_goggles.client.FirstPersonEquipRenderer;
import dev.itsrealperson.vision_goggles.client.FirstPersonStationWorkRenderer;
import dev.itsrealperson.vision_goggles.client.GogglesEquipAnimator;
import dev.itsrealperson.vision_goggles.client.GogglesFlipAnimator;
import dev.itsrealperson.vision_goggles.client.StationWorkAnimator;
import dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensPreviewState;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
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
    private float offHandHeight;

    @Shadow
    private float oOffHandHeight;

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
        Minecraft mc = Minecraft.getInstance();
        ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
        boolean hasRealGoggles = !helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem;
        boolean realTransitioning = hasRealGoggles && GogglesFlipAnimator.isLocalTransitioning();
        boolean protoTransitioning = !hasRealGoggles
                && mc.screen == null
                && ProtoTriLensPreviewState.isHeadPreviewEnabled()
                && ProtoTriLensPreviewState.isInGameFlipTransitioning();
        HumanoidArm arm = (hand == InteractionHand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();
        if (arm == player.getMainArm()
                && (realTransitioning || protoTransitioning)
                && mc.options.getCameraType().isFirstPerson()) {
            this.mainHandHeight = 0.0f;
            this.oMainHandHeight = 0.0f;
            ci.cancel();
            return;
        }

        StationWorkAnimator.syncFromRender(player);

        if (player.isScoping() || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }

        if (StationWorkAnimator.isWorking()) {
            if (hand == InteractionHand.MAIN_HAND) {
                FirstPersonStationWorkRenderer.render(player, poseStack, buffer, combinedLight);
                this.mainHandHeight = 0.0f;
                this.oMainHandHeight = 0.0f;
            } else {
                this.offHandHeight = 0.0f;
                this.oOffHandHeight = 0.0f;
            }
            ci.cancel();
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
