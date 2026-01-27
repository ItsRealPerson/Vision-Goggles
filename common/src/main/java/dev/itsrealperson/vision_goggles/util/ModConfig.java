package dev.itsrealperson.vision_goggles.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, Float> EXTRA_BATTERIES = new HashMap<>();
    private static Path configPath;
    
    public static ConfigData data = new ConfigData();

    public static class ConfigData {
        public int nvgDurationTicks = 6000;
        public int thermalDurationTicks = 9000;
        public int hydroDurationTicks = 6000;
        public int biometricDurationTicks = 4500;
        public int modularDurationTicks = 6000;
        public int nvgColorTheme = 0; // 0: Green, 1: White, 2: Cyan
        public List<String> extraBatteryItems = new ArrayList<>(List.of("minecraft:iron_ingot|0.1", "minecraft:copper_ingot|0.25"));
    }

    public static void load() {
        configPath = Platform.getConfigFolder().resolve("vision_goggles.json");
        File file = configPath.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                data = GSON.fromJson(reader, ConfigData.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
        updateBatteryMap();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateFromSync(int nvg, int thermal, int hydro, int bio, int modular, int theme, List<String> batteries) {
        data.nvgDurationTicks = nvg;
        data.thermalDurationTicks = thermal;
        data.hydroDurationTicks = hydro;
        data.biometricDurationTicks = bio;
        data.modularDurationTicks = modular;
        data.nvgColorTheme = theme;
        data.extraBatteryItems = batteries;
        updateBatteryMap();
    }

    public static void updateBatteryMap() {
        EXTRA_BATTERIES.clear();
        for (String entry : data.extraBatteryItems) {
            try {
                String[] parts = entry.split(Pattern.quote("|"));
                if (parts.length == 2) {
                    String idStr = parts[0].trim();
                    float charge = Float.parseFloat(parts[1].trim());
                    ResourceLocation loc = ResourceLocation.parse(idStr);
                    EXTRA_BATTERIES.put(loc, charge);
                }
            } catch (Exception ignored) {}
        }
    }

    public static float getBatteryCharge(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() == dev.itsrealperson.vision_goggles.registry.ModItems.NVG_BATTERY.get()) return 0.5f;
        
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return EXTRA_BATTERIES.getOrDefault(id, 0.0f);
    }

    public static int getNvgDuration() { return data.nvgDurationTicks; }
    public static int getThermalDuration() { return data.thermalDurationTicks; }
    public static int getHydroDuration() { return data.hydroDurationTicks; }
    public static int getBiometricDuration() { return data.biometricDurationTicks; }
    public static int getModularDuration() { return data.modularDurationTicks; }
    public static int getNvgColorTheme() { return data.nvgColorTheme; }
    public static List<String> getExtraBatteryItems() { return data.extraBatteryItems; }
}