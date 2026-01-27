package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.registry.ModDataComponents;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void vision_goggles$instantCutoff(LivingEntity livingEntity, float partialTick, CallbackInfoReturnable<Float> cir) {
        if (livingEntity instanceof Player player) {
            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem) {
                boolean isActive = Objects.requireNonNullElse(helmet.get(ModDataComponents.ACTIVE.get()), false);
                if (!isActive) {
                    cir.setReturnValue(0.0f);
                }
            }
        }
    }
}