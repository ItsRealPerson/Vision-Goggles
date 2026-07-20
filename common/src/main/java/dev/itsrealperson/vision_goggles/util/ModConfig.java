package dev.itsrealperson.vision_goggles.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static CommonConfig common = new CommonConfig();
    public static ClientConfig client = new ClientConfig();
    private static final Map<ResourceLocation, Float> BATTERY_MAP = new HashMap<>();

    public static class CommonConfig {
        public int nvgDurationTicks = 6000;
        public int thermalDurationTicks = 9000;
        public int hydroDurationTicks = 6000;
        public int biometricDurationTicks = 4500;
        public int modularDurationTicks = 6000;
        public List<String> extraBatteryItems = new ArrayList<>(List.of("minecraft:iron_ingot|0.1", "minecraft:copper_ingot|0.25"));
    }

    public static class ClientConfig {
        public int nvgColorTheme = 0; // 0: Green, 1: White, 2: Cyan
        public boolean showCoordinates = true;
        public boolean showSaturation = true;
        public boolean showOxygenCounter = true;
        public boolean showLowDurabilityWarning = true;
    }

    public static void load() {
        loadCommon();
        loadClient();
        updateBatteryMap();
    }

    private static <T> T loadConfig(String filename, Class<T> clazz, T fallback) {
        File configFile = Platform.getConfigFolder().resolve(filename).toFile();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                return GSON.fromJson(reader, clazz);
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

    public static void saveCommon() {
        saveConfig("vision_goggles-common.json", common);
    }

    public static void saveClient() {
        saveConfig("vision_goggles-client.json", client);
    }

    public static void save() {
        saveCommon();
        saveClient();
    }

    public static void updateFromSync(int nvg, int thermal, int hydro, int bio, int modular, List<String> extra) {
        common.nvgDurationTicks = nvg;
        common.thermalDurationTicks = thermal;
        common.hydroDurationTicks = hydro;
        common.biometricDurationTicks = bio;
        common.modularDurationTicks = modular;
        common.extraBatteryItems = extra;
        updateBatteryMap();
    }

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

    public static int getNvgDuration() { return common.nvgDurationTicks; }
    public static int getThermalDuration() { return common.thermalDurationTicks; }
    public static int getHydroDuration() { return common.hydroDurationTicks; }
    public static int getBiometricDuration() { return common.biometricDurationTicks; }
    public static int getModularDuration() { return common.modularDurationTicks; }
    public static int getNvgColorTheme() { return client.nvgColorTheme; }
    public static List<String> getExtraBatteryItems() { return common.extraBatteryItems; }

    public static boolean shouldShowCoordinates() { return client.showCoordinates; }
    public static boolean shouldShowSaturation() { return client.showSaturation; }
    public static boolean shouldShowOxygen() { return client.showOxygenCounter; }
    public static boolean shouldShowDurabilityWarning() { return client.showLowDurabilityWarning; }

    public static float getBatteryCharge(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        if (stack.getItem() == dev.itsrealperson.vision_goggles.registry.ModItems.NVG_BATTERY.get()) return 0.5f;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (BATTERY_MAP.containsKey(id)) return BATTERY_MAP.get(id);
        return 0.0f;
    }
}
