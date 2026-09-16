package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import dev.itsrealperson.vision_goggles.client.VisionShaderManager;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
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
     * Process our shader RIGHT AFTER the world is rendered, BEFORE any GUI/HUD is drawn.
     * This ensures:
     *   - World view: has NVG/thermal effect applied ✓
     *   - Hotbar, hearts, HUD: rendered on top, NOT tinted ✓
     *   - Pause menu, inventory, any screen: NOT tinted ✓
     *
     * Injecting after renderLevel() also means this only fires when a world is loaded,
     * so it never runs in the main menu or loading screens.
     *
     * require = 0: if the target method signature ever changes, fail silently rather than crash.
     */
    @Inject(method = "render",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
                     shift = At.Shift.AFTER),
            require = 0)
    private void vision_goggles$processOurShader(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        VisionShaderManager.processShader(partialTick);
        VisionShaderManager.processFlashlight(partialTick);
    }


    /**
     * When the window is resized (e.g. windowed ↔ fullscreen toggle), resize our PostChain
     * framebuffers to match. Since our chain is NOT in GameRenderer.postEffect, the vanilla
     * resize code won't touch it — we handle it here to avoid the mismatched-resolution glitch.
     */
    @Inject(method = "resize", at = @At("TAIL"))
    private void vision_goggles$resizeOurChain(int width, int height, CallbackInfo ci) {
        VisionShaderManager.onResize(width, height);
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