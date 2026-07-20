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
        return nbt != null && nbt.getBoolean(ModConstants.TAG_ACTIVE) && vgItem.hasModule(goggles, ModConstants.ID_SONAR);
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

        float maxRange = 32.0f; // Blocks range
        
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

            // Draw blip
            gui.fill(blipX - 1, blipY - 1, blipX + 1, blipY + 1, dotColor);
        }
    }
}
