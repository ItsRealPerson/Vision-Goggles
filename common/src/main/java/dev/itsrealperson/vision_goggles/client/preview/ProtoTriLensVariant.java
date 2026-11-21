package dev.itsrealperson.vision_goggles.client.preview;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public enum ProtoTriLensVariant {
    NVG("nvg", "NVG", "Tri-Lens NVG (Splinter)", 0xFF3FD968),
    THERMAL("thermal", "THERMAL", "Tri-Lens Thermal (ENVG-B)", 0xFFFF8833),
    BIO("bio", "BIO", "Tri-Lens Biometric", 0xFFE64545),
    HYDRO("hydro", "HYDRO", "Tri-Lens Hydro", 0xFF33D6CC),
    MODULAR("modular", "MODULAR", "Tri-Lens Modular CNC", 0xFF66B2FF);

    private final String id;
    private final String shortLabel;
    private final String displayName;
    private final int accentColor;
    private final ResourceLocation geoModel;
    private final ResourceLocation baseTexture;
    private final ResourceLocation glowTexture;

    ProtoTriLensVariant(String id, String shortLabel, String displayName, int accentColor) {
        this.id = id;
        this.shortLabel = shortLabel;
        this.displayName = displayName;
        this.accentColor = accentColor;
        this.geoModel = new ResourceLocation(Vision_goggles.MOD_ID, "geo/item/proto_tri_lens_" + id + ".geo.json");
        this.baseTexture = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/proto_tri_lens_" + id + ".png");
        this.glowTexture = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/proto_tri_lens_" + id + "_glow.png");
    }

    public String getId() {
        return id;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public ResourceLocation getGeoModel() {
        return geoModel;
    }

    public ResourceLocation getBaseTexture() {
        return baseTexture;
    }

    public ResourceLocation getGlowTexture() {
        return glowTexture;
    }

    public static ProtoTriLensVariant fromIdOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String norm = raw.trim().toLowerCase(Locale.ROOT);
        for (ProtoTriLensVariant v : values()) {
            if (v.id.equals(norm)) {
                return v;
            }
        }
        return null;
    }
}
