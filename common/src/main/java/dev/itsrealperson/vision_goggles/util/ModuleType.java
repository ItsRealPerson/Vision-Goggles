package dev.itsrealperson.vision_goggles.util;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public enum ModuleType {
    SOLAR(ModConstants.ID_SOLAR, ModConstants.MODULE_SOLAR) {
        @Override
        public void tickPower(ItemStack stack, ServerPlayer player) {
            if (player.tickCount % 20 == 0) {
                net.minecraft.core.BlockPos pos = player.blockPosition().above();
                boolean isDay = player.level().isDay();
                if (isDay && player.level().canSeeSky(pos)) {
                    float current = stack.getOrCreateTag().getFloat(ModConstants.TAG_BATTERY);
                    float capacity = ((dev.itsrealperson.vision_goggles.item.VisionGogglesItem)stack.getItem()).getBatteryCapacity(stack);
                    if (current < capacity) {
                        float chargeRate = 1.5f;
                        int maxDamage = stack.getMaxDamage();
                        if (maxDamage > 0) {
                            float durabilityFactor = (float)(maxDamage - stack.getDamageValue()) / maxDamage;
                            chargeRate *= Math.max(0.1f, durabilityFactor);
                        }
                        stack.getOrCreateTag().putFloat(ModConstants.TAG_BATTERY, Math.min(capacity, current + chargeRate));
                    }
                }
            }
        }
    },
    ZOOM(ModConstants.ID_ZOOM, ModConstants.MODULE_ZOOM),
    SONAR(ModConstants.ID_SONAR, ModConstants.MODULE_SONAR),
    BATTERY_EXPANSION(ModConstants.ID_BATTERY_EXPANSION, ModConstants.MODULE_BATTERY_EXPANSION),
    VITAL_INFO(ModConstants.ID_VITAL_INFO, ModConstants.MODULE_VITAL_INFO),
    ENVIRONMENT(ModConstants.ID_ENVIRONMENT, ModConstants.MODULE_ENVIRONMENT),
    SPAWN_SECURITY(ModConstants.ID_SPAWN_SECURITY, ModConstants.MODULE_SPAWN_SECURITY),
    CHUNK_VIEWER(ModConstants.ID_CHUNK_VIEWER, ModConstants.MODULE_CHUNK_VIEWER);

    private final ResourceLocation location;
    private final String legacyId;
    private static final Map<String, ModuleType> BY_ID = new HashMap<>();

    static {
        for (ModuleType type : values()) {
            BY_ID.put(type.location.toString(), type);
            BY_ID.put(type.legacyId, type); // For migration
        }
    }

    ModuleType(String location, String legacyId) {
        this.location = new ResourceLocation(location);
        this.legacyId = legacyId;
    }

    public String getId() {
        return location.toString();
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public static ModuleType byId(String id) {
        return BY_ID.get(id);
    }

    public void tickPower(ItemStack goggles, ServerPlayer player) {
        // Default: do nothing
    }
}
