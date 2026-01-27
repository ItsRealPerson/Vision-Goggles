package dev.itsrealperson.vision_goggles.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

public class ProModularGogglesItem extends ModularGogglesItem {

    public ProModularGogglesItem() {
        super();
    }

    @Override
    public int getMaxModules() {
        return 4; // Pro version has 4 slots
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("tooltip.vision_goggles.pro_version").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
    }
}