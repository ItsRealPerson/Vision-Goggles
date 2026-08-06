package dev.itsrealperson.vision_goggles.forge.mixin;

import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import dev.itsrealperson.vision_goggles.client.renderer.VisionGogglesGeoRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "initializeClient", at = @At("HEAD"), remap = false)
    private void vision_goggles$initializeClient(Consumer<IClientItemExtensions> consumer, CallbackInfo ci) {
        if ((Object) this instanceof VisionGogglesItem) {
            consumer.accept(new IClientItemExtensions() {
                private VisionGogglesGeoRenderer renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (this.renderer == null) {
                        this.renderer = new VisionGogglesGeoRenderer();
                    }
                    return this.renderer;
                }
            });
        }
    }
}
