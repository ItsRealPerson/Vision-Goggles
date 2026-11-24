package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.client.GunHeatTracker;
import dev.itsrealperson.vision_goggles.client.VisionShaderManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {
    @Inject(method = "play", at = @At("HEAD"))
    private void vision_goggles$onPlay(SoundInstance sound, CallbackInfo ci) {
        ResourceLocation loc = sound.getLocation();
        String ns = loc.getNamespace(), path = loc.getPath();
        if (!java.util.Set.of("tacz", "pointblank", "vic", "cgm", "superbwarfare", "jeg").contains(ns)) return;
        for (String b : new String[]{"reload", "dry", "aim", "draw", "put_away", "bolt", "click", "inspect", "equip"}) if (path.contains(b)) return;
        boolean suppressed = path.contains("silence") || path.contains("suppres");
        for (String g : new String[]{"shoot", "fire", "gun", "shot", "blast", "rifle", "pistol", "sniper"}) {
            if (path.contains(g)) {
                if (!suppressed) {
                    VisionShaderManager.gunFlash(sound.getX(), sound.getY(), sound.getZ(), true);
                }
                GunHeatTracker.onGunshot(sound.getX(), sound.getY(), sound.getZ());
                return;
            }
        }
    }
}
