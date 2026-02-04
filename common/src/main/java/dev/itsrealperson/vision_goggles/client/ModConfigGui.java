package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.util.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import dev.architectury.platform.Platform;

import java.util.List;

public class ModConfigGui {
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.vision_goggles.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.vision_goggles.general"));

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.nvg_duration"), ModConfig.getNvgDuration())
                .setDefaultValue(6000)
                .setTooltip(Component.translatable("config.vision_goggles.nvg_duration.tooltip"))
                .setSaveConsumer(newValue -> ModConfig.data.nvgDurationTicks = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.thermal_duration"), ModConfig.getThermalDuration())
                .setDefaultValue(9000)
                .setTooltip(Component.translatable("config.vision_goggles.thermal_duration.tooltip"))
                .setSaveConsumer(newValue -> ModConfig.data.thermalDurationTicks = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.hydro_duration"), ModConfig.getHydroDuration())
                .setDefaultValue(6000)
                .setTooltip(Component.translatable("config.vision_goggles.hydro_duration.tooltip"))
                .setSaveConsumer(newValue -> ModConfig.data.hydroDurationTicks = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.bio_duration"), ModConfig.getBiometricDuration())
                .setDefaultValue(4500)
                .setTooltip(Component.translatable("config.vision_goggles.bio_duration.tooltip"))
                .setSaveConsumer(newValue -> ModConfig.data.biometricDurationTicks = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.modular_duration"), ModConfig.getModularDuration())
                .setDefaultValue(6000)
                .setTooltip(Component.translatable("config.vision_goggles.modular_duration.tooltip"))
                .setSaveConsumer(newValue -> ModConfig.data.modularDurationTicks = newValue)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Component.translatable("config.vision_goggles.nvg_color_theme"), ModConfig.getNvgColorTheme(), 0, 2)
                .setDefaultValue(0)
                .setTooltip(Component.translatable("config.vision_goggles.nvg_color_theme.tooltip"))
                .setSaveConsumer(newValue -> ModConfig.data.nvgColorTheme = newValue)
                .setTextGetter(value -> {
                    if (value == 0) return Component.translatable("config.vision_goggles.nvg_color_theme.green");
                    if (value == 1) return Component.translatable("config.vision_goggles.nvg_color_theme.white");
                    return Component.translatable("config.vision_goggles.nvg_color_theme.cyan");
                })
                .build());

        general.addEntry(entryBuilder.startStrList(Component.translatable("config.vision_goggles.extra_batteries"), ModConfig.getExtraBatteryItems())
                .setDefaultValue(List.of("minecraft:iron_ingot|0.1", "minecraft:copper_ingot|0.25"))
                .setTooltip(Component.translatable("config.vision_goggles.extra_batteries.tooltip"))
                .setSaveConsumer(newValue -> ModConfig.data.extraBatteryItems = newValue)
                .build());

        // Categoría HUD (v1.0.0)
        ConfigCategory hud = builder.getOrCreateCategory(Component.translatable("config.vision_goggles.hud"));

        hud.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.vision_goggles.show_coords"), ModConfig.shouldShowCoordinates())
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.data.showCoordinates = newValue)
                .build());

        hud.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.vision_goggles.show_saturation"), ModConfig.shouldShowSaturation())
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.data.showSaturation = newValue)
                .build());

        hud.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.vision_goggles.show_oxygen"), ModConfig.shouldShowOxygen())
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.data.showOxygenCounter = newValue)
                .build());

        hud.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.vision_goggles.show_durability"), ModConfig.shouldShowDurabilityWarning())
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.data.showLowDurabilityWarning = newValue)
                .build());

        builder.setSavingRunnable(() -> {
            ModConfig.save();
            ModConfig.updateBatteryMap();
            
            if (Platform.getEnv().name().equals("CLIENT")) {
                dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToServer(
                    new dev.itsrealperson.vision_goggles.network.ConfigSavePacket(
                        ModConfig.getNvgDuration(), 
                        ModConfig.getThermalDuration(), 
                        ModConfig.getHydroDuration(),
                        ModConfig.getBiometricDuration(),
                        ModConfig.getModularDuration(),
                        ModConfig.getNvgColorTheme(),
                        ModConfig.getExtraBatteryItems()
                    )
                );
            }
        });

        return builder.build();
    }
}
