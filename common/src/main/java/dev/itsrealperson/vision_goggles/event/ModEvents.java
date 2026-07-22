package dev.itsrealperson.vision_goggles.event;

import dev.architectury.event.events.common.TickEvent;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;

import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.network.BatteryPacket;
import dev.itsrealperson.vision_goggles.network.ConfigSyncPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;

public class ModEvents {

    public static boolean HITBOX_DEBUG_MODE = true;

    public static void init() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(net.minecraft.commands.Commands.literal("vgdebug")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    HITBOX_DEBUG_MODE = !HITBOX_DEBUG_MODE;
                    context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("Hitbox Debug Mode: " + HITBOX_DEBUG_MODE), true);
                    return 1;
                })
            );
        });
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer) {
                tickGoggles((ServerPlayer) player);
            }
        });

        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            


            // Lógica Real (Solo se ejecuta si el objetivo es un jugador usando las gafas)
            if (entity instanceof ServerPlayer player) {
                ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
                if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem) {
                    float ratio = ModConfig.getDamageTransferRatio();
                    if (ratio > 0) {
                        int damage = Math.max(1, Math.round(amount * ratio));
                        helmet.hurtAndBreak(damage, player, (p) -> p.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.HEAD));
                    }
                }
            }
            return EventResult.pass();
        });

        // Detect when player right-clicks with an item that could be a battery
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) return CompoundEventResult.pass();

            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (helmet.isEmpty() || !(helmet.getItem() instanceof VisionGogglesItem goggles)) {
                return CompoundEventResult.pass();
            }

            float currentBatt = helmet.getOrCreateTag().getFloat(ModConstants.TAG_BATTERY);
            float maxBatt = goggles.getBatteryCapacity(helmet);
            if (currentBatt >= maxBatt) return CompoundEventResult.pass();

            float chargeFraction = ModConfig.getBatteryCharge(stack);
            if (chargeFraction > 0) {
                float chargeToAdd = maxBatt * chargeFraction;
                float newBatt = Math.min(maxBatt, currentBatt + chargeToAdd);
                helmet.getOrCreateTag().putFloat(ModConstants.TAG_BATTERY, newBatt);

                if (!player.isCreative()) {
                    stack.shrink(1);
                }

                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                return CompoundEventResult.interruptTrue(stack);
            }
            return CompoundEventResult.pass();
        });

        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                NetworkManager.INSTANCE.sendToPlayer(serverPlayer, new ConfigSyncPacket(ModConfig.toCommonJson()));
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