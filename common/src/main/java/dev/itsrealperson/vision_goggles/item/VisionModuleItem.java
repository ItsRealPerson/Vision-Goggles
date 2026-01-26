package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.world.item.Item;

public class VisionModuleItem extends Item {
    private final VisionMode visionMode;

    public VisionModuleItem(VisionMode visionMode) {
        super(new Item.Properties().stacksTo(64));
        this.visionMode = visionMode;
    }

    public VisionMode getVisionMode() {
        return visionMode;
    }
}
