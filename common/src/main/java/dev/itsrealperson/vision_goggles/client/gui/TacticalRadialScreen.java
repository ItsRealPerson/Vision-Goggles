package dev.itsrealperson.vision_goggles.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.itsrealperson.vision_goggles.client.ModKeyMappings;
import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.mixin.client.KeyMappingAccessor;
import dev.itsrealperson.vision_goggles.network.BatteryPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.network.SelectVisionModePacket;
import dev.itsrealperson.vision_goggles.network.ToggleFlashlightPacket;
import dev.itsrealperson.vision_goggles.network.ToggleSonarPacket;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.ModuleType;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft-styled radial action wheel using crisp pixel-aligned panels,
 * recessed 18x18 item slots, and a classic Minecraft tooltip hub.
 */
public class TacticalRadialScreen extends Screen {

    public static class RadialOption {
        public final String title;
        public final String subtitle;
        public final int color;
        public final ItemStack icon;
        public final Runnable action;
        public final boolean enabled;

        public RadialOption(String title, String subtitle, int color, ItemStack icon, boolean enabled, Runnable action) {
            this.title = title;
            this.subtitle = subtitle;
            this.color = color;
            this.icon = icon;
            this.enabled = enabled;
            this.action = action;
        }
    }

    private final List<RadialOption> options = new ArrayList<>();
    private int selectedIndex = -1;
    private int lastHoverIndex = -1;
    private int openTicks = 0;
    private boolean executed = false;

    private static final int TILE_W = 54;
    private static final int TILE_H = 34;
    private static final int DEADZONE_RADIUS = 24;

    public TacticalRadialScreen() {
        super(Component.translatable("gui.vision_goggles.radial.title"));
    }

    public static boolean hasFlashlightAvailable(Player player) {
        if (player == null) return false;
        if (!TacticalFlashlightItem.getAnyHeldFlashlight(player).isEmpty()) return true;
        ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
        if (!helmet.isEmpty() && helmet.getItem() instanceof ModularGogglesItem modular) {
            return modular.getUtilityModules(helmet).contains(ModuleType.FLASHLIGHT);
        }
        return false;
    }

