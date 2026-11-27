package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.itsrealperson.vision_goggles.client.hud.IHudModule;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.mixin.client.GameRendererAccessor;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.scripting.LuaCustomModule;
import dev.itsrealperson.vision_goggles.scripting.LuaModuleManager;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VisionHUDOverlay {
    private static final List<IHudModule> MODULES = new ArrayList<>();
    private static final Set<UUID> MARKED_BIOMETRIC_TARGETS = new HashSet<>();
    private static int lastBiometricTargetId = -1;
    private static long biometricLockStartMs = 0L;

    public static void registerModule(IHudModule module) {
        MODULES.add(module);
    }

    public static void render(GuiGraphics gui, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack goggles = PlatformMethods.getEquippedHelmet(mc.player);
        if (goggles.isEmpty() || !(goggles.getItem() instanceof VisionGogglesItem)) return;

        // Solo renderizar si las gafas están encendidas (esta lógica se sincronizará con VisionRenderer)
        if (!VisionRenderer.isVisorActive()) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        renderWaterShortCircuitOverlay(gui, mc, width, height);

        for (IHudModule module : MODULES) {
            if (module.shouldRender(mc.player, goggles)) {
                module.render(gui, goggles, partialTicks, width, height);
            }
        }

        renderBiometricTargetLockOn(gui, mc, goggles, partialTicks, width, height);

        dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge.onRenderHud(gui, partialTicks);
        dev.itsrealperson.vision_goggles.api.VisionGogglesClientAPI.renderHudOverlayLayers(gui, partialTicks);
    }

    /**
     * Projects a 3D world position onto 2D GUI coordinates.
     * Returns float[]{screenX, screenY, distance, onScreen(1/0)} or null if behind the camera.
     */
    public static float[] projectWorldToGui(Minecraft mc, double x, double y, double z, float partialTicks) {
        if (mc == null || mc.gameRenderer == null || mc.level == null) return null;
        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return null;

        Vec3 camPos = camera.getPosition();
        double relX = x - camPos.x;
        double relY = y - camPos.y;
        double relZ = z - camPos.z;

        Vector3f look = camera.getLookVector();
        Vector3f upVec = camera.getUpVector();
        Vector3f leftVec = camera.getLeftVector();

        float forward = (float) (relX * look.x + relY * look.y + relZ * look.z);
        double totalDist = Math.sqrt(relX * relX + relY * relY + relZ * relZ);
        if (forward <= 0.08f) {
            return null;
        }

        float up = (float) (relX * upVec.x + relY * upVec.y + relZ * upVec.z);
        float right = -(float) (relX * leftVec.x + relY * leftVec.y + relZ * leftVec.z);

        float fov = (float) Math.toRadians(((GameRendererAccessor) mc.gameRenderer).vision_goggles$getFov(camera, partialTicks, true));
        float halfHeight = (float) Math.tan(fov * 0.5f);
        int winW = Math.max(1, mc.getWindow().getScreenWidth());
        int winH = Math.max(1, mc.getWindow().getScreenHeight());
        float aspectRatio = (float) winW / (float) winH;
        float halfWidth = halfHeight * aspectRatio;

        float ndcX = right / (forward * halfWidth);
        float ndcY = up / (forward * halfHeight);

        float screenWidth = (float) mc.getWindow().getGuiScaledWidth();
        float screenHeight = (float) mc.getWindow().getGuiScaledHeight();

        float screenX = (ndcX + 1.0f) * 0.5f * screenWidth;
        float screenY = (1.0f - ndcY) * 0.5f * screenHeight;
        boolean onScreen = ndcX >= -1.02f && ndcX <= 1.02f && ndcY >= -1.02f && ndcY <= 1.02f;

        return new float[]{screenX, screenY, (float) totalDist, onScreen ? 1.0f : 0.0f};
    }

    public static void drawCornerBrackets(GuiGraphics gui, int cx, int cy, int halfSize, int armLen, int color) {
        int x1 = cx - halfSize;
        int x2 = cx + halfSize;
        int y1 = cy - halfSize;
        int y2 = cy + halfSize;

        // Top-left
        gui.fill(x1, y1, x1 + armLen, y1 + 1, color);
        gui.fill(x1, y1, x1 + 1, y1 + armLen, color);
        // Top-right
        gui.fill(x2 - armLen, y1, x2 + 1, y1 + 1, color);
        gui.fill(x2, y1, x2 + 1, y1 + armLen, color);
        // Bottom-left
        gui.fill(x1, y2, x1 + armLen, y2 + 1, color);
        gui.fill(x1, y2 - armLen + 1, x1 + 1, y2 + 1, color);
        // Bottom-right
        gui.fill(x2 - armLen, y2, x2 + 1, y2 + 1, color);
        gui.fill(x2, y2 - armLen + 1, x2 + 1, y2 + 1, color);
    }

    private static final EquipmentSlot[] INSPECT_SLOTS = new EquipmentSlot[]{
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private static final class BiometricCandidate {
        final LivingEntity entity;
        final float screenX;
        final float screenY;
        final float worldDist;
        final float centerDistSq;
        final Vec3 aimPos;
        final float sortScore;

        BiometricCandidate(LivingEntity entity, float screenX, float screenY, float worldDist, float centerDistSq, Vec3 aimPos) {
            this.entity = entity;
            this.screenX = screenX;
            this.screenY = screenY;
            this.worldDist = worldDist;
            this.centerDistSq = centerDistSq;
            this.aimPos = aimPos;
            this.sortScore = centerDistSq * 0.002f + worldDist;
        }
    }

    /**
     * Handles the configurable {@code Mark Target} keybind ({@link ModKeyMappings#markTargetKey}) in BIOMETRIC mode:
     * toggles manual lock-on on the entity under the crosshair (up to effective range), or clears all marks on
     * Shift+press / empty-space press.
     */
    public static void handleMarkTargetKey(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            return;
        }
        if (!VisionRenderer.isVisorActive() || !VisionRenderer.isVisionModeActive()
                || VisionRenderer.getCurrentVisionMode() != VisionMode.BIOMETRIC) {
            return;
        }
        Player player = mc.player;
        if (player.isShiftKeyDown()) {
            if (!MARKED_BIOMETRIC_TARGETS.isEmpty()) {
                MARKED_BIOMETRIC_TARGETS.clear();
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 0.85F, 0.25F));
            }
            return;
        }

        double maxRange = getEffectiveDetectionRange(player, 32.0);
        LivingEntity hitEntity = raycastBiometricEntity(mc, player, maxRange, 1.0f);
        if (hitEntity != null) {
            UUID uuid = hitEntity.getUUID();
            if (MARKED_BIOMETRIC_TARGETS.contains(uuid)) {
                MARKED_BIOMETRIC_TARGETS.remove(uuid);
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 1.1F, 0.25F));
            } else {
                MARKED_BIOMETRIC_TARGETS.add(uuid);
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 1.75F, 0.30F));
            }
        } else if (!MARKED_BIOMETRIC_TARGETS.isEmpty()) {
            MARKED_BIOMETRIC_TARGETS.clear();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 0.85F, 0.25F));
        }
    }

    private static LivingEntity raycastBiometricEntity(Minecraft mc, Player player, double maxRange, float partialTicks) {
        Vec3 eyePos = player.getEyePosition(partialTicks);
        Vec3 look = player.getViewVector(partialTicks);
        Vec3 targetEnd = eyePos.add(look.scale(maxRange));
        double maxDist = maxRange;

        BlockHitResult blockHit = mc.level.clip(new ClipContext(
                eyePos, targetEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            maxDist = eyePos.distanceTo(blockHit.getLocation());
            targetEnd = blockHit.getLocation();
        }

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.2);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eyePos, targetEnd, searchBox,
                e -> e != player && e.isAlive() && !e.isSpectator() && !e.isInvisible()
                        && e.isPickable() && e instanceof LivingEntity && !(e instanceof ArmorStand),
                maxDist * maxDist
        );
        return (hit != null && hit.getEntity() instanceof LivingEntity living) ? living : null;
    }

    private static void renderBiometricTargetLockOn(GuiGraphics gui, Minecraft mc, ItemStack goggles, float partialTicks, int width, int height) {
        if (!VisionRenderer.isVisionModeActive() || VisionRenderer.getCurrentVisionMode() != VisionMode.BIOMETRIC) {
            lastBiometricTargetId = -1;
            return;
        }
        // Yield to Lua threat_scanner chip when installed so target cards don't overlap
        if (goggles.getItem() instanceof ModularGogglesItem modular) {
            LuaCustomModule luaAr = LuaModuleManager.findModuleByIdOrPath("threat_scanner");
            if (luaAr != null && luaAr.isHardwareInstalled(goggles, modular)) {
                return;
            }
        }

        Player player = mc.player;
        if (player == null || mc.level == null) return;

        final double maxRange = getEffectiveDetectionRange(player, 32.0);
        final int maxTargets = 10;
        final float midX = width * 0.5f;
        final float midY = height * 0.5f;
        final float lensRadius = Math.min(width, height) * 0.47f;
        final float lensRadiusSq = lensRadius * lensRadius;

        LivingEntity directHit = raycastBiometricEntity(mc, player, maxRange, partialTicks);
        int directHitId = directHit != null ? directHit.getId() : -1;

        List<LivingEntity> nearby = mc.level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(maxRange),
                e -> e != player
                        && e.isAlive()
                        && !e.isSpectator()
                        && !e.isInvisible()
                        && !(e instanceof ArmorStand)
        );

        List<BiometricCandidate> candidates = new ArrayList<>();
        for (LivingEntity entity : nearby) {
            if (!player.hasLineOfSight(entity)) {
                continue;
            }
            Vec3 basePos = entity.getPosition(partialTicks);
            Vec3 aimPos = basePos.add(0.0, entity.getBbHeight() * 0.55, 0.0);
            float[] proj = projectWorldToGui(mc, aimPos.x, aimPos.y, aimPos.z, partialTicks);
            if (proj == null || proj[3] < 0.5f) {
                continue;
            }
            float sx = proj[0];
            float sy = proj[1];
            float dist = proj[2];
            if (dist > maxRange) {
                continue;
            }
            float dx = sx - midX;
            float dy = sy - midY;
            float centerDistSq = dx * dx + dy * dy;
            if (centerDistSq > lensRadiusSq) {
                continue;
            }
            candidates.add(new BiometricCandidate(entity, sx, sy, dist, centerDistSq, aimPos));
        }

        if (candidates.isEmpty()) {
            lastBiometricTargetId = -1;
            return;
        }

        candidates.sort(Comparator.comparingDouble(c -> c.sortScore));
        int count = Math.min(maxTargets, candidates.size());

        // Trigger soft audio blip when focusing a new direct crosshair target
        BiometricCandidate primary = candidates.get(0);
        boolean primaryDirect = (primary.entity.getId() == directHitId) || (primary.centerDistSq <= (22.0f * 22.0f));
        long now = System.currentTimeMillis();
        if (primaryDirect) {
            if (primary.entity.getId() != lastBiometricTargetId) {
                lastBiometricTargetId = primary.entity.getId();
                biometricLockStartMs = now;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 1.55F, 0.22F));
            }
        } else {
            lastBiometricTargetId = -1;
        }

        for (int i = count - 1; i >= 0; i--) {
            BiometricCandidate cand = candidates.get(i);
            boolean isLookedAt = (cand.entity.getId() == directHitId)
                    || (i == 0 && cand.centerDistSq <= (22.0f * 22.0f));
            boolean isMarked = MARKED_BIOMETRIC_TARGETS.contains(cand.entity.getUUID());
            renderBiometricEntityAr(gui, mc, cand, isLookedAt, isMarked, partialTicks, width, height);
        }
    }

    /**
     * Returns the effective entity detection radius: full {@code surfaceRange} above water,
     * reduced underwater ({@code 18m} near surface {@code Y >= 56}, scaling smoothly down to {@code 10m} at {@code Y <= 0}).
     */
    public static double getEffectiveDetectionRange(Player player, double surfaceRange) {
        if (player == null || !player.isEyeInFluid(FluidTags.WATER)) {
            return surfaceRange;
        }
        double y = player.getY();
        if (y >= 56.0) {
            return Math.min(surfaceRange, 18.0);
        }
        if (y <= 0.0) {
            return Math.min(surfaceRange, 10.0);
        }
        double t = y / 56.0;
        return Math.min(surfaceRange, 10.0 + t * 8.0);
    }

    private static void renderBiometricEntityAr(
            GuiGraphics gui,
            Minecraft mc,
            BiometricCandidate cand,
            boolean isLookedAt,
            boolean isMarked,
            float partialTicks,
            int screenW,
            int screenH
    ) {
        LivingEntity target = cand.entity;
        boolean expanded = isLookedAt || isMarked;
        boolean isHostile = target instanceof Enemy || target instanceof Monster
                || (target instanceof NeutralMob neutral && neutral.isAngry());
        boolean isFriendly = (target instanceof Player)
                || (target instanceof Npc)
                || (target instanceof TamableAnimal tamable && tamable.isTame())
                || (target instanceof AbstractGolem && !isHostile);

        int iffColor;
        if (isHostile) {
            iffColor = 0xFFFF3B3B; // Tactical Red
        } else if (isFriendly) {
            iffColor = 0xFF22EEAA; // Cyan / Emerald Green
        } else {
            iffColor = 0xFFFFAA22; // Tactical Amber
        }

        // High-contrast frame color when manually marked (Gold) or directly looked at (Bright Cyan)
        int frameColor = isMarked ? 0xFFFFD700 : (isLookedAt ? 0xFF00F5FF : iffColor);

        Vec3 basePos = target.getPosition(partialTicks);
        float[] topProj = projectWorldToGui(mc, basePos.x, basePos.y + target.getBbHeight() + 0.12, basePos.z, partialTicks);
        float[] botProj = projectWorldToGui(mc, basePos.x, basePos.y - 0.05, basePos.z, partialTicks);

        int cx = Math.round(cand.screenX);
        int cy = Math.round(cand.screenY);
        int halfH = 12;
        if (topProj != null && botProj != null) {
            halfH = Mth.clamp(Math.round(Math.abs(botProj[1] - topProj[1]) * 0.5f), 8, 34);
        } else {
            halfH = Mth.clamp(Math.round(180.0f / Math.max(3.0f, cand.worldDist)), 8, 30);
        }
        int armLen = Mth.clamp(halfH / 3, 3, 9);

        // 1. Tactical AR corner brackets (double-reinforced when marked or directly focused)
        drawCornerBrackets(gui, cx, cy, halfH, armLen, frameColor);
        if (expanded) {
            drawCornerBrackets(gui, cx, cy, halfH + 2, Math.max(2, armLen - 1), frameColor);
        }

        // 2. Health bar & header above the top bracket
        //    Tier 1 (peripheral): HP only (clean screen)
        //    Tier 2 (looked at / marked): Distance + HP (+ [LOCK] badge if marked)
        float hp = Math.max(0.0f, target.getHealth());
        float maxHp = Math.max(1.0f, target.getMaxHealth());
        float hpPct = Mth.clamp(hp / maxHp, 0.0f, 1.0f);

        int barW = Math.max(24, halfH * 2);
        int barX = cx - barW / 2;
        int barY = cy - halfH - (expanded ? 7 : 5);

        gui.fill(barX - 1, barY - 1, barX + barW + 1, barY + 3, 0xA0080C12);
        int fillW = Math.round(barW * hpPct);
        int barCol = hpPct < 0.25f ? 0xFFFF3333 : (hpPct < 0.60f ? 0xFFFFBB22 : iffColor);
        if (fillW > 0) {
            gui.fill(barX, barY, barX + fillW, barY + 2, barCol);
        }

        String statsLine = expanded
                ? (isMarked ? String.format("[LOCK] %.1fm | %.0f/%.0f", cand.worldDist, hp, maxHp)
                : String.format("%.1fm | %.0f/%.0f", cand.worldDist, hp, maxHp))
                : String.format("%.0f/%.0f", hp, maxHp);
        float textScale = expanded ? 0.66f : 0.56f;
        int statsW = Math.round(mc.font.width(statsLine) * textScale);
        int statsX = Mth.clamp(cx - statsW / 2, 4, screenW - statsW - 4);
        int statsY = barY - Math.round(9 * textScale) - 1;
        gui.fill(statsX - 2, statsY - 1, statsX + statsW + 2, statsY + Math.round(8 * textScale), 0x88080C12);
        drawScaledText(gui, mc, statsLine, statsX, statsY, textScale, expanded ? frameColor : 0xFFE8F2FF);

        // Peripheral targets stop here (only AR frame + HP)
        if (!expanded) {
            return;
        }

        // 3. Aim / Gaze Vector (Tier 2 only): projects where the target/player is looking/aiming + [! AIM !] threat alert
        Vec3 targetEyePos = basePos.add(0.0, target.getEyeHeight(), 0.0);
        Vec3 viewVec = target.getViewVector(partialTicks);
        if (viewVec.lengthSqr() > 1.0e-5) {
            viewVec = viewVec.normalize();
            Vec3 aimEndPos = targetEyePos.add(viewVec.scale(3.2));
            float[] eyeProj = projectWorldToGui(mc, targetEyePos.x, targetEyePos.y, targetEyePos.z, partialTicks);
            float[] endProj = projectWorldToGui(mc, aimEndPos.x, aimEndPos.y, aimEndPos.z, partialTicks);

            Vec3 toLocalPlayer = mc.player.getEyePosition(partialTicks).subtract(targetEyePos);
            double aimAtMeDot = toLocalPlayer.lengthSqr() > 1.0e-4 ? viewVec.dot(toLocalPlayer.normalize()) : -1.0;
            boolean aimedAtMe = (aimAtMeDot > 0.92) || (target instanceof Mob mob && mob.getTarget() == mc.player);

            boolean flashOn = ((System.currentTimeMillis() / 120L) % 2L) == 0L;
            int lineColor = aimedAtMe
                    ? (flashOn ? 0xFFFF2222 : 0xFFFF8844)
                    : (isHostile ? 0xCCFF8844 : 0xCC55EEFF);
            int markerCore = aimedAtMe ? 0xFFFF2222 : 0xEEFFEE55;

            if (eyeProj != null && endProj != null && eyeProj[3] >= 0.5f && endProj[3] >= 0.5f) {
                int ex = Math.round(eyeProj[0]);
                int ey = Math.round(eyeProj[1]);
                int ax = Math.round(endProj[0]);
                int ay = Math.round(endProj[1]);
                float sepSq = (ax - ex) * (ax - ex) + (ay - ey) * (ay - ey);
                if (sepSq >= 16.0f) {
                    drawDashedLine(gui, ex, ey, ax, ay, lineColor);
                    drawLeadMarker(gui, ax, ay, lineColor, markerCore);
                }
            }

            if (aimedAtMe) {
                String warnTag = "[! AIM !]";
                float wScale = 0.62f;
                int wW = Math.round(mc.font.width(warnTag) * wScale);
                int wX = Mth.clamp(cx - wW / 2, 4, screenW - wW - 4);
                int wY = Math.max(4, statsY - Math.round(9 * wScale) - 1);
                int warnCol = flashOn ? 0xFFFF2222 : 0xFFFFCC33;
                gui.fill(wX - 2, wY - 1, wX + wW + 2, wY + Math.round(8 * wScale), 0xB0200404);
                drawScaledText(gui, mc, warnTag, wX, wY, wScale, warnCol);
            }
        }

        // 4. Held Item & Worn Armor Inspector (Tier 2 only: mini item icons below the bottom bracket + held item name)
        List<ItemStack> gear = new ArrayList<>(6);
        for (EquipmentSlot slot : INSPECT_SLOTS) {
            ItemStack st = target.getItemBySlot(slot);
            if (!st.isEmpty()) {
                gear.add(st);
            }
        }
        int bottomY = cy + halfH + 5;
        if (!gear.isEmpty()) {
            float iconScale = 0.55f;
            int iconStep = 10;
            int totalW = gear.size() * iconStep - 1;
            int startX = Mth.clamp(cx - totalW / 2, 4, screenW - totalW - 4);
            gui.fill(startX - 2, bottomY - 1, startX + totalW + 2, bottomY + 10, 0x88080C12);
            for (int g = 0; g < gear.size(); g++) {
                int ix = startX + g * iconStep;
                gui.pose().pushPose();
                gui.pose().translate(ix, bottomY, 0.0f);
                gui.pose().scale(iconScale, iconScale, 1.0f);
                gui.renderItem(gear.get(g), 0, 0);
                gui.pose().popPose();
            }
            bottomY += 12;
        }

        ItemStack held = !target.getMainHandItem().isEmpty() ? target.getMainHandItem() : target.getOffhandItem();
        if (!held.isEmpty()) {
            String itemName = held.getHoverName().getString();
            if (itemName.length() > 24) {
                itemName = itemName.substring(0, 22) + "..";
            }
            String handLine = "HAND: " + itemName;
            float hScale = 0.62f;
            int hW = Math.round(mc.font.width(handLine) * hScale);
            int hX = Mth.clamp(cx - hW / 2, 4, screenW - hW - 4);
            int hY = Math.min(screenH - 12, bottomY);
            gui.fill(hX - 2, hY - 1, hX + hW + 2, hY + Math.round(8 * hScale), 0x99080C12);
            drawScaledText(gui, mc, handLine, hX, hY, hScale, frameColor);
        }
    }

    private static void drawDashedLine(GuiGraphics gui, int x0, int y0, int x1, int y1, int color) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) return;
        float incX = dx / (float) steps;
        float incY = dy / (float) steps;
        float x = x0;
        float y = y0;
        for (int i = 0; i <= steps; i++) {
            if ((i % 6) < 3) {
                int px = Math.round(x);
                int py = Math.round(y);
                gui.fill(px, py, px + 1, py + 1, color);
            }
            x += incX;
            y += incY;
        }
    }

    private static void drawLeadMarker(GuiGraphics gui, int px, int py, int accentColor, int coreColor) {
        // Compact tactical diamond + center crosshair (⊕) at predicted intercept point
        gui.fill(px - 3, py, px + 4, py + 1, coreColor);
        gui.fill(px, py - 3, px + 1, py + 4, coreColor);
        gui.fill(px - 2, py - 2, px - 1, py - 1, accentColor);
        gui.fill(px + 2, py - 2, px + 3, py - 1, accentColor);
        gui.fill(px - 2, py + 2, px - 1, py + 3, accentColor);
        gui.fill(px + 2, py + 2, px + 3, py + 3, accentColor);
    }

    public static void drawScaledText(GuiGraphics gui, Minecraft mc, String text, int x, int y, float scale, int color) {
        gui.pose().pushPose();
        gui.pose().translate(x, y, 0);
        gui.pose().scale(scale, scale, 1.0f);
        gui.drawString(mc.font, text, 0, 0, color, true);
        gui.pose().popPose();
    }

    private static void renderWaterShortCircuitOverlay(GuiGraphics gui, Minecraft mc, int width, int height) {
        float progress = VisionShaderManager.getUnderwaterShortProgress(mc);
        if (progress <= 0.0f) return;

        long seed = System.nanoTime() / 25_000_000L; // ~40 Hz erratic electronic jitter
        java.util.Random rng = new java.util.Random(seed);

        // Brief dark blackout flashes ramping in frequency & opacity
        if (rng.nextFloat() < (0.18f + progress * 0.52f)) {
            int darkAlpha = Math.min(210, Math.round((0.28f + 0.50f * progress) * 255.0f * (0.5f + 0.5f * rng.nextFloat())));
            gui.fill(0, 0, width, height, (darkAlpha << 24));
        }

        // Erratic horizontal static/short-circuit glitch bars
        int bars = 3 + Math.round(progress * 9.0f);
        for (int i = 0; i < bars; i++) {
            int y = rng.nextInt(Math.max(1, height));
            int h = 1 + rng.nextInt(3 + Math.round(progress * 5.0f));
            int x = rng.nextInt(Math.max(1, width / 3));
            int w = width - x - rng.nextInt(Math.max(1, width / 4));
            int alpha = Math.min(200, Math.round((0.20f + 0.55f * progress) * 255.0f * rng.nextFloat()));
            int rgb = rng.nextBoolean() ? 0x99EEFF : 0x101820;
            gui.fill(x, y, Math.min(width, x + w), Math.min(height, y + h), (alpha << 24) | rgb);
        }
    }
}
