package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void vision_goggles$onMouseScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (VisionRenderer.onMouseScroll(yOffset)) {
            ci.cancel();
        }
    }
}
