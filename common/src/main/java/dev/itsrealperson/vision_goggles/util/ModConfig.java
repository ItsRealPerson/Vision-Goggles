package dev.itsrealperson.vision_goggles.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
    public static ConfigData data = new ConfigData();
    private static final Map<ResourceLocation, Float> BATTERY_MAP = new HashMap<>();

    public static class ConfigData {
        public int nvgDurationTicks = 6000;
        public int thermalDurationTicks = 9000;
        public int hydroDurationTicks = 6000;
        public int biometricDurationTicks = 4500;
        public int modularDurationTicks = 6000;
        public List<String> extraBatteryItems = new ArrayList<>(List.of("minecraft:iron_ingot|0.1", "minecraft:copper_ingot|0.25"));
    }

    public static void load() {
        Path configPath = Platform.getConfigFolder().resolve("vision_goggles.json");
        File configFile = configPath.toFile();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                data = GSON.fromJson(reader, ConfigData.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
        updateBatteryMap();
    }

    public static void save() {
        Path configPath = Platform.getConfigFolder().resolve("vision_goggles.json");
        try (FileWriter writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateFromSync(int nvg, int thermal, int hydro, int bio, int modular, List<String> extra) {
        data.nvgDurationTicks = nvg;
        data.thermalDurationTicks = thermal;
        data.hydroDurationTicks = hydro;
        data.biometricDurationTicks = bio;
        data.modularDurationTicks = modular;
        data.extraBatteryItems = extra;
        updateBatteryMap();
    }

    public static void updateBatteryMap() {
        BATTERY_MAP.clear();
        for (String entry : data.extraBatteryItems) {
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

    public static int getNvgDuration() { return data.nvgDurationTicks; }
    public static int getThermalDuration() { return data.thermalDurationTicks; }
    public static int getHydroDuration() { return data.hydroDurationTicks; }
    public static int getBiometricDuration() { return data.biometricDurationTicks; }
    public static int getModularDuration() { return data.modularDurationTicks; }
    public static List<String> getExtraBatteryItems() { return data.extraBatteryItems; }

    public static float getBatteryCharge(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        if (stack.getItem() == dev.itsrealperson.vision_goggles.registry.ModItems.NVG_BATTERY.get()) {
            return 0.5f;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (BATTERY_MAP.containsKey(id)) return BATTERY_MAP.get(id);
        return 0.0f;
    }
}