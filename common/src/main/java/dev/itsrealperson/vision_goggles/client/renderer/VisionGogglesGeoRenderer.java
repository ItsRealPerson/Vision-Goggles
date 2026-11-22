package dev.itsrealperson.vision_goggles.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.client.model.VisionGogglesGeoModel;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class VisionGogglesGeoRenderer extends GeoItemRenderer<VisionGogglesItem> {

    public VisionGogglesGeoRenderer() {
        super(new VisionGogglesGeoModel());
        addRenderLayer(new TriLensGlowGeoLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(VisionGogglesItem animatable) {
        return TriLensVisualHelper.getBaseTexture(getCurrentItemStack(), animatable);
    }

    @Override
    public RenderType getRenderType(
            VisionGogglesItem animatable,
            ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick
    ) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext transformType,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (this.model instanceof VisionGogglesGeoModel vgModel) {
            vgModel.setCurrentStack(stack);
        }
        poseStack.pushPose();
        poseStack.translate(
                TriLensVisualHelper.CENTER_OFFSET_X,
                TriLensVisualHelper.CENTER_OFFSET_Y,
                TriLensVisualHelper.CENTER_OFFSET_Z
        );
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public void preRender(
            PoseStack poseStack,
            VisionGogglesItem animatable,
            BakedGeoModel model,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        ItemStack stack = getCurrentItemStack();
        if (getGeoModel() instanceof VisionGogglesGeoModel vgModel) {
            vgModel.setCurrentStack(stack);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        TriLensVisualHelper.applyModularBoneVisibility(model, stack, animatable);
    }

    private static final class TriLensGlowGeoLayer extends GeoRenderLayer<VisionGogglesItem> {
        private final VisionGogglesGeoRenderer itemRenderer;

        private TriLensGlowGeoLayer(VisionGogglesGeoRenderer renderer) {
            super(renderer);
            this.itemRenderer = renderer;
        }

        @Override
        public void render(
                PoseStack poseStack,
                VisionGogglesItem animatable,
                BakedGeoModel bakedModel,
                RenderType renderType,
                MultiBufferSource bufferSource,
                VertexConsumer buffer,
                float partialTick,
                int packedLight,
                int packedOverlay
        ) {
            ItemStack stack = this.itemRenderer.getCurrentItemStack();
            ResourceLocation glowTexture = TriLensVisualHelper.getGlowTexture(stack, animatable);
            if (glowTexture == null) {
                return;
            }
            RenderType glowRenderType = RenderType.eyes(glowTexture);
            getRenderer().reRender(
                    bakedModel,
                    poseStack,
                    bufferSource,
                    animatable,
                    glowRenderType,
                    bufferSource.getBuffer(glowRenderType),
                    partialTick,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f
            );
        }
    }
}
