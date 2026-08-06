package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import dev.itsrealperson.vision_goggles.client.VisionShaderManager;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    /**
     * Per-frame injection: called at the end of GameRenderer.render() just before
     * the PostChain is processed. This ensures that mods like Superb Warfare 0.8.9
     * (which temporarily hijack postEffect for scope/drone camera passes) cannot
     * permanently leave postEffect as null, causing Vision Goggles shaders to disappear.
     *
     * We use TAIL (end of render()) so all other mods have already done their frame
     * setup and our shader gets the final say before the screen is presented.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void vision_goggles$ensureShaderPerFrame(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            VisionShaderManager.ensureShaderActive(mc);
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void vision_goggles$applyZoom(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(cir.getReturnValue() * VisionRenderer.getZoomMultiplier());
    }

    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void vision_goggles$instantCutoff(LivingEntity livingEntity, float partialTick, CallbackInfoReturnable<Float> cir) {
        if (livingEntity instanceof Player player) {
            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem) {
                // If goggles are equipped but NOT active, force 0.0 scale (instant off)
                if (!helmet.getOrCreateTag().getBoolean(ModConstants.TAG_ACTIVE)) {
                    cir.setReturnValue(0.0f);
                }
            }
        }
    }
}