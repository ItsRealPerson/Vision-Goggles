package dev.itsrealperson.vision_goggles.util;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FlashlightDeterrence {

    public static void apply(Level level, Player player, double range) {
        if (level.isClientSide) return;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0f);
        AABB area = player.getBoundingBox().inflate(range);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, e -> 
            e != player && e.isAlive() && (e instanceof Enemy || (e instanceof Mob mob && mob.getTarget() == player))
        );

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.getEyePosition().subtract(eyePos);
            double dist = toTarget.length();
            if (dist > range || dist < 0.5) continue;

            Vec3 dirToTarget = toTarget.normalize();
            double dot = lookVec.dot(dirToTarget);

            // Within strobe cone (~45 degrees, dot > 0.70)
            if (dot > 0.70) {
                // Check if target is looking back towards the light (face-to-face blinding)
                Vec3 targetLook = target.getViewVector(1.0f);
                double facingDot = targetLook.dot(dirToTarget.scale(-1.0));
                if (facingDot > 0.20) {
                    // Apply brief disorientation slowness (1.25s)
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false, false));
                    // If Creeper, suppress fuse swelling countdown
                    if (target instanceof Creeper creeper) {
                        creeper.setSwellDir(-1);
                    }
                }
            }
        }
    }
}
