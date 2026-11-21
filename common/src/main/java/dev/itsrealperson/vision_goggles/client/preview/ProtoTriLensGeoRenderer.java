package dev.itsrealperson.vision_goggles.client.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtils;

/**
 * Standalone, non-destructive GeckoLib 4 renderer for the Tri-Lens 3D prototypes.
 * Renders cached {@link BakedGeoModel} geometry directly without touching any production
 * goggle item/model/texture or {@code chassis_neutral.png}.
 */
@Environment(EnvType.CLIENT)
public final class ProtoTriLensGeoRenderer {

    public static final ResourceLocation ANIMATION_RESOURCE =
            new ResourceLocation(Vision_goggles.MOD_ID, "animations/item/proto_tri_lens.animation.json");

    // Center head-space geometry (Y in [24.75, 32.75], Z in [-11.25, +5.25]) around viewport origin
    public static final float PREVIEW_CENTER_OFFSET_X = 0.0f;
    public static final float PREVIEW_CENTER_OFFSET_Y = -29.0f / 16.0f;
    public static final float PREVIEW_CENTER_OFFSET_Z = 2.75f / 16.0f;

    private static final float VISOR_FLIP_UP_RADIANS = (float) Math.toRadians(60.0f);

    private ProtoTriLensGeoRenderer() {}

    public static BakedGeoModel getBakedModel(ProtoTriLensVariant variant) {
        if (variant == null) return null;
        return GeckoLibCache.getBakedModels().get(variant.getGeoModel());
    }

    /**
     * Renders the currently selected prototype in standalone turntable space.
     * Caller sets up viewport translation, scale, and orbit rotations.
     */
    public static void renderStandalonePreview(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ProtoTriLensVariant variant,
            int packedLight,
            boolean showHeadGuide
    ) {
        if (variant == null) return;
        BakedGeoModel bakedModel = getBakedModel(variant);
        if (bakedModel == null) return;

        poseStack.pushPose();
        poseStack.translate(PREVIEW_CENTER_OFFSET_X, PREVIEW_CENTER_OFFSET_Y, PREVIEW_CENTER_OFFSET_Z);

        if (showHeadGuide) {
            renderSteveHeadGuide(poseStack, bufferSource);
        }

        float visorT = ProtoTriLensPreviewState.getVisorProgress();
        renderBakedVariant(poseStack, bufferSource, bakedModel, variant, packedLight, visorT);
        poseStack.popPose();
    }

    /**
     * Renders the selected prototype aligned onto the local player's head in third-person (F5),
     * invoked ONLY when {@link ProtoTriLensPreviewState#isHeadPreviewEnabled()} is true.
     */
    public static void renderOnPlayerHead(
            PoseStack matrixStack,
            MultiBufferSource bufferSource,
            int packedLight,
            Object parentModel
    ) {
        ProtoTriLensVariant variant = ProtoTriLensPreviewState.getSelectedVariant();
        BakedGeoModel bakedModel = getBakedModel(variant);
        if (bakedModel == null) return;

        matrixStack.pushPose();
        if (parentModel instanceof HumanoidModel<?> humanoidModel) {
            humanoidModel.head.translateAndRotate(matrixStack);
        }

        // Convert Y-up Bedrock head space (head pivot at Y=24, +Y up, +X right in BakedGeoModel)
        // into HumanoidModel Y-down head space (neck at Y=0, -Y up, -X right) while preserving +1 determinant.
        matrixStack.scale(-1.0f, -1.0f, 1.0f);
        matrixStack.translate(0.0f, -24.0f / 16.0f, 0.0f);

        float visorT = ProtoTriLensPreviewState.getInGameVisorUpProgress();
        renderBakedVariant(matrixStack, bufferSource, bakedModel, variant, packedLight, visorT);
        matrixStack.popPose();
    }

