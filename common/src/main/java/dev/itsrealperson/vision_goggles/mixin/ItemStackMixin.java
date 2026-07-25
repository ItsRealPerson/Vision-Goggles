package dev.itsrealperson.vision_goggles.mixin;

import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void vision$getMaxDamage(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof VisionGogglesItem) {
            cir.setReturnValue(dev.itsrealperson.vision_goggles.util.ModConfig.getGogglesDurability());
        }
    }
}
