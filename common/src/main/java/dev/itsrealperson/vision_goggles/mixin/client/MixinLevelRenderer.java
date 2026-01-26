package dev.itsrealperson.vision_goggles.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_goggles.client.VisionWorldRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void vision_goggles$renderWorldEffects(PoseStack poseStack, float p_109601_, long p_109602_, boolean p_109603_, net.minecraft.client.Camera camera, net.minecraft.client.renderer.GameRenderer p_109605_, net.minecraft.client.renderer.LightTexture p_109606_, org.joml.Matrix4f p_109607_, CallbackInfo ci) {
        VisionWorldRenderer.render(poseStack, camera);
    }
}
