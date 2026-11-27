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
    public static final int FLIP_DOWN_WARMUP_TICKS = 16; // 0.80s (matches GogglesFlipAnimator 0.78s flip-down)
    private static final java.util.Map<java.util.UUID, Integer> UNDERWATER_TICKS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<java.util.UUID, Boolean> LAST_ACTIVE_STATE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<java.util.UUID, Integer> WARMUP_TICKS = new java.util.concurrent.ConcurrentHashMap<>();
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
        if (stack == null || stack.isEmpty() || moduleName == null || moduleName.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            net.minecraft.nbt.ListTag modules = tag.getList(ModConstants.TAG_MODULES, net.minecraft.nbt.Tag.TAG_STRING);
            String strippedReq = moduleName.contains(":") ? moduleName.substring(moduleName.indexOf(':') + 1) : moduleName;
            for (int i = 0; i < modules.size(); i++) {
                String mod = modules.getString(i);
                if (mod.equalsIgnoreCase(moduleName)) {
                    return true;
                }
                String strippedMod = mod.contains(":") ? mod.substring(mod.indexOf(':') + 1) : mod;
                if (strippedMod.equalsIgnoreCase(strippedReq)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canInstallAddonModule(ItemStack stack, String moduleId) {
        if (stack == null || stack.isEmpty() || moduleId == null || moduleId.isBlank()) {
            return false;
        }
        return !hasModule(stack, moduleId);
    }

    public void installAddonModule(ItemStack stack, String moduleId) {
        if (!canInstallAddonModule(stack, moduleId)) {
            return;
        }
        CompoundTag nbt = stack.getOrCreateTag();
        net.minecraft.nbt.ListTag modules = nbt.getList(ModConstants.TAG_MODULES, net.minecraft.nbt.Tag.TAG_STRING);
        modules.add(net.minecraft.nbt.StringTag.valueOf(moduleId));
        nbt.put(ModConstants.TAG_MODULES, modules);
    }

    public String getLastInstalledAddonModule(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            net.minecraft.nbt.ListTag modules = tag.getList(ModConstants.TAG_MODULES, net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = modules.size() - 1; i >= 0; i--) {
                String mod = modules.getString(i);
                if (dev.itsrealperson.vision_goggles.api.VisionGogglesAPI.isRegisteredAddonModule(mod)) {
                    return mod;
                }
            }
        }
        return null;
    }

    public boolean removeAddonModule(ItemStack stack, String moduleId) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            net.minecraft.nbt.ListTag modules = tag.getList(ModConstants.TAG_MODULES, net.minecraft.nbt.Tag.TAG_STRING);
            String strippedReq = moduleId.contains(":") ? moduleId.substring(moduleId.indexOf(':') + 1) : moduleId;
            for (int i = modules.size() - 1; i >= 0; i--) {
                String mod = modules.getString(i);
                String strippedMod = mod.contains(":") ? mod.substring(mod.indexOf(':') + 1) : mod;
                if (mod.equalsIgnoreCase(moduleId) || strippedMod.equalsIgnoreCase(strippedReq)) {
                    modules.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level, List<net.minecraft.network.chat.Component> tooltipComponents, net.minecraft.world.item.TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            net.minecraft.nbt.ListTag modules = tag.getList(ModConstants.TAG_MODULES, net.minecraft.nbt.Tag.TAG_STRING);
            boolean headerAdded = false;
            for (int i = 0; i < modules.size(); i++) {
                String mod = modules.getString(i);
                Item addonItem = dev.itsrealperson.vision_goggles.api.VisionGogglesAPI.getRegisteredAddonModuleItem(mod);
                if (addonItem != null) {
                    if (!headerAdded) {
                        tooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.vision_goggles.installed_modules").withStyle(net.minecraft.ChatFormatting.GRAY));
                        headerAdded = true;
                    }
                    tooltipComponents.add(net.minecraft.network.chat.Component.literal("- ").append(addonItem.getDescription()).withStyle(net.minecraft.ChatFormatting.AQUA));
                }
            }
        }
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

    public static void triggerActivationWarmup(Player player) {
        if (player == null) return;
        java.util.UUID uuid = player.getUUID();
        WARMUP_TICKS.put(uuid, FLIP_DOWN_WARMUP_TICKS);
        LAST_ACTIVE_STATE.put(uuid, true);
    }

    public static void clearActivationState(Player player) {
        if (player == null) return;
        java.util.UUID uuid = player.getUUID();
        WARMUP_TICKS.remove(uuid);
        LAST_ACTIVE_STATE.remove(uuid);
        UNDERWATER_TICKS.remove(uuid);
    }

    public static boolean isEquippingGoggles(Player player) {
        return player != null && player.isUsingItem() && player.getUseItem().getItem() instanceof VisionGogglesItem;
    }

    public static boolean isWarmingUpOrEquipping(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;
        if (isEquippingGoggles(player)) return true;
        CompoundTag nbt = stack.getTag();
        boolean active = nbt != null && nbt.getBoolean(ModConstants.TAG_ACTIVE);
        if (!active) return false;
        java.util.UUID uuid = player.getUUID();
        if (!LAST_ACTIVE_STATE.getOrDefault(uuid, false)) {
            return true;
        }
        return WARMUP_TICKS.getOrDefault(uuid, 0) > 0;
    }

    public void serverTick(ItemStack stack, ServerPlayer player) {
        CompoundTag nbt = stack.getOrCreateTag();
        float maxBattery = (float) getBatteryCapacity(stack);
        
        if (!nbt.contains(ModConstants.TAG_BATTERY)) nbt.putFloat(ModConstants.TAG_BATTERY, maxBattery);
        float currentBattery = nbt.getFloat(ModConstants.TAG_BATTERY);
        
        VisionMode mode = getActiveVisionMode(stack);
        java.util.UUID uuid = player.getUUID();
        boolean isActive = nbt.getBoolean(ModConstants.TAG_ACTIVE);
        boolean wasActive = LAST_ACTIVE_STATE.getOrDefault(uuid, false);

        if (isActive && !wasActive) {
            WARMUP_TICKS.put(uuid, Math.max(WARMUP_TICKS.getOrDefault(uuid, 0), FLIP_DOWN_WARMUP_TICKS));
        } else if (!isActive) {
            WARMUP_TICKS.remove(uuid);
        }
        LAST_ACTIVE_STATE.put(uuid, isActive);

        if (isEquippingGoggles(player)) {
            if (isActive) {
                WARMUP_TICKS.put(uuid, FLIP_DOWN_WARMUP_TICKS);
            }
            return;
        }

        int warmup = WARMUP_TICKS.getOrDefault(uuid, 0);
        if (isActive && warmup > 0) {
            if (warmup <= 1) {
                WARMUP_TICKS.remove(uuid);
            } else {
                WARMUP_TICKS.put(uuid, warmup - 1);
            }
            return;
        }

        if (isActive) {
            if (currentBattery > 0) {
                if ((mode == VisionMode.NIGHT_VISION || mode == VisionMode.THERMAL)
                        && player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
                    int underwaterTicks = UNDERWATER_TICKS.getOrDefault(uuid, 0) + 1;
                    if (underwaterTicks >= 40) {
                        UNDERWATER_TICKS.remove(uuid);
                        nbt.putBoolean(ModConstants.TAG_ACTIVE, false);
                        LAST_ACTIVE_STATE.put(uuid, false);
                        WARMUP_TICKS.remove(uuid);
                        cleanUpEffect(player);
                        player.level().playSound(null, player.blockPosition(),
                                dev.itsrealperson.vision_goggles.registry.ModSounds.FLASHLIGHT_SWITCH.get(),
                                net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.65f);
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("message.vision_goggles.water_short_circuit")
                                        .withStyle(net.minecraft.ChatFormatting.AQUA),
                                true
                        );
                        return;
                    }
                    UNDERWATER_TICKS.put(uuid, underwaterTicks);
                } else {
                    UNDERWATER_TICKS.remove(uuid);
                }

                float baseDrain = (mode != null) ? ((mode == VisionMode.THERMAL) ? 2.0f : 1.0f) : 0.0f;
                if (mode == VisionMode.HYDRO) {
                    if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)
                            && player.getAirSupply() < player.getMaxAirSupply()
                            && player.tickCount % 2 == 0) {
                        player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 1));
                    }
                    if (player.isInWater()) {
                        baseDrain += 0.18f;
                    }
                }
                if (this instanceof ModularGogglesItem modular) {
                    for (String modId : modular.getModules(stack)) {
                        dev.itsrealperson.vision_goggles.scripting.LuaCustomModule luaMod =
                            dev.itsrealperson.vision_goggles.scripting.LuaModuleManager.findModuleByIdOrPath(modId);
                        if (luaMod != null && luaMod.getBatteryDrain() > 0 && luaMod.isHardwareInstalled(stack, modular)) {
                            String req = luaMod.getRequiredModule();
                            VisionMode reqMode = (req != null && !req.isEmpty())
                                    ? ModularGogglesItem.resolveHardwareVisionMode(req)
                                    : null;
                            if (reqMode == null || mode == null || reqMode == mode) {
                                baseDrain += luaMod.getBatteryDrain();
                            }
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
                    LAST_ACTIVE_STATE.put(uuid, false);
                    WARMUP_TICKS.remove(uuid);
                    cleanUpEffect(player);
                    UNDERWATER_TICKS.remove(uuid);
                }
            } else {
                nbt.putBoolean(ModConstants.TAG_ACTIVE, false);
                LAST_ACTIVE_STATE.put(uuid, false);
                WARMUP_TICKS.remove(uuid);
                cleanUpEffect(player);
                UNDERWATER_TICKS.remove(uuid);
            }
        } else {
            UNDERWATER_TICKS.remove(uuid);
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
            InteractionHand usedHand = player.getUsedItemHand();
            dev.itsrealperson.vision_goggles.client.GogglesEquipAnimator.onLocalEquipCompleted(usedHand, stack);
            NetworkManager.INSTANCE.sendToServer(new EquipPacket(usedHand));
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 14; // 0.70 seconds
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    private static final RawAnimation IDLE_DOWN = RawAnimation.begin().thenLoop("idle_down");
    private static final RawAnimation IDLE_UP = RawAnimation.begin().thenLoop("idle_up");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "visor_controller", 0, state -> {
            return state.setAndContinue(IDLE_DOWN);
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
