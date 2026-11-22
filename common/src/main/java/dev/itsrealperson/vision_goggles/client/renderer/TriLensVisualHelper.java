package dev.itsrealperson.vision_goggles.client.renderer;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.ProModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;

/**
 * Shared Tri-Lens asset resolution and dynamic per-bone NBT visibility helper
 * for all 6 goggle items and the neutral Modification Station chassis.
 */
@Environment(EnvType.CLIENT)
public final class TriLensVisualHelper {

    public static final float CENTER_OFFSET_X = 0.0f;
    public static final float CENTER_OFFSET_Y = -29.0f / 16.0f;
    public static final float CENTER_OFFSET_Z = 2.75f / 16.0f;

    public static final String TAG_NEUTRAL_CHASSIS = "vg_neutral_chassis";

    public static final ResourceLocation ANIMATION_LOCATION =
            new ResourceLocation(Vision_goggles.MOD_ID, "animations/item/vision_goggles.animation.json");

    public static final ResourceLocation CHASSIS_NEUTRAL_TEXTURE =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/chassis_neutral.png");

    public static final ResourceLocation TRI_LENS_NVG_TEXTURE =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_nvg.png");
    public static final ResourceLocation TRI_LENS_NVG_GLOW =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_nvg_glow.png");

    public static final ResourceLocation TRI_LENS_THERMAL_TEXTURE =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_thermal.png");
    public static final ResourceLocation TRI_LENS_THERMAL_GLOW =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_thermal_glow.png");

    public static final ResourceLocation TRI_LENS_BIO_TEXTURE =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_bio.png");
    public static final ResourceLocation TRI_LENS_BIO_GLOW =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_bio_glow.png");

    public static final ResourceLocation TRI_LENS_HYDRO_TEXTURE =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_hydro.png");
    public static final ResourceLocation TRI_LENS_HYDRO_GLOW =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_hydro_glow.png");

    public static final ResourceLocation TRI_LENS_MODULAR_TEXTURE =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_modular.png");
    public static final ResourceLocation TRI_LENS_MODULAR_GLOW =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_modular_glow.png");

    public static final ResourceLocation TRI_LENS_PRO_MODULAR_TEXTURE =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_pro_modular.png");
    public static final ResourceLocation TRI_LENS_PRO_MODULAR_GLOW =
            new ResourceLocation(Vision_goggles.MOD_ID, "textures/entities/tri_lens_pro_modular_glow.png");

    public static final String BONE_PRO_ARMOR = "mod_pro_armor";
    public static final String BONE_SOLAR = "mod_solar";
    public static final String BONE_BATTERY_PACK = "mod_battery_pack";
    public static final String BONE_FLASHLIGHT = "mod_flashlight";
    public static final String BONE_LUA_CHIP = "mod_lua_chip";
    public static final String BONE_SONAR = "mod_sonar";
    public static final String BONE_VITAL_INFO = "mod_vital_info";
    public static final String BONE_ENVIRONMENT = "mod_environment";
    public static final String BONE_ZOOM = "mod_zoom";
    public static final String BONE_OPTICS_BASE = "mod_optics_base";
    public static final String BONE_OPTICS_NVG = "mod_optics_nvg";
    public static final String BONE_OPTICS_THERMAL = "mod_optics_thermal";
    public static final String BONE_OPTICS_BIO = "mod_optics_bio";
    public static final String BONE_OPTICS_HYDRO = "mod_optics_hydro";

    public static final List<String> ALL_OPTIONAL_BONES = List.of(
            BONE_PRO_ARMOR,
            BONE_SOLAR,
            BONE_BATTERY_PACK,
            BONE_FLASHLIGHT,
            BONE_LUA_CHIP,
            BONE_SONAR,
            BONE_VITAL_INFO,
            BONE_ENVIRONMENT,
            BONE_ZOOM,
            BONE_OPTICS_BASE,
            BONE_OPTICS_NVG,
            BONE_OPTICS_THERMAL,
            BONE_OPTICS_BIO,
            BONE_OPTICS_HYDRO
    );

    private TriLensVisualHelper() {}

    public static boolean isNeutralChassis(@Nullable ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.hasTag()
                && stack.getTag().getBoolean(TAG_NEUTRAL_CHASSIS);
    }

