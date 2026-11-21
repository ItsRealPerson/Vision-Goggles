package dev.itsrealperson.vision_goggles.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class ClientGuiHelper {

    public static void openLuaScreen() {
        Minecraft.getInstance().setScreen(new LuaScriptViewerScreen());
    }

    public static void openTriLensPreviewScreen(String variantId) {
        dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensVariant v =
                dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensVariant.fromIdOrNull(variantId);
        if (v != null) {
            dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensPreviewState.setSelectedVariant(v);
        }
        Minecraft.getInstance().setScreen(new TriLensPreviewScreen());
    }

    public static void handleProtoPreview(String actionOrVariant) {
        if ("off".equalsIgnoreCase(actionOrVariant)) {
            dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensPreviewState.setHeadPreviewEnabled(false);
            return;
        }
        if ("flip".equalsIgnoreCase(actionOrVariant)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                net.minecraft.world.item.ItemStack helmet = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
                if (!helmet.isEmpty() && helmet.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("Unequip Vision Goggles to preview Tri-Lens on head."),
                            true
                    );
                    return;
                }
            }
            if (!dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensPreviewState.isHeadPreviewEnabled()) {
                dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensPreviewState.setVisorUp(true);
                dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensPreviewState.setHeadPreviewEnabled(true);
            }
            boolean flippingDown = dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensPreviewState.toggleInGameVisor();
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    flippingDown ? net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON : net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC,
                    flippingDown ? 1.25F : 0.85F
            ));
            return;
        }
        openTriLensPreviewScreen(actionOrVariant);
    }
}
