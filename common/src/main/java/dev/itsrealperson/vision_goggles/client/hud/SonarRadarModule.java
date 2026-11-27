package dev.itsrealperson.vision_goggles.client.hud;

import dev.itsrealperson.vision_goggles.util.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;

public class SonarRadarModule implements IHudModule {
    private long lastPingTime = -1;

    @Override
    public boolean shouldRender(Player player, ItemStack goggles) {
        if (goggles.isEmpty() || !(goggles.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem vgItem)) return false;
        CompoundTag nbt = goggles.getTag();
        if (nbt == null || !nbt.getBoolean(ModConstants.TAG_ACTIVE) || !vgItem.hasModule(goggles, ModConstants.ID_SONAR)) {
            return false;
        }
        // Yield the HUD to the Lua Sonar Pro 360 DSP upgrade when installed to avoid two overlapping radars
        if (vgItem instanceof dev.itsrealperson.vision_goggles.item.ModularGogglesItem modular) {
            dev.itsrealperson.vision_goggles.scripting.LuaCustomModule radarUpgrade =
                    dev.itsrealperson.vision_goggles.scripting.LuaModuleManager.findModuleByIdOrPath("radar_amenazas");
            if (radarUpgrade != null && radarUpgrade.isHardwareInstalled(goggles, modular)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void render(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        int size = 80; // Size of the radar
        int centerX = width - size / 2 - 10;
        int centerY = 10 + size / 2;
        int radius = size / 2;

        // Draw radar background (translucent black box with cyan border)
        gui.fill(centerX - radius, centerY - radius, centerX + radius, centerY + radius, 0x88000000);
        gui.fill(centerX - radius, centerY - radius, centerX + radius, centerY - radius + 1, 0xFF00FFFF); // Top border
        gui.fill(centerX - radius, centerY + radius - 1, centerX + radius, centerY + radius, 0xFF00FFFF); // Bottom
        gui.fill(centerX - radius, centerY - radius, centerX - radius + 1, centerY + radius, 0xFF00FFFF); // Left
        gui.fill(centerX + radius - 1, centerY - radius, centerX + radius, centerY + radius, 0xFF00FFFF); // Right

        // Draw crosshair
        gui.fill(centerX - 2, centerY, centerX + 3, centerY + 1, 0x8800FFFF);
        gui.fill(centerX, centerY - 2, centerX + 1, centerY + 3, 0x8800FFFF);

        float maxRange = (float) dev.itsrealperson.vision_goggles.client.VisionHUDOverlay.getEffectiveDetectionRange(player, 32.0); // 32m on land; 20m -> 12m underwater by depth
        
        long time = mc.level.getGameTime();
        if (time % 200 == 0 && lastPingTime != time) {
            lastPingTime = time;
            // Play ping sound
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.SONAR.get(), SoundSource.PLAYERS, 0.5f, 1.0f, false);
        }
        
        float timeSincePing = (time % 200) + partialTicks;
        // Make it visible for 2 seconds (40 ticks), fading out completely.
        float pingProgress = timeSincePing / 40.0f;
        
        // Fading opacity for dots
        float opacity = 1.0f - pingProgress; 
        if (opacity <= 0.01f) return; // Don't render entities after they fade out

        int alpha = (int) (opacity * 255.0f);
        int dotColor = (alpha << 24) | 0x00FFFF;

        CompoundTag nbt = goggles.getTag();
        int mode = nbt != null ? nbt.getInt(ModConstants.TAG_SONAR_MODE) : 0; // 0=Todos, 1=Jugadores, 2=No-Muertos

        // Advancing acoustic wave ring on the 2D radar
        int waveR = Math.max(2, Math.min(radius - 1, Math.round(pingProgress * (radius - 1))));
        int waveAlpha = (int) (opacity * 120.0f);
        int waveCol = (waveAlpha << 24) | 0x00FFCC;
        gui.fill(centerX - waveR, centerY - waveR, centerX + waveR, centerY - waveR + 1, waveCol);
        gui.fill(centerX - waveR, centerY + waveR - 1, centerX + waveR, centerY + waveR, waveCol);
        gui.fill(centerX - waveR, centerY - waveR, centerX - waveR + 1, centerY + waveR, waveCol);
        gui.fill(centerX + waveR - 1, centerY - waveR, centerX + waveR, centerY + waveR, waveCol);

        int echoMarkersDrawn = 0;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == player) continue;
            if (!(entity instanceof LivingEntity living)) continue;

            // Mode filtering
            if (mode == 1 && !(living instanceof Player)) continue; // Jugadores
            if (mode == 2 && living.getMobType() != net.minecraft.world.entity.MobType.UNDEAD) continue; // No-Muertos

            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();
            double distSq = dx * dx + dz * dz;

            if (distSq > maxRange * maxRange) continue;

            // Calculate rotation relative to player's look direction
            float playerYaw = player.getYRot();
            
            // Correct mapping: angle should reflect orientation where Top is Forward (-Z), Bottom is Back (+Z), Left is -X, Right is +X.
            double angle = Math.atan2(dz, dx) - Math.toRadians(playerYaw) - Math.PI;

            double dist = Math.sqrt(distSq);
            double radarDist = (dist / maxRange) * (radius - 2);

            int blipX = centerX + (int) (Math.cos(angle) * radarDist);
            int blipY = centerY + (int) (Math.sin(angle) * radarDist);

            // Draw 2D radar blip
            boolean isHostile = living instanceof net.minecraft.world.entity.monster.Enemy;
            int blipRgb = (living instanceof Player) ? 0x00EEFF : (isHostile ? 0xFF4455 : 0x00FFAA);
            gui.fill(blipX - 1, blipY - 1, blipX + 1, blipY + 1, (alpha << 24) | blipRgb);

            // Project 3D acoustic echo pulse marker onto screen (works even through solid walls)
            if (echoMarkersDrawn < 10) {
                double ex = net.minecraft.util.Mth.lerp(partialTicks, living.xo, living.getX());
                double ey = net.minecraft.util.Mth.lerp(partialTicks, living.yo, living.getY()) + living.getBbHeight() * 0.55;
                double ez = net.minecraft.util.Mth.lerp(partialTicks, living.zo, living.getZ());
                float[] proj = dev.itsrealperson.vision_goggles.client.VisionHUDOverlay.projectWorldToGui(mc, ex, ey, ez, partialTicks);
                if (proj != null && proj[3] > 0.5f) {
                    echoMarkersDrawn++;
                    int sx = Math.round(proj[0]);
                    int sy = Math.round(proj[1]);
                    int echoRgb = isHostile ? 0xFF4466 : ((living instanceof Player) ? 0x00FFFF : 0x00FFBB);
                    int echoColor = (alpha << 24) | echoRgb;
                    int pulseRingR = 4 + Math.round(pingProgress * 8.0f);
                    int ringAlpha = Math.max(0, (int) (opacity * 170.0f));
                    int ringColor = (ringAlpha << 24) | 0x00FFFF;

                    // Acoustic expanding pulse brackets & center diamond
                    dev.itsrealperson.vision_goggles.client.VisionHUDOverlay.drawCornerBrackets(gui, sx, sy, pulseRingR, 3, ringColor);
                    gui.fill(sx - 2, sy, sx + 3, sy + 1, echoColor);
                    gui.fill(sx, sy - 2, sx + 1, sy + 3, echoColor);

                    if (opacity > 0.22f) {
                        String echoTag = String.format("[ECHO %.0fm]", proj[2]);
                        int tagW = Math.round(mc.font.width(echoTag) * 0.62f);
                        dev.itsrealperson.vision_goggles.client.VisionHUDOverlay.drawScaledText(
                                gui, mc, echoTag, sx - tagW / 2, sy + pulseRingR + 2, 0.62f, echoColor
                        );
                    }
                }
            }
        }
    }
}
