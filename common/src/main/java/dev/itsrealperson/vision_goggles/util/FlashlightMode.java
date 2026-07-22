package dev.itsrealperson.vision_goggles.util;

/**
 * Defines the available flashlight beam modes.
 * Each mode has independent shader parameters (cone, range, intensity)
 * and separate NVG glare parameters.
 */
public enum FlashlightMode {
    // Narrow focused beam — long range, very blinding for NVG
    FOCUSED(0, "focused",
            0.93f, 0.97f, // coneInner, coneOuter (very tight)
            32.0f,        // range (blocks)
            1.0f,         // shader intensity
            0.90, 2.0f),  // glare threshold, glare intensity

    // Wide flood beam — medium range, covers a large area
    WIDE(1, "wide",
            0.65f, 0.82f, // coneInner, coneOuter (wide)
            16.0f,        // range
            0.85f,        // slightly dimmer per-unit
            0.65, 1.0f),  // wider glare cone, less blinding

    // Short range close-range burst — very wide, short, bright
    CLOSE(2, "close",
            0.42f, 0.68f, // coneInner, coneOuter (very wide)
            8.0f,         // short range
            1.3f,         // brighter but short
            0.45, 0.6f);  // very wide glare, low intensity at distance

    public final int id;
    public final String name;
    public final float coneInner;
    public final float coneOuter;
    public final float range;
    public final float intensity;
    // NVG glare parameters
    public final double glareThreshold;
    public final float glareIntensity;

    FlashlightMode(int id, String name,
                   float coneInner, float coneOuter,
                   float range, float intensity,
                   double glareThreshold, float glareIntensity) {
        this.id = id;
        this.name = name;
        this.coneInner = coneInner;
        this.coneOuter = coneOuter;
        this.range = range;
        this.intensity = intensity;
        this.glareThreshold = glareThreshold;
        this.glareIntensity = glareIntensity;
    }

    public FlashlightMode next() {
        FlashlightMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static FlashlightMode byId(int id) {
        for (FlashlightMode m : values()) {
            if (m.id == id) return m;
        }
        return FOCUSED;
    }
}
