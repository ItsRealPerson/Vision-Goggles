package dev.itsrealperson.vision_goggles.util;

import java.util.HashMap;
import java.util.Map;

public enum ModuleType {
    SOLAR(ModConstants.MODULE_SOLAR),
    ZOOM(ModConstants.MODULE_ZOOM),
    SONAR(ModConstants.MODULE_SONAR),
    BATTERY_EXPANSION(ModConstants.MODULE_BATTERY_EXPANSION),
    VITAL_INFO(ModConstants.MODULE_VITAL_INFO),
    ENVIRONMENT(ModConstants.MODULE_ENVIRONMENT),
    FLASHLIGHT(ModConstants.MODULE_FLASHLIGHT);

    private final String id;
    private static final Map<String, ModuleType> BY_ID = new HashMap<>();

    static {
        for (ModuleType type : values()) {
            BY_ID.put(type.id, type);
        }
    }

    ModuleType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static ModuleType byId(String id) {
        return BY_ID.get(id); // Returns null if not found (e.g. if it's a VisionMode string)
    }
}
