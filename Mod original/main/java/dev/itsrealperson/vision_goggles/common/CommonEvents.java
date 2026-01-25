package dev.itsrealperson.vision_goggles.common;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import dev.itsrealperson.vision_goggles.client.ClientHooks;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VisionGoggles.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getItemStack().getItem() == VisionGoggles.NVG_BATTERY.get()) return;

        if (BatteryItem.isBattery(event.getItemStack())) {
            
            if (event.getLevel().isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientHooks.debugBattery(event.getEntity(), event.getItemStack());
                    ClientHooks.tryUseBattery(event.getEntity(), event.getItemStack());
                });
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            } else {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        } else {
            if (event.getLevel().isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                   ClientHooks.debugBattery(event.getEntity(), event.getItemStack());
                });
            }
        }
    }
}
