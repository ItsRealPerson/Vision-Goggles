package dev.itsrealperson.vision_goggles.client.audio;

import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

public class GogglesHumSoundInstance extends AbstractTickableSoundInstance {
    private final Player player;
    private static GogglesHumSoundInstance instance;

    private GogglesHumSoundInstance(Player player) {
        super(SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, net.minecraft.util.RandomSource.create());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.4f; // Increased volume from 0.05
        this.pitch = 0.6f; // Low pitch hum
        this.relative = true;
    }

    public static void updateHum(Player player) {
        if (VisionRenderer.isVisorActive()) {
            if (instance == null || instance.isStopped()) {
                instance = new GogglesHumSoundInstance(player);
                Minecraft.getInstance().getSoundManager().play(instance);
            }
        } else {
            if (instance != null) {
                instance.stop();
                instance = null;
            }
        }
    }

    @Override
    public void tick() {
        if (this.player == null || !this.player.isAlive() || !VisionRenderer.isVisorActive()) {
            this.stop();
        } else {
            this.x = this.player.getX();
            this.y = this.player.getY();
            this.z = this.player.getZ();
        }
    }
}
