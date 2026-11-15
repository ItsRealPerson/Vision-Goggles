package dev.itsrealperson.vision_goggles.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_goggles.client.ThermalEntityPainter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

    @Inject(method = "render", at = @At("HEAD"))
    private <E extends Entity> void vision_goggles$onRenderEntityHead(
            E entity, double x, double y, double z, float rotationYaw, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity instanceof LivingEntity living) {
            String entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
            boolean isHostile = living instanceof Enemy || !living.getType().getCategory().isFriendly();
            Integer customColor = dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge.hookThermalColor(entityTypeId, isHostile);
            if (customColor != null && customColor != -1) {
                ThermalEntityPainter.setHasPerEntityCustomColor(true);
            }
        }
    }
}
