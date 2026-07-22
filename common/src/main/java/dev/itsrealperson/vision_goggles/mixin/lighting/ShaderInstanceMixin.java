package dev.itsrealperson.vision_goggles.mixin.lighting;

import dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms;
import dev.itsrealperson.vision_goggles.client.lighting.ShaderRewriter;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public class ShaderInstanceMixin {

    @ModifyVariable(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Ljava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;)V", at = @At("HEAD"), argsOnly = true, index = 1)
    private static ResourceProvider vision$wrapResourceProvider(ResourceProvider provider) {
        return ShaderRewriter.wrap(provider);
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void vision$uploadFlashlightUniforms(CallbackInfo ci) {
        FlashlightUniforms.applyToVanilla((ShaderInstance) (Object) this);
    }
}
