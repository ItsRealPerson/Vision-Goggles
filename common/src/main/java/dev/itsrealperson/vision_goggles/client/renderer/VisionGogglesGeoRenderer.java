package dev.itsrealperson.vision_goggles.client.renderer;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.client.model.VisionGogglesGeoModel;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import java.util.List;

public class VisionGogglesGeoRenderer extends GeoItemRenderer<VisionGogglesItem> {

    private static final ResourceLocation NVG_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/nvg_texture.png");
    private static final ResourceLocation THERMAL_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/thermal_vision.png");
    private static final ResourceLocation HYDRO_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/hidro_vision.png");
    private static final ResourceLocation BIO_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/bio_vision.png");

    public VisionGogglesGeoRenderer() {
        super(new VisionGogglesGeoModel());
    }

    @Override
    public ResourceLocation getTextureLocation(VisionGogglesItem animatable) {
        ItemStack stack = getCurrentItemStack();
        if (stack == null) return super.getTextureLocation(animatable);

        VisionMode mode = animatable.getVisionMode();

        if (animatable instanceof ModularGogglesItem modular) {
            List<VisionMode> modes = modular.getModes(stack);
            if (!modes.isEmpty()) {
                // Prioridad
                for (VisionMode m : modes) {
                    if (m == VisionMode.THERMAL) { mode = m; break; }
                    if (m == VisionMode.BIOMETRIC) { mode = m; break; }
                    if (m == VisionMode.HYDRO) { mode = m; }
                }
                if (mode == null) mode = modes.get(0);
            }
        }

        if (mode == VisionMode.THERMAL) return THERMAL_TEXTURE;
        if (mode == VisionMode.HYDRO) return HYDRO_TEXTURE;
        if (mode == VisionMode.BIOMETRIC) return BIO_TEXTURE;
        
        return NVG_TEXTURE;
    }

    @Override
    public void preRender(com.mojang.blaze3d.vertex.PoseStack poseStack, VisionGogglesItem animatable, software.bernie.geckolib.cache.object.BakedGeoModel model, MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        ItemStack stack = getCurrentItemStack();
        if (stack != null && getGeoModel() instanceof VisionGogglesGeoModel vgModel) {
            vgModel.setCurrentStack(stack);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
