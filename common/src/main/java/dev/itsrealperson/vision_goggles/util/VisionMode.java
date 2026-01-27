package dev.itsrealperson.vision_goggles.util;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum VisionMode {
    NIGHT_VISION(0, "night", "nvg"),
    THERMAL(1, "thermal", "thermal"),
    HYDRO(2, "hydro", "hydro"),
    BIOMETRIC(3, "bio", "bio");

    private final int id;
    private final String name;
    private final ResourceLocation shaderLocation;

    VisionMode(int id, String name, String shaderPath) {
        this.id = id;
        this.name = name;
        this.shaderLocation = ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "shaders/post/" + shaderPath + ".json");
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public ResourceLocation getShaderLocation() { return shaderLocation; }

    public Component getDisplayName() {
        return Component.translatable("hud.vision_goggles.mode_" + name);
    }

    public static VisionMode byId(int id) {
        for (VisionMode mode : values()) {
            if (mode.id == id) return mode;
        }
        return NIGHT_VISION;
    }
}
