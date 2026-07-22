package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.util.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import dev.architectury.platform.Platform;

import java.util.List;

public class ModConfigGui {

    /** Returns true if the local player has OP privileges (permission level 2+). */
    private static boolean isOp() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.hasPermissions(2);
    }

    public static Screen createConfigScreen(Screen parent) {
        boolean op = isOp();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.vision_goggles.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // ─────────────────────────────────────────────────────────────────────
        // SERVER CATEGORY — visible only to OPs
        // ─────────────────────────────────────────────────────────────────────
        if (op) {
            ConfigCategory server = builder.getOrCreateCategory(Component.translatable("config.vision_goggles.server"));

            // --- Durations ---
            server.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.nvg_duration"), ModConfig.getNvgDuration())
                    .setDefaultValue(6000)
                    .setTooltip(Component.translatable("config.vision_goggles.nvg_duration.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.nvgDurationTicks = v)
                    .build());

            server.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.thermal_duration"), ModConfig.getThermalDuration())
                    .setDefaultValue(9000)
                    .setTooltip(Component.translatable("config.vision_goggles.thermal_duration.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.thermalDurationTicks = v)
                    .build());

            server.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.hydro_duration"), ModConfig.getHydroDuration())
                    .setDefaultValue(6000)
                    .setTooltip(Component.translatable("config.vision_goggles.hydro_duration.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.hydroDurationTicks = v)
                    .build());

            server.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.bio_duration"), ModConfig.getBiometricDuration())
                    .setDefaultValue(4500)
                    .setTooltip(Component.translatable("config.vision_goggles.bio_duration.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.biometricDurationTicks = v)
                    .build());

            server.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.modular_duration"), ModConfig.getModularDuration())
                    .setDefaultValue(6000)
                    .setTooltip(Component.translatable("config.vision_goggles.modular_duration.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.modularDurationTicks = v)
                    .build());

            // --- Battery Balance ---
            server.addEntry(entryBuilder.startFloatField(Component.translatable("config.vision_goggles.battery_drain_multiplier"), ModConfig.getBatteryDrainMultiplier())
                    .setDefaultValue(1.0f)
                    .setTooltip(Component.translatable("config.vision_goggles.battery_drain_multiplier.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.batteryDrainMultiplier = v)
                    .build());

            server.addEntry(entryBuilder.startFloatField(Component.translatable("config.vision_goggles.battery_standby_drain"), ModConfig.getBatteryStandbyDrain())
                    .setDefaultValue(0.001f)
                    .setTooltip(Component.translatable("config.vision_goggles.battery_standby_drain.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.batteryStandbyDrain = v)
                    .build());

            server.addEntry(entryBuilder.startFloatField(Component.translatable("config.vision_goggles.battery_expansion_multiplier"), ModConfig.getBatteryExpansionMultiplier())
                    .setDefaultValue(1.5f)
                    .setTooltip(Component.translatable("config.vision_goggles.battery_expansion_multiplier.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.batteryExpansionMultiplier = v)
                    .build());

            server.addEntry(entryBuilder.startFloatField(Component.translatable("config.vision_goggles.solar_charge_rate"), ModConfig.getSolarChargeRate())
                    .setDefaultValue(1.5f)
                    .setTooltip(Component.translatable("config.vision_goggles.solar_charge_rate.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.solarChargeRate = v)
                    .build());

            server.addEntry(entryBuilder.startStrList(Component.translatable("config.vision_goggles.extra_batteries"), ModConfig.getExtraBatteryItems())
                    .setDefaultValue(List.of("minecraft:iron_ingot|0.1", "minecraft:copper_ingot|0.25"))
                    .setTooltip(Component.translatable("config.vision_goggles.extra_batteries.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.extraBatteryItems = v)
                    .build());

            // --- Durability ---
            server.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.goggles_durability"), ModConfig.getGogglesDurability())
                    .setDefaultValue(300)
                    .setTooltip(Component.translatable("config.vision_goggles.goggles_durability.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.gogglesDurability = v)
                    .build());

            server.addEntry(entryBuilder.startFloatField(Component.translatable("config.vision_goggles.damage_transfer_ratio"), ModConfig.getDamageTransferRatio())
                    .setDefaultValue(0.25f)
                    .setTooltip(Component.translatable("config.vision_goggles.damage_transfer_ratio.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.damageTransferRatio = v)
                    .build());

            // --- Glare / Flashlight ---
            server.addEntry(entryBuilder.startIntField(Component.translatable("config.vision_goggles.nvg_glare_max_range"), ModConfig.getNvgGlareMaxRange())
                    .setDefaultValue(25)
                    .setTooltip(Component.translatable("config.vision_goggles.nvg_glare_max_range.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.nvgGlareMaxRange = v)
                    .build());

            server.addEntry(entryBuilder.startFloatField(Component.translatable("config.vision_goggles.nvg_glare_back_factor"), ModConfig.getNvgGlareBackFactor())
                    .setDefaultValue(0.05f)
                    .setTooltip(Component.translatable("config.vision_goggles.nvg_glare_back_factor.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.nvgGlareBackFactor = v)
                    .build());

            server.addEntry(entryBuilder.startFloatField(Component.translatable("config.vision_goggles.nvg_generic_light_intensity"), ModConfig.getNvgGenericLightIntensity())
                    .setDefaultValue(1.5f)
                    .setTooltip(Component.translatable("config.vision_goggles.nvg_generic_light_intensity.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.nvgGenericLightIntensity = v)
                    .build());

            server.addEntry(entryBuilder.startIntSlider(Component.translatable("config.vision_goggles.max_flashlights"), ModConfig.getMaxFlashlights(), 1, 128)
                    .setDefaultValue(16)
                    .setTooltip(Component.translatable("config.vision_goggles.max_flashlights.tooltip"))
                    .setSaveConsumer(v -> ModConfig.common.maxSimultaneousFlashlights = v)
                    .build());
        }

        // ─────────────────────────────────────────────────────────────────────
        // CLIENT CATEGORY — visible to all players
        // ─────────────────────────────────────────────────────────────────────
        ConfigCategory client = builder.getOrCreateCategory(Component.translatable("config.vision_goggles.client"));

        client.addEntry(entryBuilder.startIntSlider(Component.translatable("config.vision_goggles.nvg_color_theme"), ModConfig.getNvgColorTheme(), 0, 2)
                .setDefaultValue(0)
                .setTooltip(Component.translatable("config.vision_goggles.nvg_color_theme.tooltip"))
                .setSaveConsumer(v -> ModConfig.client.nvgColorTheme = v)
                .setTextGetter(value -> {
                    if (value == 0) return Component.translatable("config.vision_goggles.nvg_color_theme.green");
                    if (value == 1) return Component.translatable("config.vision_goggles.nvg_color_theme.white");
                    return Component.translatable("config.vision_goggles.nvg_color_theme.cyan");
                })
                .build());

        // ─────────────────────────────────────────────────────────────────────
        // HUD CATEGORY — visible to all players
        // ─────────────────────────────────────────────────────────────────────
        ConfigCategory hud = builder.getOrCreateCategory(Component.translatable("config.vision_goggles.hud"));

        hud.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.vision_goggles.show_coords"), ModConfig.shouldShowCoordinates())
                .setDefaultValue(true)
                .setSaveConsumer(v -> ModConfig.client.showCoordinates = v)
                .build());

        hud.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.vision_goggles.show_saturation"), ModConfig.shouldShowSaturation())
                .setDefaultValue(true)
                .setSaveConsumer(v -> ModConfig.client.showSaturation = v)
                .build());

        hud.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.vision_goggles.show_oxygen"), ModConfig.shouldShowOxygen())
                .setDefaultValue(true)
                .setSaveConsumer(v -> ModConfig.client.showOxygenCounter = v)
                .build());

        hud.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.vision_goggles.show_durability"), ModConfig.shouldShowDurabilityWarning())
                .setDefaultValue(true)
                .setSaveConsumer(v -> ModConfig.client.showLowDurabilityWarning = v)
                .build());

        // ─────────────────────────────────────────────────────────────────────
        // Save runnable
        // ─────────────────────────────────────────────────────────────────────
        builder.setSavingRunnable(() -> {
            ModConfig.saveClient(); // client config always saves locally
            ModConfig.updateBatteryMap();

            // Server config changes are only sent if the player is OP
            if (op && Platform.getEnv().name().equals("CLIENT")) {
                dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToServer(
                    new dev.itsrealperson.vision_goggles.network.ConfigSavePacket(ModConfig.toCommonJson())
                );
            }
        });

        return builder.build();
    }
}
