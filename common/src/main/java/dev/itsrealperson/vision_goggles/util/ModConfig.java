package dev.itsrealperson.vision_goggles.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static CommonConfig common = new CommonConfig();
    public static ClientConfig client = new ClientConfig();
    private static final java.util.Map<ResourceLocation, Float> BATTERY_MAP = new java.util.HashMap<>();

    public static class CommonConfig {
        // ── Battery durations (ticks) ─────────────────────────────────────
        public int nvgDurationTicks      = 6000;
        public int thermalDurationTicks  = 9000;
        public int hydroDurationTicks    = 6000;
        public int biometricDurationTicks = 4500;
        public int modularDurationTicks  = 6000;

        // ── Battery balance ───────────────────────────────────────────────
        /** Global multiplier applied to all active battery drain (1.0 = normal, 2.0 = double drain). */
        public float batteryDrainMultiplier    = 1.0f;
        /** Per-tick standby drain when modules are installed but mode is off. */
        public float batteryStandbyDrain       = 0.001f;
        /** Capacity multiplier applied by the Battery Expansion module. */
        public float batteryExpansionMultiplier = 1.5f;
        /** Per-tick charge rate of the Solar module. */
        public float solarChargeRate           = 1.5f;
        /** Extra battery items that can charge goggles. Format: "item_id|charge_fraction". */
        public List<String> extraBatteryItems  = new ArrayList<>(List.of("minecraft:iron_ingot|0.1", "minecraft:copper_ingot|0.25"));

        // ── Durability ────────────────────────────────────────────────────
        /** Max durability of goggles items. Default: 300. Requires server restart to affect existing items. */
        public int gogglesDurability    = 300;
        /** Fraction of incoming damage transferred to goggles (0.25 = 1/4). Range: 0.0–1.0. */
        public float damageTransferRatio = 0.25f;

        // ── NVG glare (flashlight blinding) ───────────────────────────────
        /** Max range in blocks at which a flashlight can blind NVG users. */
        public int nvgGlareMaxRange            = 25;
        /** Residual glare factor when the NVG user faces away from the light (0.0–1.0). */
        public float nvgGlareBackFactor        = 0.05f;
        /** Glare intensity for generic held-light items (non-mod flashlight). */
        public float nvgGenericLightIntensity  = 1.5f;

        // ── Flashlight ────────────────────────────────────────────────────
        /** Max simultaneous flashlights rendered per client. Range: 1–128. (Values above 32 may degrade GPU performance). */
        public int maxSimultaneousFlashlights  = 16;
    }

    public static class ClientConfig {
        public int nvgColorTheme             = 0; // 0: Green, 1: White, 2: Cyan
        public boolean showCoordinates       = true;
        public boolean showSaturation        = true;
        public boolean showOxygenCounter     = true;
        public boolean showLowDurabilityWarning = true;
    }

    // ─── Serialization helpers ────────────────────────────────────────────────

    /** Serializes the current CommonConfig to JSON (used by ConfigSyncPacket). */
    public static String toCommonJson() {
        return GSON.toJson(common);
    }

    /** Replaces CommonConfig from a JSON string (called by clients on sync). */
    public static void applyCommonJson(String json) {
        try {
            common = GSON.fromJson(json, CommonConfig.class);
            updateBatteryMap();
        } catch (Exception e) {
            System.err.println("[Vision Goggles] Failed to apply synced config: " + e.getMessage());
        }
    }

    // ─── Load / Save ─────────────────────────────────────────────────────────

    public static void load() {
        loadCommon();
        loadClient();
        updateBatteryMap();
    }

    private static <T> T loadConfig(String filename, Class<T> clazz, T fallback) {
        File configFile = Platform.getConfigFolder().resolve(filename).toFile();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                T loaded = GSON.fromJson(reader, clazz);
                return loaded != null ? loaded : fallback;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            saveConfig(filename, fallback);
        }
        return fallback;
    }

    private static void saveConfig(String filename, Object configObj) {
        File configFile = Platform.getConfigFolder().resolve(filename).toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(configObj, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadCommon() {
        common = loadConfig("vision_goggles-common.json", CommonConfig.class, common);
    }

    private static void loadClient() {
        client = loadConfig("vision_goggles-client.json", ClientConfig.class, client);
    }

    public static void saveCommon() { saveConfig("vision_goggles-common.json", common); }
    public static void saveClient() { saveConfig("vision_goggles-client.json", client); }
    public static void save() { saveCommon(); saveClient(); }

    // ─── Battery duration getters ─────────────────────────────────────────────
    public static int getNvgDuration()        { return common.nvgDurationTicks; }
    public static int getThermalDuration()    { return common.thermalDurationTicks; }
    public static int getHydroDuration()      { return common.hydroDurationTicks; }
    public static int getBiometricDuration()  { return common.biometricDurationTicks; }
    public static int getModularDuration()    { return common.modularDurationTicks; }

    // ─── Battery balance getters ──────────────────────────────────────────────
    public static float getBatteryDrainMultiplier()     { return Math.max(0.1f, common.batteryDrainMultiplier); }
    public static float getBatteryStandbyDrain()        { return Math.max(0.0f, common.batteryStandbyDrain); }
    public static float getBatteryExpansionMultiplier() { return Math.max(1.0f, common.batteryExpansionMultiplier); }
    public static float getSolarChargeRate()            { return Math.max(0.1f, common.solarChargeRate); }
    public static List<String> getExtraBatteryItems()   { return common.extraBatteryItems; }

    // ─── Durability getters ───────────────────────────────────────────────────
    public static int getGogglesDurability()    { return Math.max(1, common.gogglesDurability); }
    public static float getDamageTransferRatio() { return Math.max(0.0f, Math.min(1.0f, common.damageTransferRatio)); }

    // ─── NVG glare getters ────────────────────────────────────────────────────
    public static int getNvgGlareMaxRange()           { return Math.max(1, common.nvgGlareMaxRange); }
    public static float getNvgGlareBackFactor()       { return Math.max(0.0f, Math.min(1.0f, common.nvgGlareBackFactor)); }
    public static float getNvgGenericLightIntensity() { return Math.max(0.0f, common.nvgGenericLightIntensity); }

    // ─── Flashlight getter ────────────────────────────────────────────────────
    public static int getMaxFlashlights() { return Math.max(1, Math.min(128, common.maxSimultaneousFlashlights)); }

    // ─── Client getters ───────────────────────────────────────────────────────
    public static int getNvgColorTheme()             { return client.nvgColorTheme; }
    public static boolean shouldShowCoordinates()    { return client.showCoordinates; }
    public static boolean shouldShowSaturation()     { return client.showSaturation; }
    public static boolean shouldShowOxygen()         { return client.showOxygenCounter; }
    public static boolean shouldShowDurabilityWarning() { return client.showLowDurabilityWarning; }

    // ─── Battery map ─────────────────────────────────────────────────────────
    public static void updateBatteryMap() {
        BATTERY_MAP.clear();
        for (String entry : common.extraBatteryItems) {
            try {
                String[] parts = entry.split("\\|");
                String idStr = parts[0].trim();
                float charge = 0.5f;
                if (parts.length > 1) {
                    float val = Float.parseFloat(parts[1].trim());
                    charge = (val > 1.0f) ? val / 100.0f : val;
                }
                ResourceLocation loc = idStr.contains(":") ? new ResourceLocation(idStr) : new ResourceLocation("minecraft", idStr);
                BATTERY_MAP.put(loc, charge);
            } catch (Exception e) {
                System.err.println("[Vision Goggles] Failed to parse config entry: " + entry);
            }
        }
    }

    public static float getBatteryCharge(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        if (stack.getItem() == dev.itsrealperson.vision_goggles.registry.ModItems.NVG_BATTERY.get()) return 0.5f;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (BATTERY_MAP.containsKey(id)) return BATTERY_MAP.get(id);
        return 0.0f;
    }
}
