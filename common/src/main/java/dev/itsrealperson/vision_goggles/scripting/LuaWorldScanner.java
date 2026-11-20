package dev.itsrealperson.vision_goggles.scripting;

import net.minecraft.client.Minecraft;
import dev.itsrealperson.vision_goggles.mixin.client.GameRendererAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class LuaWorldScanner {

    public static void register(Globals globals) {
        LuaTable world = new LuaTable();

        world.set("getTargetEntity", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                double range = args.isnumber(1) ? args.todouble(1) : 30.0;
                range = Math.min(64.0, Math.max(1.0, range));
                boolean throughWalls = args.optboolean(2, false);
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.NIL;

                Vec3 eyePos = mc.player.getEyePosition(1.0f);
                Vec3 look = mc.player.getViewVector(1.0f);
                Vec3 target = eyePos.add(look.scale(range));
                double maxDist = range;

                if (!throughWalls) {
                    BlockHitResult blockHit = mc.level.clip(new ClipContext(
                            eyePos, target, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player
                    ));
                    if (blockHit.getType() == HitResult.Type.BLOCK) {
                        maxDist = eyePos.distanceTo(blockHit.getLocation());
                        target = blockHit.getLocation();
                    }
                }

                AABB box = mc.player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.0);

                EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                        mc.player, eyePos, target, box,
                        e -> !e.isSpectator() && e.isPickable() && e instanceof LivingEntity,
                        maxDist * maxDist
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
                radius = Math.min(64.0, Math.max(1.0, radius));
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
                range = Math.min(64.0, Math.max(1.0, range));
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
                    res.set("id", blockId);
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

        world.set("worldToScreen", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue argX, LuaValue argY, LuaValue argZ) {
                if (!argX.isnumber() || !argY.isnumber() || !argZ.isnumber()) return LuaValue.NIL;
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.NIL;

                double x = argX.todouble();
                double y = argY.todouble();
                double z = argZ.todouble();

                net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();
                Vec3 camPos = camera.getPosition();
                double relX = x - camPos.x;
                double relY = y - camPos.y;
                double relZ = z - camPos.z;

                org.joml.Vector3f look = camera.getLookVector();
                org.joml.Vector3f upVec = camera.getUpVector();
                org.joml.Vector3f leftVec = camera.getLeftVector();

                float forward = (float) (relX * look.x + relY * look.y + relZ * look.z);
                double totalDist = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
                double roundedDist = Math.round(totalDist * 10.0) / 10.0;
                if (forward <= 0.05f) {
                    LuaTable res = new LuaTable();
                    res.set("x", LuaValue.valueOf(-1000));
                    res.set("y", LuaValue.valueOf(-1000));
                    res.set("visible", LuaValue.valueOf(false));
                    res.set("behind", LuaValue.valueOf(true));
                    res.set("distance", LuaValue.valueOf(roundedDist));
                    return res;
                }

                float up = (float) (relX * upVec.x + relY * upVec.y + relZ * upVec.z);
                float right = -(float) (relX * leftVec.x + relY * leftVec.y + relZ * leftVec.z);

                float fov = (float) Math.toRadians(((GameRendererAccessor) mc.gameRenderer).vision_goggles$getFov(camera, mc.getFrameTime(), true));
                float halfHeight = (float) Math.tan(fov * 0.5f);
                float aspectRatio = (float) mc.getWindow().getScreenWidth() / (float) mc.getWindow().getScreenHeight();
                float halfWidth = halfHeight * aspectRatio;

                float ndcX = right / (forward * halfWidth);
                float ndcY = up / (forward * halfHeight);

                float screenWidth = (float) mc.getWindow().getGuiScaledWidth();
                float screenHeight = (float) mc.getWindow().getGuiScaledHeight();

                float screenX = (ndcX + 1.0f) * 0.5f * screenWidth;
                float screenY = (1.0f - ndcY) * 0.5f * screenHeight;

                boolean onScreen = ndcX >= -1.05f && ndcX <= 1.05f && ndcY >= -1.05f && ndcY <= 1.05f;

                LuaTable res = new LuaTable();
                res.set("x", LuaValue.valueOf(screenX));
                res.set("y", LuaValue.valueOf(screenY));
                res.set("visible", LuaValue.valueOf(onScreen));
                res.set("behind", LuaValue.valueOf(false));
                res.set("distance", LuaValue.valueOf(roundedDist));
                return res;
            }
        });

        world.set("getBlock", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue argX, LuaValue argY, LuaValue argZ) {
                if (!argX.isnumber() || !argY.isnumber() || !argZ.isnumber()) return LuaValue.valueOf("minecraft:air");
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) return LuaValue.valueOf("minecraft:air");
                BlockPos pos = BlockPos.containing(argX.todouble(), argY.todouble(), argZ.todouble());
                if (!mc.level.isLoaded(pos)) return LuaValue.valueOf("minecraft:air");
                BlockState state = mc.level.getBlockState(pos);
                return LuaValue.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            }
        });

        world.set("getLightAt", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue argX, LuaValue argY, LuaValue argZ) {
                if (!argX.isnumber() || !argY.isnumber() || !argZ.isnumber()) return LuaValue.valueOf(0);
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) return LuaValue.valueOf(0);
                BlockPos pos = BlockPos.containing(argX.todouble(), argY.todouble(), argZ.todouble());
                if (!mc.level.isLoaded(pos)) return LuaValue.valueOf(0);
                return LuaValue.valueOf(mc.level.getMaxLocalRawBrightness(pos));
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

        world.set("scanBlocks", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null || !args.istable(2)) {
                    return new LuaTable();
                }

                int radius = Math.min(16, Math.max(1, args.isnumber(1) ? args.toint(1) : 12));
                int maxResults = Math.min(32, Math.max(1, args.isnumber(3) ? args.toint(3) : 12));
                LuaTable tbl = args.checktable(2);

                Set<Block> targetBlocks = Collections.newSetFromMap(new IdentityHashMap<>());
                LuaValue k = LuaValue.NIL;
                while (true) {
                    Varargs next = tbl.next(k);
                    k = next.arg1();
                    if (k.isnil()) break;
                    LuaValue v = next.arg(2);
                    String idStr = null;
                    if (v.isstring()) {
                        idStr = v.tojstring();
                    } else if (k.isstring() && v.toboolean()) {
                        idStr = k.tojstring();
                    }
                    if (idStr != null && !idStr.isEmpty()) {
                        ResourceLocation rl = ResourceLocation.tryParse(idStr);
                        if (rl != null) {
                            BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(targetBlocks::add);
                        }
                    }
                }
                if (targetBlocks.isEmpty()) {
                    return new LuaTable();
                }

                int px = mc.player.getBlockX();
                int py = mc.player.getBlockY();
                int pz = mc.player.getBlockZ();
                int minY = mc.level.getMinBuildHeight();
                int maxY = mc.level.getMaxBuildHeight() - 1;
                int yStart = Math.max(minY, py - radius);
                int yEnd = Math.min(maxY, py + radius);
                int rSq = radius * radius;

                // Bounded max-heap keeps only the exact closest maxResults blocks in O(maxResults) memory
                PriorityQueue<ScannedBlock> closestHeap = new PriorityQueue<>(maxResults, (a, b) -> Integer.compare(b.distSq, a.distSq));
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

                for (int dx = -radius; dx <= radius; dx++) {
                    int bx = px + dx;
                    int dxSq = dx * dx;
                    for (int dz = -radius; dz <= radius; dz++) {
                        int xzSq = dxSq + dz * dz;
                        if (xzSq > rSq) continue;
                        int bz = pz + dz;
                        if (!mc.level.hasChunk(bx >> 4, bz >> 4)) continue;

                        for (int by = yStart; by <= yEnd; by++) {
                            int dy = by - py;
                            int distSq = xzSq + dy * dy;
                            if (distSq > rSq) continue;

                            pos.set(bx, by, bz);
                            BlockState state = mc.level.getBlockState(pos);
                            Block block = state.getBlock();
                            if (targetBlocks.contains(block)) {
                                if (closestHeap.size() < maxResults) {
                                    closestHeap.offer(new ScannedBlock(block, bx, by, bz, distSq));
                                } else if (distSq < closestHeap.peek().distSq) {
                                    closestHeap.poll();
                                    closestHeap.offer(new ScannedBlock(block, bx, by, bz, distSq));
                                }
                            }
                        }
                    }
                }

                if (closestHeap.isEmpty()) {
                    return new LuaTable();
                }

                List<ScannedBlock> matches = new ArrayList<>(closestHeap);
                matches.sort(Comparator.comparingInt(m -> m.distSq));
                LuaTable result = new LuaTable();
                for (int i = 0; i < matches.size(); i++) {
                    ScannedBlock m = matches.get(i);
                    LuaTable entry = new LuaTable();
                    entry.set("id", BuiltInRegistries.BLOCK.getKey(m.block).toString());
                    entry.set("x", m.x);
                    entry.set("y", m.y);
                    entry.set("z", m.z);
                    entry.set("distance", Math.round(Math.sqrt(m.distSq) * 10.0) / 10.0);
                    result.set(i + 1, entry);
                }
                return result;
            }
        });

        world.set("getProjectilesInRadius", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                double radius = arg.isnumber() ? arg.todouble() : 24.0;
                if (Double.isNaN(radius) || Double.isInfinite(radius)) radius = 24.0;
                radius = Math.min(48.0, Math.max(1.0, radius));
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.level == null) return LuaValue.NIL;

                Vec3 playerPos = mc.player.position();
                Vec3 playerEye = mc.player.getEyePosition(1.0f);
                AABB searchBox = new AABB(
                        playerPos.x - radius, playerPos.y - radius, playerPos.z - radius,
                        playerPos.x + radius, playerPos.y + radius, playerPos.z + radius
                );

                List<Projectile> projectiles = mc.level.getEntitiesOfClass(Projectile.class, searchBox);
                LuaTable list = new LuaTable();
                int idx = 1;
                float pt = mc.getFrameTime();

                for (Projectile proj : projectiles) {
                    if (proj.getOwner() == mc.player || proj.onGround()) continue;
                    Vec3 vel = proj.getDeltaMovement();
                    if (vel.lengthSqr() < 0.0025) continue;

                    Vec3 interp = proj.getPosition(pt);
                    double dist = playerEye.distanceTo(interp);
                    if (dist > radius) continue;

                    Vec3 toPlayer = playerEye.subtract(interp);
                    boolean approaching = false;
                    if (toPlayer.lengthSqr() > 1.0E-4) {
                        approaching = vel.normalize().dot(toPlayer.normalize()) > 0.55;
                    }

                    LuaTable entry = new LuaTable();
                    entry.set("type", BuiltInRegistries.ENTITY_TYPE.getKey(proj.getType()).toString());
                    entry.set("x", Math.round(interp.x * 1000.0) / 1000.0);
                    entry.set("y", Math.round(interp.y * 1000.0) / 1000.0);
                    entry.set("z", Math.round(interp.z * 1000.0) / 1000.0);
                    entry.set("vx", Math.round(vel.x * 1000.0) / 1000.0);
                    entry.set("vy", Math.round(vel.y * 1000.0) / 1000.0);
                    entry.set("vz", Math.round(vel.z * 1000.0) / 1000.0);
                    entry.set("distance", Math.round(dist * 10.0) / 10.0);
                    entry.set("approaching", LuaValue.valueOf(approaching));
                    list.set(idx++, entry);
                }
                return list;
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
                if (mc.player == null) return LuaValue.valueOf(100.0);
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
                return LuaValue.valueOf(mc.player != null && (mc.player.getAbilities().flying || mc.player.isFallFlying()));
            }
        });

        playerTable.set("getSpeed", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.valueOf(0.0);
                net.minecraft.world.phys.Vec3 vel = mc.player.getDeltaMovement();
                double horizontalPerTick = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                return LuaValue.valueOf(Math.round(horizontalPerTick * 20.0 * 100.0) / 100.0);
            }
        });

        playerTable.set("getSonarMode", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.valueOf(0);
                net.minecraft.world.item.ItemStack goggles = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
                if (!goggles.isEmpty() && goggles.hasTag()
                        && goggles.getTag().contains(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_SONAR_MODE)) {
                    return LuaValue.valueOf(goggles.getTag().getInt(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_SONAR_MODE));
                }
                return LuaValue.valueOf(0);
            }
        });

        playerTable.set("getAbsorption", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.player != null ? (double) mc.player.getAbsorptionAmount() : 0.0);
            }
        });

        playerTable.set("getArmorDurabilityPct", new ZeroArgFunction() {
            @Override public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return LuaValue.valueOf(-1.0);
                double minPct = -1.0;
                for (net.minecraft.world.item.ItemStack piece : mc.player.getArmorSlots()) {
                    if (!piece.isEmpty() && piece.isDamageableItem() && piece.getMaxDamage() > 0) {
                        double pct = (double) (piece.getMaxDamage() - piece.getDamageValue()) / (double) piece.getMaxDamage();
                        pct = Math.max(0.0, Math.min(1.0, pct));
                        if (minPct < 0.0 || pct < minPct) {
                            minPct = pct;
                        }
                    }
                }
                return LuaValue.valueOf(minPct);
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
        Minecraft mc = Minecraft.getInstance();
        float pt = mc.getFrameTime();
        Vec3 interp = living.getPosition(pt);
        double roundedHealth = Math.round(living.getHealth() * 10.0) / 10.0;
        double roundedMaxHealth = Math.round(living.getMaxHealth() * 10.0) / 10.0;
        double roundedDist = Math.round(dist * 10.0) / 10.0;

        t.set("name", living.getName().getString());
        t.set("type", BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString());
        t.set("health", roundedHealth);
        t.set("max_health", roundedMaxHealth);
        t.set("armor", living.getArmorValue());
        t.set("x", Math.round(interp.x * 1000.0) / 1000.0);
        t.set("y", Math.round(interp.y * 1000.0) / 1000.0);
        t.set("z", Math.round(interp.z * 1000.0) / 1000.0);
        t.set("eye_y", Math.round((interp.y + living.getEyeHeight()) * 1000.0) / 1000.0);
        t.set("height", Math.round(living.getBbHeight() * 100.0) / 100.0);
        t.set("pitch", Math.round(living.getXRot() * 10.0) / 10.0);
        t.set("yaw", Math.round(living.getYRot() * 10.0) / 10.0);
        t.set("distance", roundedDist);
        boolean emitsHeat = dev.itsrealperson.vision_goggles.client.ThermalEntityPainter.emitsThermalHeat(living);
        boolean thermalVisible = false;
        if (emitsHeat && mc.level != null && mc.gameRenderer != null && mc.gameRenderer.getMainCamera() != null) {
            thermalVisible = dev.itsrealperson.vision_goggles.client.ThermalEntityPainter.computeEntityThermalHeat(
                    living, mc.gameRenderer.getMainCamera().getPosition(), pt, mc.level) > 0.11f;
        }

        t.set("is_hostile", LuaValue.valueOf(living instanceof net.minecraft.world.entity.monster.Enemy || living instanceof Monster));
        t.set("is_player", LuaValue.valueOf(living instanceof Player));
        t.set("emits_heat", LuaValue.valueOf(emitsHeat));
        t.set("thermal_visible", LuaValue.valueOf(thermalVisible));
        t.set("is_undead", LuaValue.valueOf(living.getMobType() == MobType.UNDEAD));
        return t;
    }

    private static class ScannedBlock {
        final Block block;
        final int x;
        final int y;
        final int z;
        final int distSq;

        ScannedBlock(Block block, int x, int y, int z, int distSq) {
            this.block = block;
            this.x = x;
            this.y = y;
            this.z = z;
            this.distSq = distSq;
        }
    }
}
