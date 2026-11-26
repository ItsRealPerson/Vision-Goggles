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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.AbstractGolem;
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
import java.util.List;

public class VisionHUDOverlay {
    private static final List<IHudModule> MODULES = new ArrayList<>();
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

        double range = 32.0;
        Vec3 eyePos = player.getEyePosition(partialTicks);
        Vec3 look = player.getViewVector(partialTicks);
        Vec3 targetEnd = eyePos.add(look.scale(range));
        double maxDist = range;

        BlockHitResult blockHit = mc.level.clip(new ClipContext(
                eyePos, targetEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            maxDist = eyePos.distanceTo(blockHit.getLocation());
            targetEnd = blockHit.getLocation();
        }

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eyePos, targetEnd, searchBox,
                e -> !e.isSpectator() && e.isPickable() && e instanceof LivingEntity,
                maxDist * maxDist
        );

        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            lastBiometricTargetId = -1;
            return;
        }

        long now = System.currentTimeMillis();
        if (target.getId() != lastBiometricTargetId) {
            lastBiometricTargetId = target.getId();
            biometricLockStartMs = now;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 1.55F, 0.25F));
        }

        float lockAnim = Mth.clamp((now - biometricLockStartMs) / 220.0f, 0.0f, 1.0f);
        int midX = width / 2;
        int midY = height / 2;

        boolean isUndead = target.getMobType() == MobType.UNDEAD;
        boolean isConstruct = target instanceof AbstractGolem;
        boolean isHostile = target instanceof Enemy || target instanceof Monster
                || (target instanceof NeutralMob neutral && neutral.isAngry());
        boolean isFriendly = (target instanceof Player)
                || (target instanceof Npc)
                || (target instanceof TamableAnimal tamable && tamable.isTame())
                || (target instanceof AbstractGolem && !isHostile);

        String iffTag;
        int iffColor;
        if (isHostile) {
            iffTag = "[HOSTIL]";
            iffColor = 0xFFFF3B3B; // Tactical Red
        } else if (isFriendly) {
            iffTag = (target instanceof Player) ? "[JUGADOR]" : "[ALIADO]";
            iffColor = 0xFF22EEAA; // Cyan / Emerald Green
        } else {
            iffTag = "[NEUTRAL]";
            iffColor = 0xFFFFAA22; // Tactical Amber
        }

        int retHalf = Math.round(17.0f - lockAnim * 6.0f);
        drawCornerBrackets(gui, midX, midY, retHalf, 4, iffColor);

        float hp = Math.max(0.0f, target.getHealth());
        float maxHp = Math.max(1.0f, target.getMaxHealth());
        float hpPct = Mth.clamp(hp / maxHp, 0.0f, 1.0f);
        int armor = target.getArmorValue();
        double dist = eyePos.distanceTo(target.position().add(0, target.getBbHeight() * 0.5, 0));

        String vitalStatus;
        int vitalColor;
        if (isUndead) {
            vitalStatus = "BPM: 0 [NECROTICO]";
            vitalColor = 0xFFCC66FF;
        } else if (isConstruct) {
            vitalStatus = "BPM: -- [SINTETICO]";
            vitalColor = 0xFF66DDFF;
        } else {
            int baseBpm = isHostile ? 98 : 72;
            int stressBpm = Math.round((1.0f - hpPct) * 68.0f) + (target.hurtTime > 0 ? 24 : 0);
            int jitter = (int) Math.round(Math.sin(now * 0.008 + target.getId()) * 3.0);
            int bpm = Mth.clamp(baseBpm + stressBpm + jitter, 45, 210);
            String stateLabel = hpPct < 0.25f ? "CRITICO" : (hpPct < 0.60f ? "HERIDO" : "ESTABLE");
            vitalStatus = String.format("♥ %d BPM [%s]", bpm, stateLabel);
            vitalColor = hpPct < 0.25f ? 0xFFFF4444 : (hpPct < 0.60f ? 0xFFFFCC33 : 0xFF55FF88);
        }

        String titleLine = iffTag + " " + target.getName().getString();
        String hpLine = String.format("HP: %.1f / %.1f  ARM: %d", hp, maxHp, armor);
        String distLine = String.format("DIST: %.1fm  |  %s", dist, vitalStatus);

        float scale = 0.72f;
        int wTitle = Math.round(mc.font.width(titleLine) * scale);
        int wHp = Math.round(mc.font.width(hpLine) * scale);
        int wDist = Math.round(mc.font.width(distLine) * scale);
        int cardW = Math.max(124, Math.max(wTitle, Math.max(wHp, wDist)) + 12);
        int cardH = 38;
        int cardX = Math.min(width - cardW - 6, midX + 16);
        int cardY = midY - cardH / 2;

        // Tactical dark panel with IFF accent bar
        gui.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0xB8090D14);
        gui.fill(cardX, cardY, cardX + 2, cardY + cardH, iffColor);
        gui.fill(cardX + 2, cardY, cardX + cardW, cardY + 1, 0x55FFFFFF & iffColor | 0x44000000);

        // Scaled text rendering
        drawScaledText(gui, mc, titleLine, cardX + 6, cardY + 4, scale, iffColor);

        // Health bar
        int barX = cardX + 6;
        int barY = cardY + 13;
        int barW = cardW - 12;
        gui.fill(barX, barY, barX + barW, barY + 4, 0xAA182028);
        int fillW = Math.round(barW * hpPct);
        int barCol = hpPct < 0.25f ? 0xFFFF3333 : (hpPct < 0.60f ? 0xFFFFBB22 : iffColor);
        if (fillW > 0) {
            gui.fill(barX, barY, barX + fillW, barY + 4, barCol);
        }

        drawScaledText(gui, mc, hpLine, cardX + 6, cardY + 20, 0.68f, 0xFFE6EEF8);
        drawScaledText(gui, mc, distLine, cardX + 6, cardY + 28, 0.66f, vitalColor);
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
