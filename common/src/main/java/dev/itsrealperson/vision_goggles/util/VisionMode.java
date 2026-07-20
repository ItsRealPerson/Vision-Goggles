package dev.itsrealperson.vision_goggles.util;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum VisionMode {
    NIGHT_VISION(0, "hud.vision_goggles.mode_night", "shaders/post/nvg.json", ModConstants.ID_NIGHT_VISION),
    THERMAL(1, "hud.vision_goggles.mode_thermal", "shaders/post/thermal.json", ModConstants.ID_THERMAL),
    HYDRO(2, "hud.vision_goggles.mode_hydro", "shaders/post/hydro.json", ModConstants.ID_HYDRO),
    BIOMETRIC(3, "hud.vision_goggles.mode_bio", "shaders/post/bio.json", ModConstants.ID_BIOMETRIC);

    private final int id;
    private final String translationKey;
    private final ResourceLocation shaderLocation;
    private final ResourceLocation moduleLocation;

    VisionMode(int id, String translationKey, String shaderPath, String moduleLocation) {
        this.id = id;
        this.translationKey = translationKey;
        this.shaderLocation = new ResourceLocation(Vision_goggles.MOD_ID, shaderPath);
        this.moduleLocation = new ResourceLocation(moduleLocation);
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

    public ResourceLocation getModuleLocation() {
        return moduleLocation;
    }

    public static VisionMode byId(int id) {
        for (VisionMode mode : values()) {
            if (mode.id == id) return mode;
        }
        return NIGHT_VISION; // Default
    }

    public static VisionMode byLocation(String location) {
        for (VisionMode mode : values()) {
            if (mode.moduleLocation.toString().equals(location)) return mode;
        }
        // Fallback for legacy names
        try {
            return valueOf(location);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