    @Override
    protected void init() {
        super.init();
        options.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack helmet = PlatformMethods.getEquippedHelmet(mc.player);
        boolean hasHelmet = !helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem;
        VisionGogglesItem goggles = hasHelmet ? (VisionGogglesItem) helmet.getItem() : null;

        List<VisionMode> modes = new ArrayList<>();
        if (hasHelmet) {
            if (goggles instanceof ModularGogglesItem modular) {
                modes = modular.getModes(helmet);
            } else {
                modes = goggles.getSupportedModes();
            }
        }

        for (VisionMode m : modes) {
            if (m == VisionMode.NIGHT_VISION) {
                options.add(new RadialOption(
                        Component.translatable("gui.vision_goggles.radial.nvg").getString(),
                        Component.translatable("gui.vision_goggles.radial.nvg.desc").getString(),
                        0xFF55FF55,
                        ModItems.NVG_MODULE.get().getDefaultInstance(),
                        true,
                        () -> NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.NIGHT_VISION.getId()))
                ));
            } else if (m == VisionMode.THERMAL) {
                options.add(new RadialOption(
                        Component.translatable("gui.vision_goggles.radial.thermal").getString(),
                        Component.translatable("gui.vision_goggles.radial.thermal.desc").getString(),
                        0xFFFF5555,
                        ModItems.THERMAL_MODULE.get().getDefaultInstance(),
                        true,
                        () -> NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.THERMAL.getId()))
                ));
            } else if (m == VisionMode.BIOMETRIC) {
                options.add(new RadialOption(
                        Component.translatable("gui.vision_goggles.radial.bio").getString(),
                        Component.translatable("gui.vision_goggles.radial.bio.desc").getString(),
                        0xFFFFAA00,
                        ModItems.BIO_MODULE.get().getDefaultInstance(),
                        true,
                        () -> NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.BIOMETRIC.getId()))
                ));
            } else if (m == VisionMode.HYDRO) {
                options.add(new RadialOption(
                        Component.translatable("gui.vision_goggles.radial.hydro").getString(),
                        Component.translatable("gui.vision_goggles.radial.hydro.desc").getString(),
                        0xFF55FFFF,
                        ModItems.HYDRO_MODULE.get().getDefaultInstance(),
                        true,
                        () -> NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.HYDRO.getId()))
                ));
            }
        }

        if (hasHelmet) {
            options.add(new RadialOption(
                    Component.translatable("gui.vision_goggles.radial.standby").getString(),
                    Component.translatable("gui.vision_goggles.radial.standby.desc").getString(),
                    0xFFAAAAAA,
                    ModItems.MODULAR_GOGGLES.get().getDefaultInstance(),
                    true,
                    () -> NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(-1))
            ));

            if (goggles.hasModule(helmet, ModConstants.ID_SONAR)) {
                CompoundTag nbt = helmet.getTag();
                int currentMode = (nbt != null && nbt.contains(ModConstants.TAG_SONAR_MODE)) ? nbt.getInt(ModConstants.TAG_SONAR_MODE) : 0;
                String modeDesc = Component.translatable("gui.vision_goggles.radial.sonar." + (currentMode == 0 ? "all" : (currentMode == 1 ? "players" : "undead"))).getString();
                options.add(new RadialOption(
                        Component.translatable("gui.vision_goggles.radial.sonar").getString(),
                        modeDesc,
                        0xFF55FFFF,
                        ModItems.SONAR_MODULE.get().getDefaultInstance(),
                        true,
                        () -> NetworkManager.INSTANCE.sendToServer(new ToggleSonarPacket())
                ));
            }

            if (goggles.hasModule(helmet, ModConstants.ID_ZOOM)) {
                boolean locked = VisionRenderer.isZoomLocked();
                String zoomDesc = Component.translatable(locked ? "gui.vision_goggles.radial.zoom.locked" : "gui.vision_goggles.radial.zoom.desc").getString();
                options.add(new RadialOption(
                        Component.translatable("gui.vision_goggles.radial.zoom").getString(),
                        zoomDesc,
                        0xFF55FFFF,
                        ModItems.ZOOM_MODULE.get().getDefaultInstance(),
                        true,
                        () -> VisionRenderer.toggleZoomLock(mc)
                ));
            }
        }

        if (hasFlashlightAvailable(mc.player)) {
            options.add(new RadialOption(
                    Component.translatable("gui.vision_goggles.radial.flashlight").getString(),
                    Component.translatable("gui.vision_goggles.radial.flashlight.desc").getString(),
                    0xFFFFFF55,
                    ModItems.TACTICAL_FLASHLIGHT.get().getDefaultInstance(),
                    true,
                    () -> {
                        NetworkManager.INSTANCE.sendToServer(new ToggleFlashlightPacket());
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.FLASHLIGHT_SWITCH.get(), 1.0F));
                    }
            ));
        }

        boolean hasRechargeable = hasHelmet || hasFlashlightAvailable(mc.player);
        if (hasRechargeable) {
            options.add(new RadialOption(
                    Component.translatable("gui.vision_goggles.radial.battery").getString(),
                    Component.translatable("gui.vision_goggles.radial.battery.desc").getString(),
                    0xFF55FF55,
                    ModItems.NVG_BATTERY.get().getDefaultInstance(),
                    true,
                    () -> NetworkManager.INSTANCE.sendToServer(new BatteryPacket())
            ));
        }

        if (options.isEmpty()) {
            options.add(new RadialOption(
                    Component.translatable("gui.vision_goggles.radial.no_gear").getString(),
                    Component.translatable("gui.vision_goggles.radial.no_gear.desc").getString(),
                    0xFF888888,
                    ItemStack.EMPTY,
                    false,
                    () -> {}
            ));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isKeyPhysicallyDown() {
        if (minecraft == null) return false;
        long window = minecraft.getWindow().getWindow();
        InputConstants.Key key = ((KeyMappingAccessor) ModKeyMappings.toggleGrayscaleKey).vision_goggles$getKey();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, key.getValue());
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        openTicks++;
        if (openTicks > 3 && !isKeyPhysicallyDown()) {
            executeAndClose();
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.toggleGrayscaleKey.matches(keyCode, scanCode)) {
            executeAndClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            executeAndClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void executeAndClose() {
        if (executed) return;
        executed = true;
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            RadialOption opt = options.get(selectedIndex);
            if (opt.enabled) {
                opt.action.run();
                if (minecraft != null) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 1.2F));
                }
            }
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int count = options.size();
        int nodeRadius = (count >= 7) ? 82 : 74;

        // Subtle vanilla-style screen darkening
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x55000000, 0x77000000);

        // Polar angle selection from center
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist >= DEADZONE_RADIUS && count > 0) {
            double angleRad = Math.atan2(dy, dx);
            double relAngle = angleRad - (-Math.PI / 2.0);
            while (relAngle < 0) relAngle += 2.0 * Math.PI;
            while (relAngle >= 2.0 * Math.PI) relAngle -= 2.0 * Math.PI;

            double sliceAngle = (2.0 * Math.PI) / count;
            double shiftedAngle = relAngle + (sliceAngle / 2.0);
            if (shiftedAngle >= 2.0 * Math.PI) shiftedAngle -= 2.0 * Math.PI;

            selectedIndex = (int) (shiftedAngle / sliceAngle);
            if (selectedIndex >= count) selectedIndex = count - 1;
        } else {
            selectedIndex = -1;
        }

        if (selectedIndex != lastHoverIndex) {
            if (selectedIndex >= 0 && minecraft != null && options.get(selectedIndex).enabled) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 1.6F, 0.25F));
            }
            lastHoverIndex = selectedIndex;
        }

        // Minecraft beveled action buttons arranged in a clean circle (no lines or rings)
        double sliceAngle = (2.0 * Math.PI) / Math.max(1, count);
        for (int i = 0; i < count; i++) {
            RadialOption opt = options.get(i);
            boolean isSel = (i == selectedIndex);
            double midAngle = -Math.PI / 2.0 + (i * sliceAngle);
            int nodeCx = cx + (int) Math.round(Math.cos(midAngle) * nodeRadius);
            int nodeCy = cy + (int) Math.round(Math.sin(midAngle) * nodeRadius);

            int tx = nodeCx - TILE_W / 2;
            int ty = nodeCy - TILE_H / 2;

            renderActionTile(guiGraphics, tx, ty, opt, isSel);
        }

        // Compact Minecraft beveled center info badge
        renderCenterHub(guiGraphics, cx, cy, count);
    }

    private void renderActionTile(GuiGraphics guiGraphics, int tx, int ty, RadialOption opt, boolean isSel) {
        if (!opt.enabled) {
            drawBevelPanel(guiGraphics, tx, ty, TILE_W, TILE_H, 0xFF1F2227, 0xFF32363E, 0xFF131519, 0xFF0E1012);
        } else if (isSel) {
            drawBevelPanel(guiGraphics, tx, ty, TILE_W, TILE_H, 0xFF3E4550, 0xFF7A8494, 0xFF22262C, 0xFF0E1012);
        } else {
            drawBevelPanel(guiGraphics, tx, ty, TILE_W, TILE_H, 0xFF2B2F36, 0xFF545B66, 0xFF191C20, 0xFF0E1012);
        }

        // Classic 18x18 recessed Minecraft slot for the item icon
        int slotX = tx + (TILE_W - 18) / 2;
        int slotY = ty + 3;
        drawSlotBox(guiGraphics, slotX, slotY);

        if (!opt.icon.isEmpty()) {
            guiGraphics.renderItem(opt.icon, slotX + 1, slotY + 1);
        }

        if (isSel && opt.enabled) {
            guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x40FFFFFF);
        }

        // Centered shadowed label below the slot
        int textColor = !opt.enabled ? 0xFF777777 : (isSel ? 0xFFFFFF55 : 0xFFE0E0E0);
        int strW = this.font.width(opt.title);
        int maxTextW = TILE_W - 6;

        if (strW > maxTextW) {
            float scale = Math.max(0.68f, (float) maxTextW / (float) strW);
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(tx + TILE_W / 2.0f, ty + 23.5f, 200);
            pose.scale(scale, scale, 1.0f);
            guiGraphics.drawString(this.font, opt.title, -strW / 2, 0, textColor, true);
            pose.popPose();
        } else {
            guiGraphics.drawString(this.font, opt.title, tx + (TILE_W - strW) / 2, ty + 23, textColor, true);
        }
    }

    private void renderCenterHub(GuiGraphics guiGraphics, int cx, int cy, int count) {
        if (selectedIndex >= 0 && selectedIndex < count) {
            RadialOption sel = options.get(selectedIndex);
            String line1 = sel.title;
            String line2 = sel.subtitle;
            int color1 = sel.enabled ? sel.color : 0xFF888888;

            int w1 = this.font.width(line1);
            int w2 = this.font.width(line2);
            int boxW = Math.max(76, Math.max(w1, w2) + 14);
            int boxH = 26;
            int bx = cx - boxW / 2;
            int by = cy - boxH / 2;

            drawBevelPanel(guiGraphics, bx, by, boxW, boxH, 0xFF23272D, 0xFF4A505A, 0xFF15181C, 0xFF0E1012);
            guiGraphics.drawString(this.font, line1, cx - w1 / 2, by + 4, color1, true);
            guiGraphics.drawString(this.font, line2, cx - w2 / 2, by + 15, 0xFFB0B8C2, true);
        } else {
            String titleStr = this.title.getString();
            int w1 = this.font.width(titleStr);
            int boxW = Math.max(72, w1 + 14);
            int boxH = 16;
            int bx = cx - boxW / 2;
            int by = cy - boxH / 2;

            drawBevelPanel(guiGraphics, bx, by, boxW, boxH, 0xFF23272D, 0xFF4A505A, 0xFF15181C, 0xFF0E1012);
            guiGraphics.drawString(this.font, titleStr, cx - w1 / 2, by + 4, 0xFFE0E0E0, true);
        }
    }

    private static void drawSlotBox(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF1D2025);
        guiGraphics.fill(x, y, x + 17, y + 1, 0xFF101215);
        guiGraphics.fill(x, y, x + 1, y + 17, 0xFF101215);
        guiGraphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFF4A505A);
        guiGraphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFF4A505A);
    }

    private static void drawBevelPanel(GuiGraphics guiGraphics, int x, int y, int w, int h, int body, int light, int dark, int outline) {
        guiGraphics.fill(x, y, x + w, y + h, outline);
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, body);
        guiGraphics.fill(x + 1, y + 1, x + w - 2, y + 2, light);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + h - 2, light);
        guiGraphics.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, dark);
        guiGraphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, dark);
    }
}
