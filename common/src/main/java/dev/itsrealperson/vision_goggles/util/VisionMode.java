package dev.itsrealperson.vision_goggles.util;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum VisionMode {
    NIGHT_VISION(0, "hud.vision_goggles.mode_night", "shaders/post/nvg.json"),
    THERMAL(1, "hud.vision_goggles.mode_thermal", "shaders/post/thermal.json"),
    HYDRO(2, "hud.vision_goggles.mode_hydro", "shaders/post/hydro.json"),
    BIOMETRIC(3, "hud.vision_goggles.mode_bio", "shaders/post/bio.json");

    private final int id;
    private final String translationKey;
    private final ResourceLocation shaderLocation;

    VisionMode(int id, String translationKey, String shaderPath) {
        this.id = id;
        this.translationKey = translationKey;
        this.shaderLocation = new ResourceLocation(Vision_goggles.MOD_ID, shaderPath);
    }

    public int getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    public ResourceLocation getShaderLocation() {
        return shaderLocation;
    }

    public static VisionMode byId(int id) {
        for (VisionMode mode : values()) {
            if (mode.id == id) return mode;
        }
        return NIGHT_VISION; // Default
    }
}
