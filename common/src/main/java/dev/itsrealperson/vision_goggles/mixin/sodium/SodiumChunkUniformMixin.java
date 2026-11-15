package dev.itsrealperson.vision_goggles.mixin.sodium;

import dev.itsrealperson.vision_goggles.client.lighting.SodiumFlashlightUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(targets = {
    "me.jellysquid.mods.sodium.client.gl.shader.GlProgram",
    "org.embeddedt.embeddium.client.gl.shader.GlProgram",
    "embeddedt.embeddium.client.gl.shader.GlProgram",
    "com.rubidium.client.gl.shader.GlProgram"
}, remap = false)
public abstract class SodiumChunkUniformMixin {

    @Unique
    private static final Map<Class<?>, Field> VISION$HANDLE_FIELDS = new ConcurrentHashMap<>();
    @Unique
    private static volatile Method vision$cachedHandleMethod;
    @Unique
    private static volatile boolean vision$methodResolved = false;

    @Inject(method = "<init>*", at = @At("RETURN"), require = 0, remap = false)
    private void vision$onProgramInit(CallbackInfo ci) {
        SodiumFlashlightUniforms.clearCache();
    }

    @Inject(method = "bind", at = @At("TAIL"), require = 0, remap = false)
    private void vision$uploadFlashlightUniforms(CallbackInfo ci) {
        try {
            int handleId = -1;
            Class<?> cls = this.getClass();
            Method m = vision$cachedHandleMethod;
            if (!vision$methodResolved) {
                try {
                    m = cls.getMethod("handle");
                    m.setAccessible(true);
                    vision$cachedHandleMethod = m;
                } catch (Exception ignored) {
                }
                vision$methodResolved = true;
            }
            if (m != null) {
                handleId = (int) m.invoke(this);
            } else {
                Field f = VISION$HANDLE_FIELDS.computeIfAbsent(cls, c -> {
                    try {
                        Field field = c.getField("handle");
                        field.setAccessible(true);
                        return field;
                    } catch (Exception ex) {
                        return null;
                    }
                });
                if (f != null) {
                    handleId = f.getInt(this);
                }
            }
            if (handleId != -1) {
                SodiumFlashlightUniforms.upload(handleId);
            }
        } catch (Exception ignored) {}
    }
}
