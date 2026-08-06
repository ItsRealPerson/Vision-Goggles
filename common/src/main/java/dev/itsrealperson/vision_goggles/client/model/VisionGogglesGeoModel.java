package dev.itsrealperson.vision_goggles.client.model;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class VisionGogglesGeoModel extends GeoModel<VisionGogglesItem> {

    private net.minecraft.world.item.ItemStack currentStack;

    public void setCurrentStack(net.minecraft.world.item.ItemStack stack) {
        this.currentStack = stack;
    }

    @Override
    public ResourceLocation getModelResource(VisionGogglesItem animatable) {
        String name = BuiltInRegistries.ITEM.getKey(animatable).getPath();
        
        if (currentStack != null && animatable instanceof dev.itsrealperson.vision_goggles.item.ModularGogglesItem modular) {
            java.util.List<dev.itsrealperson.vision_goggles.util.VisionMode> modes = modular.getModes(currentStack);
            if (!modes.isEmpty()) {
                dev.itsrealperson.vision_goggles.util.VisionMode mode = modes.get(0);
                for (dev.itsrealperson.vision_goggles.util.VisionMode m : modes) {
                    if (m == dev.itsrealperson.vision_goggles.util.VisionMode.THERMAL) { mode = m; break; }
                    if (m == dev.itsrealperson.vision_goggles.util.VisionMode.BIOMETRIC) { mode = m; break; }
                    if (m == dev.itsrealperson.vision_goggles.util.VisionMode.HYDRO) { mode = m; }
                }
                String base = (animatable instanceof dev.itsrealperson.vision_goggles.item.ProModularGogglesItem) ? "pro_modular_goggles" : "modular_goggles";
                
                if (mode == dev.itsrealperson.vision_goggles.util.VisionMode.THERMAL) return new ResourceLocation(Vision_goggles.MOD_ID, "geo/item/" + base + "_thermal.geo.json");
                if (mode == dev.itsrealperson.vision_goggles.util.VisionMode.BIOMETRIC) return new ResourceLocation(Vision_goggles.MOD_ID, "geo/item/" + base + "_bio.geo.json");
                if (mode == dev.itsrealperson.vision_goggles.util.VisionMode.HYDRO) return new ResourceLocation(Vision_goggles.MOD_ID, "geo/item/" + base + "_hydro.geo.json");
            }
        }
        
        return new ResourceLocation(Vision_goggles.MOD_ID, "geo/item/" + name + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VisionGogglesItem animatable) {
        return new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/nvg_texture.png"); // Default
    }

    @Override
    public ResourceLocation getAnimationResource(VisionGogglesItem animatable) {
        String name = BuiltInRegistries.ITEM.getKey(animatable).getPath();
        return new ResourceLocation(Vision_goggles.MOD_ID, "animations/item/" + name + ".animation.json");
    }
}
