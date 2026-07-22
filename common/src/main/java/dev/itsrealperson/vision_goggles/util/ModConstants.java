package dev.itsrealperson.vision_goggles.util;

public class ModConstants {
    // NBT Keys
    public static final String TAG_BATTERY = "nvg_battery";
    public static final String TAG_ACTIVE = "nvg_active";
    public static final String TAG_MODE = "vision_mode";
    public static final String TAG_SONAR_MODE = "sonar_mode";
    public static final String TAG_MODULES = "Modules";
    public static final String TAG_LAST_SYNC = "nvg_last_sync"; // For network optimization

    // Module Names (Legacy Strings for Migration)
    public static final String MODULE_SOLAR = "SOLAR";
    public static final String MODULE_ZOOM = "ZOOM";
    public static final String MODULE_SONAR = "SONAR";
    public static final String MODULE_BATTERY_EXPANSION = "BATTERY_EXPANSION";
    public static final String MODULE_VITAL_INFO = "VITAL_INFO";
    public static final String MODULE_ENVIRONMENT = "ENVIRONMENT";
    public static final String MODULE_THERMAL = "THERMAL";
    public static final String MODULE_SPAWN_SECURITY = "SPAWN_SECURITY";
    public static final String MODULE_CHUNK_VIEWER = "CHUNK_VIEWER";
    public static final String MODULE_FLASHLIGHT = "FLASHLIGHT";
    public static final String TAG_FLASHLIGHT_ACTIVE = "flashlight_active";
    public static final String TAG_FLASHLIGHT_MODE = "flashlight_mode"; // int: FlashlightMode.id


    // Module ResourceLocations (New format for v1.1.0)
    public static final String ID_SOLAR = "vision_goggles:solar";
    public static final String ID_ZOOM = "vision_goggles:zoom";
    public static final String ID_SONAR = "vision_goggles:sonar";
    public static final String ID_BATTERY_EXPANSION = "vision_goggles:battery_expansion";
    public static final String ID_VITAL_INFO = "vision_goggles:vital_info";
    public static final String ID_ENVIRONMENT = "vision_goggles:environment";
    public static final String ID_SPAWN_SECURITY = "vision_goggles:spawn_security";
    public static final String ID_CHUNK_VIEWER = "vision_goggles:chunk_viewer";
    public static final String ID_FLASHLIGHT = "vision_goggles:flashlight";
    
    // Vision Mode ResourceLocations
    public static final String ID_NIGHT_VISION = "vision_goggles:night_vision";
    public static final String ID_THERMAL = "vision_goggles:thermal";
    public static final String ID_HYDRO = "vision_goggles:hydro";
    public static final String ID_BIOMETRIC = "vision_goggles:biometric";
}
