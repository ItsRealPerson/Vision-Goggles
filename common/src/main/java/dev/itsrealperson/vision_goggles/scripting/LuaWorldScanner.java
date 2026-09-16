package dev.itsrealperson.vision_goggles.scripting;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.Collection;
import java.util.List;

public class LuaWorldScanner {

    public static void register(Globals globals) {
        LuaTable world = new LuaTable();

        world.set("getTargetEntity", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                double range = arg.isnumber() ? arg.todouble() : 30.0;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.NIL;

                Vec3 eyePos = mc.player.getEyePosition(1.0f);
                Vec3 look = mc.player.getViewVector(1.0f);
                Vec3 target = eyePos.add(look.scale(range));
                AABB box = mc.player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);

                EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                        mc.player, eyePos, target, box,
                        e -> !e.isSpectator() && e.isPickable() && e instanceof LivingEntity,
                        range * range
                );

                if (hit != null && hit.getEntity() instanceof LivingEntity living) {
                    return wrapEntity(living, eyePos.distanceTo(living.position()));
                }
                return LuaValue.NIL;
            }
        });

        world.set("getEntitiesInRadius", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                double radius = arg.isnumber() ? arg.todouble() : 16.0;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.NIL;

                Vec3 playerPos = mc.player.position();
                AABB searchBox = new AABB(
                        playerPos.x - radius, playerPos.y - radius, playerPos.z - radius,
                        playerPos.x + radius, playerPos.y + radius, playerPos.z + radius
                );

                List<LivingEntity> entities = mc.level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != mc.player);
                LuaTable list = new LuaTable();
                int idx = 1;
                for (LivingEntity living : entities) {
                    double dist = playerPos.distanceTo(living.position());
                    if (dist <= radius) {
                        list.set(idx++, wrapEntity(living, dist));
                    }
                }
                return list;
            }
        });

        world.set("raycastBlock", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                double range = arg.isnumber() ? arg.todouble() : 10.0;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.NIL;

                Vec3 eyePos = mc.player.getEyePosition(1.0f);
                Vec3 look = mc.player.getViewVector(1.0f);
                Vec3 target = eyePos.add(look.scale(range));

                BlockHitResult hit = mc.level.clip(new ClipContext(
                        eyePos, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player
                ));

                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos pos = hit.getBlockPos();
                    BlockState state = mc.level.getBlockState(pos);
                    String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

                    LuaTable res = new LuaTable();
                    res.set("name", blockId);
                    res.set("x", pos.getX());
                    res.set("y", pos.getY());
                    res.set("z", pos.getZ());
                    res.set("light_level", mc.level.getMaxLocalRawBrightness(pos));
                    res.set("can_see_sky", LuaValue.valueOf(mc.level.canSeeSky(pos)));
                    res.set("distance", eyePos.distanceTo(Vec3.atCenterOf(pos)));
                    return res;
                }
                return LuaValue.NIL;
            }
        });

        world.set("isRaining", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.level != null && mc.level.isRaining());
            }
        });

        world.set("isThundering", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.level != null && mc.level.isThundering());
            }
        });

        world.set("getTimeOfDay", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) return LuaValue.valueOf(0);
                return LuaValue.valueOf((double)(mc.level.getDayTime() % 24000L));
            }
        });

        world.set("getLightAtPlayer", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.valueOf(0);
                BlockPos pos = mc.player.blockPosition();
                return LuaValue.valueOf(mc.level.getMaxLocalRawBrightness(pos));
            }
        });

        globals.set("world", world);

        // Register dynamic player table
        LuaTable playerTable = new LuaTable();
        playerTable.set("getX", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? mc.player.getX() : 0);
            }
        });
        playerTable.set("getY", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? mc.player.getY() : 0);
            }
        });
        playerTable.set("getZ", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? mc.player.getZ() : 0);
            }
        });
        playerTable.set("getYaw", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? mc.player.getYRot() : 0);
            }
        });
        playerTable.set("getPitch", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? mc.player.getXRot() : 0);
            }
        });
        playerTable.set("getHealth", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? mc.player.getHealth() : 20);
            }
        });
        playerTable.set("getMaxHealth", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? mc.player.getMaxHealth() : 20);
            }
        });
        playerTable.set("isInWater", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null && mc.player.isInWater());
            }
        });
        playerTable.set("isDay", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.level != null && mc.level.isDay());
            }
        });
        playerTable.set("hasGoggles", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.FALSE;
                net.minecraft.world.item.ItemStack goggles = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
                return LuaValue.valueOf(!goggles.isEmpty() && goggles.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem);
            }
        });
        playerTable.set("isGogglesActive", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(dev.itsrealperson.vision_goggles.client.VisionRenderer.isVisorActive());
            }
        });
        playerTable.set("getBattery", new ZeroArgFunction() {
            @Override public LuaValue call() {
                return LuaValue.valueOf(dev.itsrealperson.vision_goggles.client.VisionRenderer.getCurrentBatteryPct());
            }
        });
        playerTable.set("hasModule", new OneArgFunction() {
            @Override public LuaValue call(LuaValue arg) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.FALSE;
                net.minecraft.world.item.ItemStack goggles = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
                if (!goggles.isEmpty() && goggles.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem vgi) {
                    return LuaValue.valueOf(vgi.hasModule(goggles, arg.tojstring()));
                }
                return LuaValue.FALSE;
            }
        });
        playerTable.set("isProGoggles", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.FALSE;
                net.minecraft.world.item.ItemStack goggles = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
                return LuaValue.valueOf(!goggles.isEmpty() && goggles.getItem() instanceof dev.itsrealperson.vision_goggles.item.ProModularGogglesItem);
            }
        });
        playerTable.set("getMaxModules", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.valueOf(0);
                net.minecraft.world.item.ItemStack goggles = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
                if (!goggles.isEmpty() && goggles.getItem() instanceof dev.itsrealperson.vision_goggles.item.ModularGogglesItem mgi) {
                    return LuaValue.valueOf(mgi.getMaxModules());
                }
                return LuaValue.valueOf(0);
            }
        });
        playerTable.set("getGogglesType", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.valueOf("none");
                net.minecraft.world.item.ItemStack goggles = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
                if (goggles.isEmpty() || !(goggles.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem)) return LuaValue.valueOf("none");
                return LuaValue.valueOf(BuiltInRegistries.ITEM.getKey(goggles.getItem()).getPath());
            }
        });
        playerTable.set("getInstalledModules", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable list = new LuaTable();
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return list;
                net.minecraft.world.item.ItemStack goggles = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
                if (!goggles.isEmpty() && goggles.hasTag() && goggles.getTag().contains(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_MODULES)) {
                    net.minecraft.nbt.ListTag modules = goggles.getTag().getList(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_MODULES, net.minecraft.nbt.Tag.TAG_STRING);
                    for (int i = 0; i < modules.size(); i++) {
                        list.set(i + 1, LuaValue.valueOf(modules.getString(i)));
                    }
                }
                return list;
            }
        });

        playerTable.set("getArmor", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? mc.player.getArmorValue() : 0);
            }
        });

        playerTable.set("getFoodLevel", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? mc.player.getFoodData().getFoodLevel() : 20);
            }
        });

        playerTable.set("getSaturation", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? (double) mc.player.getFoodData().getSaturationLevel() : 5.0);
            }
        });

        playerTable.set("getOxygen", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.valueOf(300);
                int air = mc.player.getAirSupply();
                int max = mc.player.getMaxAirSupply();
                return LuaValue.valueOf(Math.max(0, air) * 100.0 / Math.max(1, max));
            }
        });

        playerTable.set("getEffects", new ZeroArgFunction() {
            @Override public LuaValue call() {
                LuaTable list = new LuaTable();
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return list;
                Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
                int idx = 1;
                for (MobEffectInstance eff : effects) {
                    LuaTable entry = new LuaTable();
                    entry.set("id", BuiltInRegistries.MOB_EFFECT.getKey(eff.getEffect()).toString());
                    entry.set("name", eff.getEffect().getDescriptionId());
                    entry.set("amplifier", eff.getAmplifier());
                    entry.set("duration", eff.getDuration());
                    entry.set("is_beneficial", LuaValue.valueOf(eff.getEffect().isBeneficial()));
                    list.set(idx++, entry);
                }
                return list;
            }
        });

        playerTable.set("getBiome", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.valueOf("unknown");
                try {
                    BlockPos pos = mc.player.blockPosition();
                    net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder = mc.level.getBiome(pos);
                    java.util.Optional<net.minecraft.resources.ResourceLocation> key =
                        mc.level.registryAccess()
                               .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                               .getResourceKey(biomeHolder.value())
                               .map(net.minecraft.resources.ResourceKey::location);
                    return LuaValue.valueOf(key.map(net.minecraft.resources.ResourceLocation::toString).orElse("unknown"));
                } catch (Exception e) {
                    return LuaValue.valueOf("unknown");
                }
            }
        });

        playerTable.set("getLightLevel", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.valueOf(0);
                BlockPos pos = mc.player.blockPosition();
                return LuaValue.valueOf(mc.level.getMaxLocalRawBrightness(pos));
            }
        });

        playerTable.set("getDimension", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.valueOf("overworld");
                net.minecraft.resources.ResourceLocation dim = mc.level.dimension().location();
                return LuaValue.valueOf(dim.getPath()); // "overworld", "the_nether", "the_end"
            }
        });

        playerTable.set("onGround", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null && mc.player.onGround());
            }
        });

        playerTable.set("isSprinting", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null && mc.player.isSprinting());
            }
        });

        playerTable.set("isCrouching", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null && mc.player.isCrouching());
            }
        });

        playerTable.set("isFlying", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null && mc.player.getAbilities().flying);
            }
        });

        playerTable.set("getSpeed", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.valueOf(0.0);
                net.minecraft.world.phys.Vec3 vel = mc.player.getDeltaMovement();
                double horizontal = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                return LuaValue.valueOf(Math.round(horizontal * 1000.0) / 1000.0);
            }
        });

        globals.set("player", playerTable);

        LuaTable gogglesTable = new LuaTable();
        gogglesTable.set("setMaxModules", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue type, LuaValue count) {
                String t = type.tojstring().toLowerCase();
                int c = count.toint();
                if ("pro".equals(t)) {
                    dev.itsrealperson.vision_goggles.item.ModularGogglesItem.setCustomProMax(c);
                    dev.itsrealperson.vision_goggles.util.ModConfig.common.proModularMaxModules = c;
                } else {
                    dev.itsrealperson.vision_goggles.item.ModularGogglesItem.setCustomModularMax(c);
                    dev.itsrealperson.vision_goggles.util.ModConfig.common.modularMaxModules = c;
                }
                dev.itsrealperson.vision_goggles.util.ModConfig.save();
                return LuaValue.TRUE;
            }
        });
        gogglesTable.set("getMaxModules", new OneArgFunction() {
            @Override public LuaValue call(LuaValue type) {
                String t = type.tojstring().toLowerCase();
                if ("pro".equals(t)) {
                    int custom = dev.itsrealperson.vision_goggles.item.ModularGogglesItem.getCustomProMax();
                    return LuaValue.valueOf(custom > 0 ? custom : 4);
                } else {
                    int custom = dev.itsrealperson.vision_goggles.item.ModularGogglesItem.getCustomModularMax();
                    return LuaValue.valueOf(custom > 0 ? custom : 2);
                }
            }
        });
        globals.set("goggles", gogglesTable);
    }

    private static LuaTable wrapEntity(LivingEntity living, double dist) {
        LuaTable t = new LuaTable();
        double roundedHealth = Math.round(living.getHealth() * 10.0) / 10.0;
        double roundedMaxHealth = Math.round(living.getMaxHealth() * 10.0) / 10.0;
        double roundedDist = Math.round(dist * 10.0) / 10.0;

        t.set("name", living.getName().getString());
        t.set("type", BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString());
        t.set("health", roundedHealth);
        t.set("max_health", roundedMaxHealth);
        t.set("armor", living.getArmorValue());
        t.set("x", Math.round(living.getX() * 10.0) / 10.0);
        t.set("y", Math.round(living.getY() * 10.0) / 10.0);
        t.set("z", Math.round(living.getZ() * 10.0) / 10.0);
        t.set("distance", roundedDist);
        t.set("is_hostile", LuaValue.valueOf(living instanceof net.minecraft.world.entity.monster.Enemy || living instanceof Monster));
        t.set("is_player", LuaValue.valueOf(living instanceof Player));
        return t;
    }
}