    @Nullable
    public static VisionGogglesItem resolveGogglesItem(@Nullable ItemStack stack, @Nullable VisionGogglesItem item) {
        if (item != null) {
            return item;
        }
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof VisionGogglesItem goggles) {
            return goggles;
        }
        return null;
    }

    public static ResourceLocation getGeoModelLocation(@Nullable ItemStack stack, @Nullable VisionGogglesItem item) {
        VisionGogglesItem resolved = resolveGogglesItem(stack, item);
        String path = (resolved != null)
                ? BuiltInRegistries.ITEM.getKey(resolved).getPath()
                : "modular_goggles";
        return new ResourceLocation(Vision_goggles.MOD_ID, "geo/item/" + path + ".geo.json");
    }

    public static ResourceLocation getBaseTexture(@Nullable ItemStack stack, @Nullable VisionGogglesItem item) {
        if (isNeutralChassis(stack)) {
            return CHASSIS_NEUTRAL_TEXTURE;
        }
        VisionGogglesItem resolved = resolveGogglesItem(stack, item);
        if (resolved instanceof ProModularGogglesItem) {
            return TRI_LENS_PRO_MODULAR_TEXTURE;
        }
        if (resolved instanceof ModularGogglesItem) {
            return TRI_LENS_MODULAR_TEXTURE;
        }
        if (resolved != null) {
            VisionMode mode = resolved.getVisionMode();
            if (mode == VisionMode.THERMAL) return TRI_LENS_THERMAL_TEXTURE;
            if (mode == VisionMode.BIOMETRIC) return TRI_LENS_BIO_TEXTURE;
            if (mode == VisionMode.HYDRO) return TRI_LENS_HYDRO_TEXTURE;
        }
        return TRI_LENS_NVG_TEXTURE;
    }

    @Nullable
    public static ResourceLocation getGlowTexture(@Nullable ItemStack stack, @Nullable VisionGogglesItem item) {
        if (isNeutralChassis(stack)) {
            return null;
        }
        VisionGogglesItem resolved = resolveGogglesItem(stack, item);
        if (resolved instanceof ProModularGogglesItem) {
            return TRI_LENS_PRO_MODULAR_GLOW;
        }
        if (resolved instanceof ModularGogglesItem) {
            return TRI_LENS_MODULAR_GLOW;
        }
        if (resolved != null) {
            VisionMode mode = resolved.getVisionMode();
            if (mode == VisionMode.THERMAL) return TRI_LENS_THERMAL_GLOW;
            if (mode == VisionMode.BIOMETRIC) return TRI_LENS_BIO_GLOW;
            if (mode == VisionMode.HYDRO) return TRI_LENS_HYDRO_GLOW;
        }
        return TRI_LENS_NVG_GLOW;
    }

    public static void applyModularBoneVisibility(
            @Nullable BakedGeoModel model,
            @Nullable ItemStack stack,
            @Nullable VisionGogglesItem item
    ) {
        if (model == null) {
            return;
        }

        // BakedGeoModel is a cached singleton shared across all stacks: always reset all 14 mod_* bones first.
        for (String boneName : ALL_OPTIONAL_BONES) {
            setBoneHidden(model, boneName, false);
        }

        VisionGogglesItem resolved = resolveGogglesItem(stack, item);
        if (isNeutralChassis(stack) || !(resolved instanceof ModularGogglesItem modular)) {
            for (String boneName : ALL_OPTIONAL_BONES) {
                setBoneHidden(model, boneName, !BONE_OPTICS_BASE.equals(boneName));
            }
            return;
        }

        boolean hasValidStack = stack != null && !stack.isEmpty();
        boolean isPro = modular instanceof ProModularGogglesItem;

        boolean hasSolar = hasValidStack && modular.hasModule(stack, ModConstants.ID_SOLAR);
        boolean hasBattery = hasValidStack && modular.hasBatteryExpansion(stack);
        boolean hasFlashlight = hasValidStack && modular.hasModule(stack, ModConstants.ID_FLASHLIGHT);
        boolean hasLuaChip = hasValidStack && modular.hasModule(stack, "programmable");
        boolean hasSonar = hasValidStack && modular.hasModule(stack, ModConstants.ID_SONAR);
        boolean hasVitalInfo = hasValidStack && modular.hasModule(stack, ModConstants.ID_VITAL_INFO);
        boolean hasEnvironment = hasValidStack && modular.hasModule(stack, ModConstants.ID_ENVIRONMENT);
        boolean hasZoom = hasValidStack && modular.hasModule(stack, ModConstants.ID_ZOOM);

        boolean hasNvg = hasValidStack && modular.hasModule(stack, ModConstants.ID_NIGHT_VISION);
        boolean hasThermal = hasValidStack && modular.hasModule(stack, ModConstants.ID_THERMAL);
        boolean hasBio = hasValidStack && modular.hasModule(stack, ModConstants.ID_BIOMETRIC);
        boolean hasHydro = hasValidStack && modular.hasModule(stack, ModConstants.ID_HYDRO);
        boolean hasAnyOptics = hasNvg || hasThermal || hasBio || hasHydro;

        setBoneHidden(model, BONE_PRO_ARMOR, !isPro);
        setBoneHidden(model, BONE_SOLAR, !hasSolar);
        setBoneHidden(model, BONE_BATTERY_PACK, !hasBattery);
        setBoneHidden(model, BONE_FLASHLIGHT, !hasFlashlight);
        setBoneHidden(model, BONE_LUA_CHIP, !hasLuaChip);
        setBoneHidden(model, BONE_SONAR, !hasSonar);
        setBoneHidden(model, BONE_VITAL_INFO, !hasVitalInfo);
        setBoneHidden(model, BONE_ENVIRONMENT, !hasEnvironment);
        setBoneHidden(model, BONE_ZOOM, !hasZoom);

        setBoneHidden(model, BONE_OPTICS_NVG, !hasNvg);
        setBoneHidden(model, BONE_OPTICS_THERMAL, !hasThermal);
        setBoneHidden(model, BONE_OPTICS_BIO, !hasBio);
        setBoneHidden(model, BONE_OPTICS_HYDRO, !hasHydro);
        setBoneHidden(model, BONE_OPTICS_BASE, hasAnyOptics);
    }

    private static void setBoneHidden(BakedGeoModel model, String boneName, boolean hidden) {
        model.getBone(boneName).ifPresent(bone -> bone.setHidden(hidden));
        GeoBone walked = findBoneRecursive(model.topLevelBones(), boneName);
        if (walked != null) {
            walked.setHidden(hidden);
        }
    }

    @Nullable
    private static GeoBone findBoneRecursive(List<GeoBone> bones, String name) {
        if (bones == null) {
            return null;
        }
        for (GeoBone bone : bones) {
            if (name.equals(bone.getName())) {
                return bone;
            }
            GeoBone childMatch = findBoneRecursive(bone.getChildBones(), name);
            if (childMatch != null) {
                return childMatch;
            }
        }
        return null;
    }
}
