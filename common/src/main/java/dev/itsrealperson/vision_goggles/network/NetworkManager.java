package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkChannel;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.resources.ResourceLocation;

public class NetworkManager {
    public static final NetworkChannel INSTANCE = NetworkChannel.create(new ResourceLocation(Vision_goggles.MOD_ID, "main"));

    public static void register() {
        INSTANCE.register(EquipPacket.class, EquipPacket::encode, EquipPacket::new, EquipPacket::handle);
        INSTANCE.register(ToggleNVGPacket.class, ToggleNVGPacket::encode, ToggleNVGPacket::new, ToggleNVGPacket::handle);
        INSTANCE.register(BatteryPacket.class, BatteryPacket::encode, BatteryPacket::new, BatteryPacket::handle);
        INSTANCE.register(BatterySyncPacket.class, BatterySyncPacket::encode, BatterySyncPacket::new, BatterySyncPacket::handle);
        INSTANCE.register(ConfigSyncPacket.class, ConfigSyncPacket::encode, ConfigSyncPacket::new, ConfigSyncPacket::handle);
        INSTANCE.register(ConfigSavePacket.class, ConfigSavePacket::encode, ConfigSavePacket::new, ConfigSavePacket::handle);
    }
}
