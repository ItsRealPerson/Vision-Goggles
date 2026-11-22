package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GogglesRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public GogglesRenderLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack matrixStack, MultiBufferSource buffer, int packedLight, T livingEntity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(livingEntity instanceof Player player)) return;

        // 1. Render Vest Mounted Flashlight on torso if equipped
        ItemStack flashlight = PlatformMethods.getEquippedFlashlight(player);
        if (!flashlight.isEmpty() && this.getParentModel() instanceof HumanoidModel<?> baseModel) {
            matrixStack.pushPose();
            baseModel.body.translateAndRotate(matrixStack);

            // Positioned securely on the chest with head/switch on top and lens facing directly forward into the world
            matrixStack.translate(0.18f, 0.20f, -0.16f);
            matrixStack.scale(0.32f, 0.32f, 0.32f);
            matrixStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0f));

            Minecraft.getInstance().getItemRenderer().renderStatic(
                flashlight,
                net.minecraft.world.item.ItemDisplayContext.NONE,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                matrixStack,
                buffer,
                player.level(),
                0
            );
            matrixStack.popPose();
        }

        // 2. Render Goggles on head
        ItemStack stack = PlatformMethods.getEquippedHelmet(player);
        boolean hasRealGoggles = !stack.isEmpty() && stack.getItem() instanceof VisionGogglesItem;
        if (!hasRealGoggles) return;

        matrixStack.pushPose();

        if (this.getParentModel() instanceof HumanoidModel<?> baseModel) {
            baseModel.head.translateAndRotate(matrixStack);
        }
        matrixStack.scale(-1.0f, -1.0f, 1.0f);
        matrixStack.translate(0.0f, -24.0f / 16.0f, 0.0f);

        boolean isActive = false;
        if (stack.hasTag() && stack.getTag().getBoolean(ModConstants.TAG_ACTIVE)) {
            float batt = stack.getTag().contains(ModConstants.TAG_BATTERY) ? stack.getTag().getFloat(ModConstants.TAG_BATTERY) : 1.0f;
            if (batt > 0.0f) {
                isActive = true;
            }
        }

        boolean isLocalPlayer = (player == Minecraft.getInstance().player);
        float progress = isLocalPlayer
            ? GogglesFlipAnimator.getLocalProgress()
            : GogglesFlipAnimator.getEntityProgress(player.getUUID(), isActive);

        dev.itsrealperson.vision_goggles.client.renderer.TriLensManualRenderer.render(
            stack, (VisionGogglesItem) stack.getItem(), matrixStack, buffer, progress, partialTicks, packedLight,
            isActive && progress >= 0.98f, 1.0f);

        matrixStack.popPose();
    }
}
