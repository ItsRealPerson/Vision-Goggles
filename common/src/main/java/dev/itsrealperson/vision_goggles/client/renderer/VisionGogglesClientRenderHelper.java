package dev.itsrealperson.vision_goggles.client.renderer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.client.RenderProvider;

import java.util.function.Consumer;

public class VisionGogglesClientRenderHelper {
    public static void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
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
