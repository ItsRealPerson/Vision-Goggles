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
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.EventResult;
import dev.itsrealperson.vision_goggles.network.BatteryPacket;
import dev.itsrealperson.vision_goggles.network.ConfigSyncPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;

public class ModEvents {

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer) {
                tickGoggles((ServerPlayer) player);
            }
        });

        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
                if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem) {
                    // Damage calculation: 1 point per 4 damage taken, minimum 1
                    int damage = Math.max(1, (int) (amount / 4.0F));
                    helmet.hurtAndBreak(damage, player, (p) -> p.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.HEAD));
                }
            }
            return EventResult.pass();
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
                        ModConfig.getExtraBatteryItems()
                ));
            }
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player instanceof ServerPlayer) {
                // Cleanup removed
            }
        });
    }

    private static void tickGoggles(ServerPlayer player) {
        ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
        
        if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem gogglesItem) {
            gogglesItem.serverTick(helmet, player);
        } else {
            VisionGogglesItem.cleanUpEffect(player);
        }
    }
}