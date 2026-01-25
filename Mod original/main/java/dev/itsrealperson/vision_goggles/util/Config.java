package dev.itsrealperson.vision_goggles.util;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Mod.EventBusSubscriber(modid = VisionGoggles.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    public static class ServerConfig {
        public final ForgeConfigSpec.IntValue nvgDurationTicks;
        public final ForgeConfigSpec.IntValue thermalDurationTicks;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> extraBatteryItems;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.push("battery_stats");
            nvgDurationTicks = builder
                    .comment("Duración de la batería de las gafas de visión nocturna (ticks).")
                    .defineInRange("nvgDurationTicks", 6000, 20, 1000000);

            thermalDurationTicks = builder
                    .comment("Duración de la batería de las gafas térmicas (ticks).")
                    .defineInRange("thermalDurationTicks", 9000, 20, 1000000);
            builder.pop();

            builder.push("general");
            extraBatteryItems = builder
                    .comment("Lista de items bateria extra. Formato: 'modid:item|porcentaje'.", 
                             "Ejemplo: 'minecraft:iron_ingot|15' recarga un 15%.", 
                             "Si no pones porcentaje (ej: 'minecraft:redstone'), por defecto es 50%.")
                    .defineListAllowEmpty("extraBatteryItems", List.of("minecraft:iron_ingot|10", "minecraft:copper_ingot|25"), obj -> obj instanceof String);
            builder.pop();
        }
    }
    static {
        Pair<ServerConfig, ForgeConfigSpec> serverPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();
    }

    public static int nvgDuration;
    public static int thermalDuration;
    public static List<? extends String> batteryItemIDs = List.of();
    public static final java.util.Map<net.minecraft.resources.ResourceLocation, Float> BATTERY_MAP = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onModConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            nvgDuration = SERVER.nvgDurationTicks.get();
            thermalDuration = SERVER.thermalDurationTicks.get();
            batteryItemIDs = SERVER.extraBatteryItems.get();

            BATTERY_MAP.clear();

            for (String entry : batteryItemIDs) {
                try {
                    String[] parts = entry.split("\\|");
                    String idStr = parts[0].trim();
                    float charge = 0.5f;

                    if (parts.length > 1) {
                        try {
                            float val = Float.parseFloat(parts[1].trim());
                            charge = (val > 1.0f) ? val / 100.0f : val;
                        } catch (NumberFormatException e) {
                            System.out.println("Error parsing charge value for: " + idStr + ". Using default 50%.");
                        }
                    }

                    net.minecraft.resources.ResourceLocation loc;
                    if (idStr.contains(":")) {
                        loc = new net.minecraft.resources.ResourceLocation(idStr);
                    } else {
                        loc = new net.minecraft.resources.ResourceLocation("minecraft", idStr);
                    }
                    
                    BATTERY_MAP.put(loc, charge);
                    System.out.println("Loaded Battery: " + loc + " -> " + (charge * 100) + "%");

                } catch (Exception e) {
                    System.out.println("Failed to load battery config entry: " + entry);
                    e.printStackTrace();
                }
            }

        }
    }
    public static float getBatteryCharge(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;
        

        if (stack.getItem() == dev.itsrealperson.vision_goggles.VisionGoggles.NVG_BATTERY.get()) return 0.5f;

        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && BATTERY_MAP.containsKey(id)) {
            return BATTERY_MAP.get(id);
        }
        

        if (stack.is(dev.itsrealperson.vision_goggles.common.BatteryItem.NVG_BATTERIES)) return 0.5f;
        
        return 0.0f;
    }
}