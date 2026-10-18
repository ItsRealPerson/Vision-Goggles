package dev.itsrealperson.vision_goggles.fabric.mixin.client;

import dev.itsrealperson.vision_goggles.item.CustomReequipItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;

    @Unique private int vision_goggles$lastSelectedSlot = -1;
    @Unique private boolean vision_goggles$slotChanged = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void vision_goggles$trackSlotChange(CallbackInfo ci) {
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            int currentSlot = player.getInventory().selected;
            this.vision_goggles$slotChanged = (currentSlot != this.vision_goggles$lastSelectedSlot);
            this.vision_goggles$lastSelectedSlot = currentSlot;
        } else {
            this.vision_goggles$slotChanged = false;
        }
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"
        )
    )
    private boolean vision_goggles$redirectMatches(ItemStack oldStack, ItemStack newStack) {
        if (ItemStack.matches(oldStack, newStack)) {
            return true;
        }

        if (!oldStack.isEmpty() && oldStack.getItem() instanceof CustomReequipItem custom) {
            boolean isMainHand = (oldStack == this.mainHandItem);
            boolean slotChanged = isMainHand && this.vision_goggles$slotChanged;

            boolean shouldReequip = custom.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
            if (!shouldReequip) {
                return true;
            }
        }

        return false;
    }
}
