package dev.itsrealperson.vision_goggles.mixin.sodium;

import dev.itsrealperson.vision_goggles.client.lighting.SodiumShaderPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.ShaderParser", remap = false)
public class SodiumShaderParserMixin {
    @Inject(method = "parseShader", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void vision$injectFlashlight(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<String> cir) {
        String ret = cir.getReturnValue();
        if (ret == null) return;
        
        String patched = SodiumShaderPatcher.patch(ret);
        if (patched != ret) {
            cir.setReturnValue(patched);
        }
    }
}
