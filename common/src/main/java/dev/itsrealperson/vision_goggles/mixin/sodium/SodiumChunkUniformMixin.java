package dev.itsrealperson.vision_goggles.mixin.sodium;

import dev.itsrealperson.vision_goggles.client.lighting.SodiumFlashlightUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.GlProgram", remap = false)
public abstract class SodiumChunkUniformMixin {

    @Inject(method = "bind", at = @At("TAIL"), require = 0, remap = false)
    private void vision$uploadFlashlightUniforms(CallbackInfo ci) {
        try {
            int handleId = -1;
            try {
                java.lang.reflect.Method m = this.getClass().getMethod("handle");
                handleId = (int) m.invoke(this);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field f = this.getClass().getField("handle");
                    handleId = f.getInt(this);
                } catch (Exception ex) {}
            }
            if (handleId != -1) {
                SodiumFlashlightUniforms.upload(handleId);
            }
        } catch (Exception e) {}
    }
}
