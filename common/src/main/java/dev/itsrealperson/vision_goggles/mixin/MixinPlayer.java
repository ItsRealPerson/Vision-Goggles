package dev.itsrealperson.vision_goggles.mixin;

import dev.itsrealperson.vision_goggles.api.VisionGogglesAPI;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayer extends LivingEntity {

    protected MixinPlayer(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void vision_goggles$hydroUnderwaterMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        if (self.isEyeInFluid(FluidTags.WATER)
                && !EnchantmentHelper.hasAquaAffinity(self)
                && VisionGogglesAPI.getActiveVisionMode(self) == VisionMode.HYDRO) {
            cir.setReturnValue(cir.getReturnValueF() * 5.0F);
        }
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void vision_goggles$hydroPropulsionBoost(Vec3 travelVector, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.isInWater()
                && !self.getAbilities().flying
                && (travelVector.x != 0.0 || travelVector.y != 0.0 || travelVector.z != 0.0 || self.isSwimming())
                && VisionGogglesAPI.getActiveVisionMode(self) == VisionMode.HYDRO) {
            Vec3 dm = self.getDeltaMovement();
            double horizSq = dm.x * dm.x + dm.z * dm.z;
            double maxHoriz = self.isSwimming() ? 0.34 : 0.24;
            if (horizSq > 1.0E-5 && horizSq < maxHoriz * maxHoriz) {
                double factor = 1.038;
                double newX = dm.x * factor;
                double newZ = dm.z * factor;
                double newHoriz = Math.sqrt(newX * newX + newZ * newZ);
                if (newHoriz > maxHoriz) {
                    double clamp = maxHoriz / newHoriz;
                    newX *= clamp;
                    newZ *= clamp;
                }
                double newY = dm.y;
                if (self.isSwimming() && Math.abs(dm.y) < 0.28) {
                    newY = dm.y * 1.025;
                }
                self.setDeltaMovement(newX, newY, newZ);
            }
        }
    }
}
