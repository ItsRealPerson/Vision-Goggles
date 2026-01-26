package dev.itsrealperson.vision_goggles.event;

import dev.architectury.event.events.common.TickEvent;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.itsrealperson.vision_goggles.network.BatteryPacket;
import dev.itsrealperson.vision_goggles.network.ConfigSyncPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;

public class ModEvents {
    public static final String NBT_BATTERY = "nvg_battery";
    public static final String NBT_ACTIVE = "nvg_active";
    public static final String NBT_MODE = "vision_mode";

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer) {
                tickGoggles((ServerPlayer) player);
            }
        });

        // Detect when player right-clicks with an item that could be a battery
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) return CompoundEventResult.pass();

            // If it's a battery according to config but NOT our own BatteryItem 
            // (Our own item already handles this in its class)
            if (!(stack.getItem() instanceof dev.itsrealperson.vision_goggles.item.BatteryItem) && ModConfig.getBatteryCharge(stack) > 0) {
                if (player.level().isClientSide) {
                    if (!PlatformMethods.getEquippedHelmet(player).isEmpty()) {
                        NetworkManager.INSTANCE.sendToServer(new BatteryPacket());
                        float charge = ModConfig.getBatteryCharge(stack);
                        int pct = (int)(charge * 100);
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.vision_goggles.recharged", pct), true);
                        return CompoundEventResult.interruptTrue(stack);
                    } else {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.vision_goggles.equip_warning"), true);
                    }
                }
                return CompoundEventResult.interruptTrue(stack);
            }
            return CompoundEventResult.pass();
        });

        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                NetworkManager.INSTANCE.sendToPlayer(serverPlayer, new ConfigSyncPacket(
                        ModConfig.getNvgDuration(),
                        ModConfig.getThermalDuration(),
                        ModConfig.getHydroDuration(),
                        ModConfig.getBiometricDuration(),
                        ModConfig.getModularDuration(),
                        ModConfig.getNvgColorTheme(),
                        ModConfig.getExtraBatteryItems()
                ));
            }
        });
    }

    private static void tickGoggles(ServerPlayer player) {
        ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
        
        if (helmet.isEmpty()) {
            cleanUpEffect(player);
            return;
        }

        CompoundTag nbt = helmet.getOrCreateTag();
        float maxBattery = (float)ModConfig.getNvgDuration();
        boolean hasSolar = false;
        int modeId = nbt.getInt(NBT_MODE);
        dev.itsrealperson.vision_goggles.util.VisionMode mode = dev.itsrealperson.vision_goggles.util.VisionMode.byId(modeId);

        if (helmet.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem gogglesItem) {
            if (gogglesItem instanceof dev.itsrealperson.vision_goggles.item.ModularGogglesItem modularGoggles) {
                maxBattery = (float) modularGoggles.getBatteryCapacity(helmet);
                hasSolar = modularGoggles.getUtilityModules(helmet).contains("SOLAR");
            } else {
                maxBattery = (float) gogglesItem.getBatteryCapacity();
            }
        }

        if (!nbt.contains(NBT_BATTERY)) nbt.putFloat(NBT_BATTERY, maxBattery);
        float currentBattery = nbt.getFloat(NBT_BATTERY);

        // SOLAR RECHARGE: Works even when goggles are OFF
        if (hasSolar && player.level().isDay() && player.level().canSeeSky(player.blockPosition().above())) {
            if (player.level().getMaxLocalRawBrightness(player.blockPosition().above()) > 10) {
                currentBattery = Math.min(maxBattery, currentBattery + 1.5f);
                nbt.putFloat(NBT_BATTERY, currentBattery);
            }
        }

        if (nbt.getBoolean(NBT_ACTIVE)) {
            if (currentBattery > 0) {
                float drain = (mode == dev.itsrealperson.vision_goggles.util.VisionMode.THERMAL) ? 2.0f : 1.0f;
                currentBattery = Math.max(0, currentBattery - drain);
                nbt.putFloat(NBT_BATTERY, currentBattery);
                
                // Effect Logic:
                boolean shouldApplyNV = true;
                // DO NOT apply Night Vision in Biometric, Normal Mode (-1), or Hydro (if dry)
                if (modeId == -1 || mode == dev.itsrealperson.vision_goggles.util.VisionMode.BIOMETRIC) {
                    shouldApplyNV = false;
                } else if (mode == dev.itsrealperson.vision_goggles.util.VisionMode.HYDRO && !player.isUnderWater()) {
                    shouldApplyNV = false;
                }

                if (shouldApplyNV) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 215, 0, false, false, false));
                } else {
                    cleanUpEffect(player);
                }
                
                if (currentBattery <= 0) nbt.putBoolean(NBT_ACTIVE, false);
            } else {
                nbt.putBoolean(NBT_ACTIVE, false);
                cleanUpEffect(player);
            }
        } else {
            cleanUpEffect(player);
        }
    }

    private static void cleanUpEffect(ServerPlayer player) {
        if (player.hasEffect(MobEffects.NIGHT_VISION)) {
            MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null && effect.getDuration() <= 215) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
}