    /**
     * Renders ONLY the {@code "visor"} bone of the selected Tri-Lens prototype in first-person
     * {@link net.minecraft.client.gui.GuiGraphics} space (omitting {@code "head"} straps so rear
     * straps do not occlude the view).
     *
     * <p>Caller sets up {@code GuiGraphics} screen translation, scale, and tactical tilt in Y-down
     * space. Applying {@code scale(1, -1, 1)} cancels the {@code GuiGraphics} ortho Y-flip so
     * {@code det(proj * modelView) = +1} (preserving GL CCW winding for {@link RenderType#eyes})
     * and maps Bedrock {@code +X/+Y/-Z} directly to screen {@code +X/-Y/-Z}.</p>
     *
     * @param visorDownProgress {@code 0.0f} = flipped UP on forehead, {@code 1.0f} = flipped DOWN over eyes
     * @param glowIntensity     emissive lens intensity {@code [0..1]}
     */
    public static void renderFirstPersonVisor(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ProtoTriLensVariant variant,
            int packedLight,
            float visorDownProgress,
            float glowIntensity
    ) {
        if (variant == null) return;
        BakedGeoModel bakedModel = getBakedModel(variant);
        if (bakedModel == null) return;

        GeoBone visorBone = findBoneByName(bakedModel.topLevelBones(), "visor");
        if (visorBone == null) return;

        float visorT = 1.0f - net.minecraft.util.Mth.clamp(visorDownProgress, 0.0f, 1.0f);

        poseStack.pushPose();
        // Cancel GuiGraphics Y-down ortho flip so +Y is up, +X is right, -Z is forward, and GL CCW winding stays +1
        poseStack.scale(1.0f, -1.0f, 1.0f);
        // Center the "visor" bone hinge pivot ([0, 27.5, -5.5]) at the local origin
        poseStack.translate(
                -visorBone.getPivotX() / 16.0f,
                -visorBone.getPivotY() / 16.0f,
                -visorBone.getPivotZ() / 16.0f
        );

        VertexConsumer baseConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(variant.getBaseTexture()));
        renderBoneRecursively(poseStack, visorBone, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, visorT, 1.0f, 1.0f, 1.0f, 1.0f);

        if (ProtoTriLensPreviewState.isGlowEnabled() && glowIntensity > 0.01f) {
            VertexConsumer glowConsumer = bufferSource.getBuffer(RenderType.eyes(variant.getGlowTexture()));
            renderBoneRecursively(poseStack, visorBone, glowConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, visorT, glowIntensity, glowIntensity, glowIntensity, 1.0f);
        }

