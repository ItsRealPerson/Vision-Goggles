package dev.itsrealperson.vision_goggles.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_goggles.client.model.VisionGogglesGeoModel;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Renders the unified 64x64 Tri-Lens BakedGeoModel outside of the item pipeline
 * (third-person head layer and first-person flip overlay), posing {@code visor_pivot}
 * by flip progress (0 = flipped up, 1 = flipped down).
 */
public final class TriLensManualRenderer {

    public static final String BONE_VISOR_PIVOT = "visor_pivot";
    private static final float UP_ROT_RAD = (float) Math.toRadians(60.0);
    private static final float UP_POS_Y = 2.0f;
    private static final float UP_POS_Z = -0.75f;

    private static VisionGogglesGeoRenderer renderer;

    private TriLensManualRenderer() {}

    private static VisionGogglesGeoRenderer renderer() {
        if (renderer == null) {
            renderer = new VisionGogglesGeoRenderer();
        }
        return renderer;
    }

    /** Pivot of visor_pivot in model pixels (y up), used to center it for GUI rendering. */
    public static final float PIVOT_Y_PX = 27.5f;
    public static final float PIVOT_Z_PX = -5.5f;

    public static void render(
            ItemStack stack,
            VisionGogglesItem item,
            PoseStack poseStack,
            MultiBufferSource buffers,
            float progress,
            float partialTick,
            int packedLight,
            boolean glow,
            float glowIntensity
    ) {
        VisionGogglesGeoRenderer r = renderer();
        VisionGogglesGeoModel geo = (VisionGogglesGeoModel) r.getGeoModel();
        geo.setCurrentStack(stack);
        ResourceLocation loc = TriLensVisualHelper.getGeoModelLocation(stack, item);
        BakedGeoModel baked = geo.getBakedModel(loc);
        if (baked == null) {
            return;
        }
        TriLensVisualHelper.applyModularBoneVisibility(baked, stack, item);

        float up = 1.0f - Math.max(0.0f, Math.min(1.0f, progress));
        GeoBone visor = baked.getBone(BONE_VISOR_PIVOT).orElse(null);
        if (visor != null) {
            visor.setRotX(UP_ROT_RAD * up);
            visor.setRotY(0.0f);
            visor.setRotZ(0.0f);
            visor.setPosX(0.0f);
            visor.setPosY(UP_POS_Y * up);
            visor.setPosZ(UP_POS_Z * up);
        }

        RenderType baseType = RenderType.entityCutoutNoCull(TriLensVisualHelper.getBaseTexture(stack, item));
        var baseBuf = buffers.getBuffer(baseType);
        for (GeoBone bone : baked.topLevelBones()) {
            r.renderRecursively(poseStack, item, bone, baseType, buffers, baseBuf, false, partialTick,
                    packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        }

        ResourceLocation glowTex = TriLensVisualHelper.getGlowTexture(stack, item);
        if (glow && glowTex != null && glowIntensity > 0.01f) {
            RenderType glowType = RenderType.eyes(glowTex);
            var glowBuf = buffers.getBuffer(glowType);
            for (GeoBone bone : baked.topLevelBones()) {
                r.renderRecursively(poseStack, item, bone, glowType, buffers, glowBuf, false, partialTick,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        glowIntensity, glowIntensity, glowIntensity, 1.0f);
            }
        }

        if (visor != null) {
            visor.setRotX(0.0f);
            visor.setPosY(0.0f);
            visor.setPosZ(0.0f);
        }
    }
}
