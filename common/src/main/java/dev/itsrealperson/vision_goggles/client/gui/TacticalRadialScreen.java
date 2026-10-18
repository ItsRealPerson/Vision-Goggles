package dev.itsrealperson.vision_goggles.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.itsrealperson.vision_goggles.client.ModKeyMappings;
import dev.itsrealperson.vision_goggles.mixin.client.KeyMappingAccessor;
import dev.itsrealperson.vision_goggles.item.BatteryItem;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.network.BatteryPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.network.SelectVisionModePacket;
import dev.itsrealperson.vision_goggles.network.ToggleFlashlightPacket;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TacticalRadialScreen extends Screen {

    public static class RadialOption {
        public final String title;
        public final String subtitle;
        public final int color;
        public final Runnable action;
        public final boolean enabled;

        public RadialOption(String title, String subtitle, int color, boolean enabled, Runnable action) {
            this.title = title;
            this.subtitle = subtitle;
            this.color = color;
            this.enabled = enabled;
            this.action = action;
        }
    }

    private final List<RadialOption> options = new ArrayList<>();
    private int selectedIndex = -1;
    private int lastHoverIndex = -1;

    public TacticalRadialScreen() {
        super(Component.literal("Menú Radial Táctico"));
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

        boolean hasNVG = hasMode(modes, VisionMode.NIGHT_VISION);
        boolean hasThermal = hasMode(modes, VisionMode.THERMAL);
        boolean hasBio = hasMode(modes, VisionMode.BIOMETRIC);
        boolean hasHydro = hasMode(modes, VisionMode.HYDRO);

        // 0: NVG
        options.add(new RadialOption("NVG", "Visión Nocturna", 0xFF00FF55, hasNVG, () -> {
            NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.NIGHT_VISION.getId()));
        }));

        // 1: THERMAL
        options.add(new RadialOption("THERMAL", "Visor Térmico", 0xFFFF5533, hasThermal, () -> {
            NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.THERMAL.getId()));
        }));

        // 2: BIOMETRIC
        options.add(new RadialOption("BIOMÉTRICO", "Radar Vital", 0xFFFFAA00, hasBio, () -> {
            NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.BIOMETRIC.getId()));
        }));

        // 3: HYDRO
        options.add(new RadialOption("HYDRO", "Visión Subacuática", 0xFF00CCFF, hasHydro, () -> {
            NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.HYDRO.getId()));
        }));

        // 4: FLASHLIGHT
        options.add(new RadialOption("LINTERNA", "Alternar Luz", 0xFFFFFF88, true, () -> {
            NetworkManager.INSTANCE.sendToServer(new ToggleFlashlightPacket());
        }));

        // 5: RECARGA
        options.add(new RadialOption("BATERÍA", "Recarga Rápida", 0xFF55FF55, true, () -> {
            NetworkManager.INSTANCE.sendToServer(new BatteryPacket());
        }));

        // 6: STANDBY / OFF
        options.add(new RadialOption("STANDBY", "Apagar / Levantar", 0xFFAAAAAA, hasHelmet, () -> {
            NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(-1));
        }));
    }

    private boolean hasMode(List<VisionMode> modes, VisionMode target) {
        for (VisionMode m : modes) {
            if (m == target) return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int openTicks = 0;
    private boolean executed = false;

    private boolean isKeyPhysicallyDown() {
        if (minecraft == null) return false;
        long window = minecraft.getWindow().getWindow();
        com.mojang.blaze3d.platform.InputConstants.Key key = ((KeyMappingAccessor) ModKeyMappings.toggleGrayscaleKey).vision_goggles$getKey();
        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM) {
            return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, key.getValue());
        } else if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        openTicks++;
        // When user releases the physical key after holding it, execute and close
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
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
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
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Subtle dark vignette background
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x55000000, 0x77000000);

        // Calculate hovered slice based on angle
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        int count = options.size();
        if (dist > 28.0 && count > 0) {
            double angle = Math.toDegrees(Math.atan2(dy, dx)); // -180 to 180
            if (angle < 0) angle += 360.0; // 0 to 360, 0 is right (East)

            // Offset angle so slice 0 starts at Top (-90 degrees)
            double sliceAngle = 360.0 / count;
            double adjustedAngle = (angle + 90.0 + (sliceAngle / 2.0)) % 360.0;
            selectedIndex = (int) (adjustedAngle / sliceAngle);
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

        // Draw tactical circular brackets & slices
        float radius = 78.0f;
        double sliceAngleRad = Math.toRadians(360.0 / count);

        // Center hub circle
        int hubColor = 0xAA0E1418;
        guiGraphics.fill(centerX - 24, centerY - 24, centerX + 24, centerY + 24, hubColor);

        // Draw slices
        for (int i = 0; i < count; i++) {
            RadialOption opt = options.get(i);
            boolean isSelected = (i == selectedIndex);

            // Angle for slice center (starting from Top: -PI/2)
            double centerAngle = -Math.PI / 2.0 + (i * sliceAngleRad);
            int optX = (int) (centerX + Math.cos(centerAngle) * radius);
            int optY = (int) (centerY + Math.sin(centerAngle) * radius);

            int btnW = 54;
            int btnH = 22;
            int x1 = optX - btnW / 2;
            int y1 = optY - btnH / 2;
            int x2 = optX + btnW / 2;
            int y2 = optY + btnH / 2;

            int bgBoxColor;
            int textColor;
            int borderColor;

            if (!opt.enabled) {
                bgBoxColor = 0x66111111;
                textColor = 0x66888888;
                borderColor = 0x44444444;
            } else if (isSelected) {
                bgBoxColor = 0xEE142A24;
                textColor = 0xFFFFFFFF;
                borderColor = opt.color;
            } else {
                bgBoxColor = 0xAA0E1418;
                textColor = opt.color;
                borderColor = 0x55335544;
            }

            // Box and tactical border
            guiGraphics.fill(x1, y1, x2, y2, bgBoxColor);
            guiGraphics.renderOutline(x1, y1, btnW, btnH, borderColor);

            // Label
            int strW = this.font.width(opt.title);
            guiGraphics.drawString(this.font, opt.title, optX - strW / 2, optY - 4, textColor, false);

            if (isSelected) {
                // Tactical selection pip line
                guiGraphics.hLine(x1 - 3, x2 + 3, y1 - 2, opt.color);
                guiGraphics.hLine(x1 - 3, x2 + 3, y2 + 2, opt.color);
            }
        }

        // Center hub telemetry
        if (selectedIndex >= 0 && selectedIndex < count) {
            RadialOption sel = options.get(selectedIndex);
            int subW = this.font.width(sel.subtitle);
            guiGraphics.drawString(this.font, sel.title, centerX - this.font.width(sel.title) / 2, centerY - 9, sel.color, true);
            guiGraphics.drawString(this.font, sel.subtitle, centerX - subW / 2, centerY + 3, 0xFFAAAAAA, false);
        } else {
            guiGraphics.drawString(this.font, "TACTICAL", centerX - this.font.width("TACTICAL") / 2, centerY - 9, 0xFF00FF88, true);
            guiGraphics.drawString(this.font, "DIAL", centerX - this.font.width("DIAL") / 2, centerY + 3, 0xFF888888, false);
        }

        // Reticle crosshair in center
        guiGraphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, 0xFF00FFAA);
    }
}
