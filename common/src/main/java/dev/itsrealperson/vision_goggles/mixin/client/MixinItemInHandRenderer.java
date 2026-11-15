package dev.itsrealperson.vision_goggles.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_goggles.client.GogglesFlipAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void vision_goggles$hideHandDuringFlip(
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffer,
            LocalPlayer playerEntity,
            int combinedLight,
            CallbackInfo ci
    ) {
        if (GogglesFlipAnimator.isLocalTransitioning() && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            ci.cancel();
        }
    }
}
