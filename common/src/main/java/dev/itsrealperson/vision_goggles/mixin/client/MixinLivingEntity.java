package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void vision_goggles$virtualNightVision(MobEffect effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == MobEffects.NIGHT_VISION
                && (Object) this == Minecraft.getInstance().player
                && VisionRenderer.shouldApplyNightVisionLight()) {
            cir.setReturnValue(true);
        }
    }
}
