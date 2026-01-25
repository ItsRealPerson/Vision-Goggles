package dev.itsrealperson.vision_goggles.network;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(VisionGoggles.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(ToggleNVGPacket.class, id())
                .encoder(ToggleNVGPacket::encode)
                .decoder(ToggleNVGPacket::new)
                .consumerMainThread(ToggleNVGPacket::handle)
                .add();

        net.messageBuilder(BatteryPacket.class, id())
                .encoder(BatteryPacket::encode)
                .decoder(BatteryPacket::new)
                .consumerMainThread(BatteryPacket::handle)
                .add();

        net.messageBuilder(EquipPacket.class, id())
                .encoder(EquipPacket::encode)
                .decoder(EquipPacket::new)
                .consumerMainThread(EquipPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}
