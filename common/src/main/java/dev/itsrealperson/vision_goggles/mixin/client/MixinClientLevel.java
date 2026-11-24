package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.client.VisionShaderManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevel {
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void vision_goggles$onAddEntity(int id, Entity entity, CallbackInfo ci) {
        String tk = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        if (tk.startsWith("tacz:") || tk.contains("bullet")) VisionShaderManager.gunFlash(entity.getX(), entity.getY(), entity.getZ(), true);
    }
}
