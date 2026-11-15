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
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

public class VisionGogglesItem extends Item implements GeoItem, CustomReequipItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final List<VisionMode> supportedModes;
    private final IntSupplier batteryCapacity;

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged) return true;
        return !ItemStack.isSameItem(oldStack, newStack);
    }

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

    public static VisionMode getActiveVisionMode(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof VisionGogglesItem goggles)) {
            return null;
        }
        CompoundTag nbt = stack.getTag();
        int nbtModeId = (nbt != null && nbt.contains(ModConstants.TAG_MODE)) ? nbt.getInt(ModConstants.TAG_MODE) : -1;
        if (goggles instanceof ModularGogglesItem modular) {
            List<VisionMode> modes = modular.getModes(stack);
            if (modes.isEmpty()) return null;
            if (nbtModeId != -1) {
                VisionMode requested = VisionMode.byId(nbtModeId);
                if (requested != null && modes.contains(requested)) {
                    return requested;
                }
            }
            return modes.get(0);
        } else {
            if (nbtModeId != -1) {
                VisionMode requested = VisionMode.byId(nbtModeId);
                if (requested != null && goggles.getSupportedModes().contains(requested)) {
                    return requested;
                }
            }
            return goggles.getVisionMode();
        }
    }

    public void serverTick(ItemStack stack, ServerPlayer player) {
        CompoundTag nbt = stack.getOrCreateTag();
        float maxBattery = (float) getBatteryCapacity(stack);
        
        if (!nbt.contains(ModConstants.TAG_BATTERY)) nbt.putFloat(ModConstants.TAG_BATTERY, maxBattery);
        float currentBattery = nbt.getFloat(ModConstants.TAG_BATTERY);
        
        VisionMode mode = getActiveVisionMode(stack);

        if (nbt.getBoolean(ModConstants.TAG_ACTIVE)) {
            if (currentBattery > 0) {
                float baseDrain = (mode != null) ? ((mode == VisionMode.THERMAL) ? 2.0f : 1.0f) : 0.0f;
                if (this instanceof ModularGogglesItem modular) {
                    for (String modId : modular.getModules(stack)) {
                        dev.itsrealperson.vision_goggles.scripting.LuaCustomModule luaMod =
                            dev.itsrealperson.vision_goggles.scripting.LuaModuleManager.findModuleByIdOrPath(modId);
                        if (luaMod != null && luaMod.getBatteryDrain() > 0) {
                            baseDrain += luaMod.getBatteryDrain();
                        }
                    }
                }
                float drain = baseDrain * dev.itsrealperson.vision_goggles.util.ModConfig.getBatteryDrainMultiplier();
                currentBattery = Math.max(0, currentBattery - drain);
                nbt.putFloat(ModConstants.TAG_BATTERY, currentBattery);
                
                // Night vision is handled purely client-side via LightTexture mixin;
                // clean up any legacy invisible NIGHT_VISION effect from older saves.
                cleanUpEffect(player);
                
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

    private static final RawAnimation IDLE_DOWN = RawAnimation.begin().thenLoop("idle_down");
    private static final RawAnimation IDLE_UP = RawAnimation.begin().thenLoop("idle_up");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "visor_controller", 0, state -> {
            ItemStack stack = state.getData(DataTickets.ITEMSTACK);
            boolean active = stack != null && stack.hasTag() && stack.getTag().getBoolean(ModConstants.TAG_ACTIVE);
            return state.setAndContinue(active ? IDLE_DOWN : IDLE_UP);
        }));
    }

    private final java.util.function.Supplier<Object> renderProvider = new java.util.function.Supplier<Object>() {
        private Object geoRenderProvider;
        @Override
        public Object get() {
            if (this.geoRenderProvider == null) {
                createRenderer(provider -> this.geoRenderProvider = provider);
            }
            return this.geoRenderProvider;
        }
    };

    @Override
    public void createRenderer(java.util.function.Consumer<Object> consumer) {
        dev.itsrealperson.vision_goggles.client.renderer.VisionGogglesClientRenderHelper.createRenderer(consumer);
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
