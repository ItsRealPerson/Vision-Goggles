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
                        int modeId = helmet.getOrCreateTag().getInt(ModConstants.TAG_MODE);
                        VisionMode mode = VisionMode.byId(modeId);
                        
                        if (mode == VisionMode.HYDRO) {
                            RenderSystem.setShaderFogStart(viewDistance * 0.5F);
                            RenderSystem.setShaderFogEnd(viewDistance * 5.0F); // Push fog way back
                        }
                    }
                }
            }
        }
    }
}