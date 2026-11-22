package dev.itsrealperson.vision_goggles.mixin.client;

import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import dev.itsrealperson.vision_goggles.client.VisionShaderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void vision_goggles$onSound(ClientboundSoundPacket p, CallbackInfo ci) {
        var key = p.getSound().unwrapKey().orElse(null);
        if (key == null) return;
        String ns = key.location().getNamespace(), path = key.location().getPath();
        if (!java.util.Set.of("tacz", "pointblank", "vic", "cgm", "superbwarfare", "jeg").contains(ns)) return;
        for (String b : new String[]{"silence", "suppres", "reload", "dry", "aim", "draw", "put_away", "bolt", "click", "inspect", "equip"}) if (path.contains(b)) return;
        for (String g : new String[]{"shoot", "fire", "gun", "shot", "blast", "rifle", "pistol", "sniper"}) if (path.contains(g)) { VisionShaderManager.gunFlash(p.getX(), p.getY(), p.getZ(), true); return; }
    }


    @Inject(method = "handleExplosion", at = @At("HEAD"))
    private void vision_goggles$onExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && VisionRenderer.isVisionModeActive()) {
            double distSq = mc.player.distanceToSqr(packet.getX(), packet.getY(), packet.getZ());
            if (distSq < 1600.0) { // within 40 blocks
                double dist = Math.sqrt(distSq);
                // Scaling: Point-blank (0-4 blocks) = 2.0f max blinding flash, 40 blocks = 0.45f
                float flash = (float) Math.max(0.45, 2.0 - (dist / 40.0) * 1.55);
                VisionShaderManager.triggerFlash(flash);
            }
        }
    }

    @Inject(method = "handleAddEntity", at = @At("HEAD"))
    private void vision_goggles$onAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        String tk = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(packet.getType()));
        if (tk.contains("bullet") || tk.startsWith("tacz")) VisionShaderManager.gunFlash(packet.getX(), packet.getY(), packet.getZ(), true);
        if (packet.getType() == EntityType.LIGHTNING_BOLT) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null && VisionRenderer.isVisionModeActive()) {
                double distSq = mc.player.distanceToSqr(packet.getX(), packet.getY(), packet.getZ());
                if (distSq < 4096.0) { // within 64 blocks
                    VisionShaderManager.triggerFlash(1.8f);
                }
            }
        }
    }
}
