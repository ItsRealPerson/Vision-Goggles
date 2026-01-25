package dev.itsrealperson.vision_goggles.server;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import dev.itsrealperson.vision_goggles.client.ClientEvents;
import dev.itsrealperson.vision_goggles.util.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = VisionGoggles.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {
    private static final String NBT_BATTERY = "nvg_battery";
    private static final String NBT_ACTIVE = "nvg_active";
    private static final String NBT_MODE = "vision_mode";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.player;

        Optional<ItemStack> curio = CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> ClientEvents.isVisor(stack.getItem())).map(slot -> slot.stack());
        
        if (curio.isEmpty()) {
            cleanUpEffect(player);
            return;
        }

        ItemStack helmet = curio.get();
        CompoundTag nbt = helmet.getOrCreateTag();
        
        if (nbt.getBoolean(NBT_ACTIVE)) {
            float maxBattery = (helmet.getItem().toString().contains("thermal")) ? (float)Config.thermalDuration : (float)Config.nvgDuration;
            if (!nbt.contains(NBT_BATTERY)) nbt.putFloat(NBT_BATTERY, maxBattery);

            float currentBattery = nbt.getFloat(NBT_BATTERY);
            if (currentBattery > 0) {
                float drain = nbt.getInt(NBT_MODE) == 1 ? 2.0f : 1.0f;
                currentBattery = Math.max(0, currentBattery - drain);
                nbt.putFloat(NBT_BATTERY, currentBattery);
                
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 215, 0, false, false, false));
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