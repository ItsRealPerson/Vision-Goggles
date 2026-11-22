package dev.itsrealperson.vision_goggles.client.model;

import dev.itsrealperson.vision_goggles.client.renderer.TriLensVisualHelper;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.model.GeoModel;

public class VisionGogglesGeoModel extends GeoModel<VisionGogglesItem> {

    private ItemStack currentStack;

    public void setCurrentStack(ItemStack stack) {
        this.currentStack = stack;
    }

    public ItemStack getCurrentStack() {
        return this.currentStack;
    }

    @Override
    public ResourceLocation getModelResource(VisionGogglesItem animatable) {
        return TriLensVisualHelper.getGeoModelLocation(this.currentStack, animatable);
    }

    @Override
    public ResourceLocation getTextureResource(VisionGogglesItem animatable) {
        return TriLensVisualHelper.getBaseTexture(this.currentStack, animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(VisionGogglesItem animatable) {
        return TriLensVisualHelper.ANIMATION_LOCATION;
    }
}
