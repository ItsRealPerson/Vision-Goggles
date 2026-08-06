package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import dev.itsrealperson.vision_goggles.network.BatterySyncPacket;
import dev.itsrealperson.vision_goggles.network.EquipPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class VisionGogglesItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<VisionMode> supportedModes;
    private final IntSupplier batteryCapacity;

    public VisionGogglesItem(IntSupplier batteryCapacity, VisionMode... modes) {
        super(new Item.Properties().stacksTo(1).durability(dev.itsrealperson.vision_goggles.util.ModConfig.getGogglesDurability()));
        this.batteryCapacity = batteryCapacity;
        this.supportedModes = Arrays.asList(modes);
        if (this.supportedModes.isEmpty()) throw new IllegalArgumentException("Must have at least one vision mode");
    }

    public int getBatteryCapacity() {
        return batteryCapacity.getAsInt();
    }
    
    public int getBatteryCapacity(ItemStack stack) {
        return getBatteryCapacity();
    }

    public VisionMode getVisionMode() {
        return supportedModes.get(0); // Default mode
    }

    public List<VisionMode> getSupportedModes() {
        return supportedModes;
    }

    public boolean hasModule(ItemStack stack, String moduleName) {
        // Por defecto, las gafas especializadas pueden tener ciertos módulos integrados
        // o simplemente no tener soporte para módulos adicionales.
        return false;
    }

    public void serverTick(ItemStack stack, ServerPlayer player) {
        CompoundTag nbt = stack.getOrCreateTag();
        float maxBattery = (float) getBatteryCapacity(stack);
        
        if (!nbt.contains(ModConstants.TAG_BATTERY)) nbt.putFloat(ModConstants.TAG_BATTERY, maxBattery);
        float currentBattery = nbt.getFloat(ModConstants.TAG_BATTERY);
        
        int modeId = nbt.getInt(ModConstants.TAG_MODE);
        VisionMode mode = VisionMode.byId(modeId);

        if (nbt.getBoolean(ModConstants.TAG_ACTIVE)) {
            if (currentBattery > 0) {
                float baseDrain = (mode == VisionMode.THERMAL) ? 2.0f : 1.0f;
                float drain = baseDrain * dev.itsrealperson.vision_goggles.util.ModConfig.getBatteryDrainMultiplier();
                currentBattery = Math.max(0, currentBattery - drain);
                nbt.putFloat(ModConstants.TAG_BATTERY, currentBattery);
                
                // Effect Logic
                boolean shouldApplyNV = true;
                if (modeId == -1 || mode == VisionMode.BIOMETRIC) {
                    shouldApplyNV = false;
                } else if (mode == VisionMode.HYDRO && !player.isUnderWater()) {
                    shouldApplyNV = false;
                }

                if (shouldApplyNV) {
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, false));
                } else {
                    cleanUpEffect(player);
                }
                
                if (currentBattery <= 0) {
                    nbt.putBoolean(ModConstants.TAG_ACTIVE, false);
                    cleanUpEffect(player);
                }
            } else {
                nbt.putBoolean(ModConstants.TAG_ACTIVE, false);
                cleanUpEffect(player);
            }
        }

        // Optimization: Sync only if battery changed significantly or reached zero
        if (player.tickCount % 20 == 0) {
            float lastSync = nbt.contains(ModConstants.TAG_LAST_SYNC) ? nbt.getFloat(ModConstants.TAG_LAST_SYNC) : -1.0f;
            if (Math.abs(currentBattery - lastSync) >= 1.0f || (currentBattery <= 0 && lastSync > 0)) {
                NetworkManager.INSTANCE.sendToPlayer(player, new BatterySyncPacket(currentBattery));
                nbt.putFloat(ModConstants.TAG_LAST_SYNC, currentBattery);
            }
        }
    }

    public static void cleanUpEffect(ServerPlayer player) {
        if (player.hasEffect(MobEffects.NIGHT_VISION)) {
            MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
            if (effect != null && !effect.isVisible() && !effect.showIcon()) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof Player player && level.isClientSide) {
            NetworkManager.INSTANCE.sendToServer(new EquipPacket());
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 10; // 0.5 segundos
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Añadir controladores si hay animaciones
    }

    private final java.util.function.Supplier<Object> renderProvider = new java.util.function.Supplier<Object>() {
        private software.bernie.geckolib.animatable.client.RenderProvider geoRenderProvider;
        @Override
        public Object get() {
            if (this.geoRenderProvider == null) {
                createRenderer(provider -> this.geoRenderProvider = (software.bernie.geckolib.animatable.client.RenderProvider) provider);
            }
            return this.geoRenderProvider;
        }
    };

    @Override
    public void createRenderer(java.util.function.Consumer<Object> consumer) {
        consumer.accept(new software.bernie.geckolib.animatable.client.RenderProvider() {
            private dev.itsrealperson.vision_goggles.client.renderer.VisionGogglesGeoRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new dev.itsrealperson.vision_goggles.client.renderer.VisionGogglesGeoRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public java.util.function.Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
