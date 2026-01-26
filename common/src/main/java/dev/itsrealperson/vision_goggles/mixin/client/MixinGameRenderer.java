package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void vision_goggles$applyZoom(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValue() * VisionRenderer.getZoomMultiplier());
    }
}
