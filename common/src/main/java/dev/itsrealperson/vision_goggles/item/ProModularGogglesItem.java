package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProModularGogglesItem extends ModularGogglesItem {

    public ProModularGogglesItem() {
        super(4); // Pro version has 4 slots
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable("tooltip.vision_goggles.pro_version").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
