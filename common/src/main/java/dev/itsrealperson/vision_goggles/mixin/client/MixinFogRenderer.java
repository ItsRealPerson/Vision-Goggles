package dev.itsrealperson.vision_goggles.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void vision_goggles$onSetupFog(Camera camera, FogRenderer.FogMode fogMode, float viewDistance, boolean thickFog, float partialTick, CallbackInfo ci) {
        if (camera.getFluidInCamera() == FogType.WATER) {
            Entity entity = camera.getEntity();
            if (entity instanceof Player player) {
                ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
                if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem) {
                    if (helmet.getOrCreateTag().getBoolean(ModConstants.TAG_ACTIVE)) {
                        VisionMode mode = VisionGogglesItem.getActiveVisionMode(helmet);
                        
                        if (mode == VisionMode.HYDRO) {
                            // Depth-dependent underwater visibility: crystal clear near surface (Y>=56 -> ~140m),
                            // tapering with abyssal depth & pressure down to Y=20 (~75m), Y=0 (~48m), Y<=-40 (~24m).
                            double y = player.getY();
                            float fogEnd;
                            if (y >= 56.0) {
                                fogEnd = 140.0F;
                            } else if (y >= 20.0) {
                                float t = (float) ((y - 20.0) / 36.0);
                                fogEnd = 75.0F + t * (140.0F - 75.0F);
                            } else if (y >= 0.0) {
                                float t = (float) (y / 20.0);
                                fogEnd = 48.0F + t * (75.0F - 48.0F);
                            } else if (y > -40.0) {
                                float t = (float) ((y + 40.0) / 40.0);
                                fogEnd = 24.0F + t * (48.0F - 24.0F);
                            } else {
                                fogEnd = 24.0F;
                            }
                            float fogStart = Math.min(fogEnd * 0.22F, 18.0F);
                            RenderSystem.setShaderFogStart(fogStart);
                            RenderSystem.setShaderFogEnd(fogEnd);
                        }
                    }
                }
            }
        }
    }
}