        poseStack.popPose();
    }

    private static GeoBone findBoneByName(java.util.List<GeoBone> bones, String name) {
        if (bones == null) return null;
        for (GeoBone bone : bones) {
            if (name.equals(bone.getName())) {
                return bone;
            }
            GeoBone childMatch = findBoneByName(bone.getChildBones(), name);
            if (childMatch != null) {
                return childMatch;
            }
        }
        return null;
    }

    private static void renderBakedVariant(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BakedGeoModel bakedModel,
            ProtoTriLensVariant variant,
            int packedLight,
            float visorT
    ) {
        VertexConsumer baseConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(variant.getBaseTexture()));
        for (GeoBone bone : bakedModel.topLevelBones()) {
            renderBoneRecursively(poseStack, bone, baseConsumer, packedLight, OverlayTexture.NO_OVERLAY, visorT, 1.0f, 1.0f, 1.0f, 1.0f);
        }

        if (ProtoTriLensPreviewState.isGlowEnabled()) {
            VertexConsumer glowConsumer = bufferSource.getBuffer(RenderType.eyes(variant.getGlowTexture()));
            for (GeoBone bone : bakedModel.topLevelBones()) {
                renderBoneRecursively(poseStack, bone, glowConsumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, visorT, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    private static void renderBoneRecursively(
            PoseStack poseStack,
            GeoBone bone,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float visorT,
            float r,
            float g,
            float b,
            float a
    ) {
        if (bone.isHidden()) return;

        poseStack.pushPose();
        applyBoneTransforms(poseStack, bone, visorT);

        if (!bone.isHidingChildren()) {
            for (GeoCube cube : bone.getCubes()) {
                poseStack.pushPose();
                renderCube(poseStack, cube, buffer, packedLight, packedOverlay, r, g, b, a);
                poseStack.popPose();
            }
            for (GeoBone child : bone.getChildBones()) {
                renderBoneRecursively(poseStack, child, buffer, packedLight, packedOverlay, visorT, r, g, b, a);
            }
        } else {
            for (GeoCube cube : bone.getCubes()) {
                poseStack.pushPose();
                renderCube(poseStack, cube, buffer, packedLight, packedOverlay, r, g, b, a);
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private static void applyBoneTransforms(PoseStack poseStack, GeoBone bone, float visorT) {
        float posX = bone.getPosX();
        float posY = bone.getPosY();
        float posZ = bone.getPosZ();
        float rotX = bone.getRotX();
        float rotY = bone.getRotY();
        float rotZ = bone.getRotZ();

        // Apply proto_tri_lens.animation.json idle_down -> idle_up interpolation cleanly on "visor" bone
        if ("visor".equals(bone.getName())) {
            posY = visorT * 2.0f;
            posZ = visorT * -0.75f;
            rotX = visorT * VISOR_FLIP_UP_RADIANS;
            rotY = 0.0f;
            rotZ = 0.0f;
        }

        poseStack.translate(-posX / 16.0f, posY / 16.0f, posZ / 16.0f);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        if (rotZ != 0.0f || rotY != 0.0f || rotX != 0.0f) {
            if (rotZ != 0.0f) poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(rotZ));
            if (rotY != 0.0f) poseStack.mulPose(com.mojang.math.Axis.YP.rotation(rotY));
            if (rotX != 0.0f) poseStack.mulPose(com.mojang.math.Axis.XP.rotation(rotX));
        }
        RenderUtils.scaleMatrixForBone(poseStack, bone);
        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);
    }

    private static void renderCube(
            PoseStack poseStack,
            GeoCube cube,
            VertexConsumer buffer,
            int packedLight,
            int packedOverlay,
            float r,
            float g,
            float b,
            float a
    ) {
        RenderUtils.translateToPivotPoint(poseStack, cube);
        RenderUtils.rotateMatrixAroundCube(poseStack, cube);
        RenderUtils.translateAwayFromPivotPoint(poseStack, cube);

        Matrix4f poseMatrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;
            Vector3f normal = normalMatrix.transform(new Vector3f(quad.normal()));
            // Match GeckoLib's planar normal inversion on flat cubes if needed
            if ((cube.size().y() == 0 || cube.size().z() == 0) && normal.x() < 0) {
                normal.mul(-1, 1, 1);
            }
            if ((cube.size().x() == 0 || cube.size().z() == 0) && normal.y() < 0) {
                normal.mul(1, -1, 1);
            }
            if ((cube.size().x() == 0 || cube.size().y() == 0) && normal.z() < 0) {
                normal.mul(1, 1, -1);
            }

            for (GeoVertex vertex : quad.vertices()) {
                Vector3f pos = vertex.position();
                Vector4f transformed = poseMatrix.transform(new Vector4f(pos.x(), pos.y(), pos.z(), 1.0f));
                buffer.vertex(
                        transformed.x(), transformed.y(), transformed.z(),
                        r, g, b, a,
                        vertex.texU(), vertex.texV(),
                        packedOverlay, packedLight,
                        normal.x(), normal.y(), normal.z()
                );
            }
        }
    }

    private static void renderSteveHeadGuide(PoseStack poseStack, MultiBufferSource bufferSource) {
        // Standard Steve head bounds in Bedrock model space: [-4..4, 24..32, -4..4]
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(
                poseStack, lineConsumer,
                -4.0 / 16.0, 24.0 / 16.0, -4.0 / 16.0,
                4.0 / 16.0, 32.0 / 16.0, 4.0 / 16.0,
                0.38f, 0.72f, 0.85f, 0.65f
        );
        // Eye-level reference line on the front face (Y = 28, Z = -4.02)
        Matrix4f mat = poseStack.last().pose();
        Matrix3f norm = poseStack.last().normal();
        lineConsumer.vertex(mat, -4.0f / 16.0f, 28.0f / 16.0f, -4.02f / 16.0f)
                .color(0.45f, 0.90f, 0.55f, 0.75f).normal(norm, 1.0f, 0.0f, 0.0f).endVertex();
        lineConsumer.vertex(mat, 4.0f / 16.0f, 28.0f / 16.0f, -4.02f / 16.0f)
                .color(0.45f, 0.90f, 0.55f, 0.75f).normal(norm, 1.0f, 0.0f, 0.0f).endVertex();
    }
}
