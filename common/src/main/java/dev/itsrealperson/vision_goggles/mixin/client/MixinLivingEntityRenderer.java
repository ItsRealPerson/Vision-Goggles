package dev.itsrealperson.vision_goggles.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void vision_goggles$renderSonarOutline(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (VisionRenderer.isSonarActive()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && entity != mc.player && mc.player.distanceToSqr(entity) < 400) { // 20 blocks
                // We've already handled entity rendering in a similar way for HeatSilhouetteLayer
                // But sonar needs to render through walls.
                // This is a bit more complex without full outline logic, but for now we rely on the
                // fact that it pulses periodically.
            }
        }
    }
}
