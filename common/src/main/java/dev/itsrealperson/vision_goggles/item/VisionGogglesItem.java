package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.registry.ModDataComponents;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import dev.itsrealperson.vision_goggles.network.BatterySyncPacket;
import dev.itsrealperson.vision_goggles.network.EquipPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

public class VisionGogglesItem extends Item {
    private final List<VisionMode> supportedModes;
    private final IntSupplier batteryCapacity;

    public VisionGogglesItem(IntSupplier batteryCapacity, VisionMode... modes) {
        super(new Item.Properties().stacksTo(1));
        this.batteryCapacity = batteryCapacity;
        this.supportedModes = Arrays.asList(modes);
        if (this.supportedModes.isEmpty()) throw new IllegalArgumentException("Must have at least one vision mode");
    }

    public int getBatteryCapacity() {
        return batteryCapacity.getAsInt();
    }
    
    public int getBatteryCapacity(ItemStack stack) {
        return getBatteryCapacity();
    }

    public VisionMode getVisionMode() {
        return supportedModes.get(0); // Default mode
    }

    public List<VisionMode> getSupportedModes() {
        return supportedModes;
    }

    public void serverTick(ItemStack stack, ServerPlayer player) {
        float maxBattery = (float) getBatteryCapacity(stack);
        
        if (!stack.has(ModDataComponents.BATTERY.get())) stack.set(ModDataComponents.BATTERY.get(), maxBattery);
        float currentBattery = Objects.requireNonNullElse(stack.get(ModDataComponents.BATTERY.get()), maxBattery);
        
        int modeId = Objects.requireNonNullElse(stack.get(ModDataComponents.MODE.get()), 0);
        VisionMode mode = VisionMode.byId(modeId);
        boolean isActive = Objects.requireNonNullElse(stack.get(ModDataComponents.ACTIVE.get()), false);

        if (isActive) {
            if (currentBattery > 0) {
                float drain = (mode == VisionMode.THERMAL) ? 2.0f : 1.0f;
                currentBattery = Math.max(0, currentBattery - drain);
                stack.set(ModDataComponents.BATTERY.get(), currentBattery);
                
                boolean shouldApplyNV = true;
                if (modeId == -1 || mode == VisionMode.BIOMETRIC) {
                    shouldApplyNV = false;
                } else if (mode == VisionMode.HYDRO && !player.isUnderWater()) {
                    shouldApplyNV = false;
                }

                if (shouldApplyNV) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 215, 0, false, false, false));
                } else {
                    cleanUpEffect(player);
                }
                
                if (currentBattery <= 0) {
                    stack.set(ModDataComponents.ACTIVE.get(), false);
                    cleanUpEffect(player);
                }
            } else {
                stack.set(ModDataComponents.ACTIVE.get(), false);
                cleanUpEffect(player);
            }
        } else {
            cleanUpEffect(player);
        }

        if (player.tickCount % 20 == 0) {
            NetworkManager.sendToPlayer(player, new BatterySyncPacket(currentBattery));
        }
    }

    public static void cleanUpEffect(ServerPlayer player) {
        if (player.hasEffect(MobEffects.NIGHT_VISION)) {
            MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null && effect.getDuration() <= 300) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (level.isClientSide) {
            NetworkManager.sendToServer(new EquipPacket());
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        return InteractionResultHolder.pass(stack);
    }
}
