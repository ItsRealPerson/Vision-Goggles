package dev.itsrealperson.vision_goggles.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.itsrealperson.vision_goggles.client.ModKeyMappings;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.mixin.client.KeyMappingAccessor;
import dev.itsrealperson.vision_goggles.network.BatteryPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.network.SelectVisionModePacket;
import dev.itsrealperson.vision_goggles.network.ToggleFlashlightPacket;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.ModuleType;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

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

    // Dimensiones calibradas para eliminar solapamientos y alojar sockets tácticos
    private static final float INNER_RADIUS = 42.0f;
    private static final float OUTER_RADIUS = 112.0f;
    private static final float MID_RADIUS = (INNER_RADIUS + OUTER_RADIUS) / 2.0f; // 77.0f
    private static final float COMPASS_RING_RADIUS = 120.0f;
    private static final double GAP_RADIANS = Math.toRadians(3.0);

    public TacticalRadialScreen() {
        super(Component.translatable("gui.vision_goggles.radial.title"));
    }

    public static boolean hasFlashlightAvailable(net.minecraft.world.entity.player.Player player) {
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

        // Modos de visión instalados
        for (VisionMode m : modes) {
            if (m == VisionMode.NIGHT_VISION) {
                options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.nvg").getString(),
                        Component.translatable("gui.vision_goggles.radial.nvg.desc").getString(), 0xFF00FF55,
                        ModItems.NVG_MODULE.get().getDefaultInstance(), true, () -> {
                    NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.NIGHT_VISION.getId()));
                }));
            } else if (m == VisionMode.THERMAL) {
                options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.thermal").getString(),
                        Component.translatable("gui.vision_goggles.radial.thermal.desc").getString(), 0xFFFF5533,
                        ModItems.THERMAL_MODULE.get().getDefaultInstance(), true, () -> {
                    NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.THERMAL.getId()));
                }));
            } else if (m == VisionMode.BIOMETRIC) {
                options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.bio").getString(),
                        Component.translatable("gui.vision_goggles.radial.bio.desc").getString(), 0xFFFFAA00,
                        ModItems.BIO_MODULE.get().getDefaultInstance(), true, () -> {
                    NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.BIOMETRIC.getId()));
                }));
            } else if (m == VisionMode.HYDRO) {
                options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.hydro").getString(),
                        Component.translatable("gui.vision_goggles.radial.hydro.desc").getString(), 0xFF00CCFF,
                        ModItems.HYDRO_MODULE.get().getDefaultInstance(), true, () -> {
                    NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(VisionMode.HYDRO.getId()));
                }));
            }
        }

        // Opciones de Gafas Equipadas
        if (hasHelmet) {
            // Se usa el ítem modular limpio de registro para evitar que el casco 3D se vea oscuro
            options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.standby").getString(),
                    Component.translatable("gui.vision_goggles.radial.standby.desc").getString(), 0xFFAAAAAA,
                    ModItems.MODULAR_GOGGLES.get().getDefaultInstance(), true, () -> {
                NetworkManager.INSTANCE.sendToServer(new SelectVisionModePacket(-1));
            }));

            if (goggles.hasModule(helmet, ModConstants.ID_SPAWN_SECURITY)) {
                boolean enabled = dev.itsrealperson.vision_goggles.client.VisionWorldRenderer.isSpawnSecurityEnabled();
                String secDesc = Component.translatable(enabled ? "gui.vision_goggles.radial.security.on" : "gui.vision_goggles.radial.security.off").getString();
                options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.security").getString(), secDesc, 0xFFFF9900,
                        ModItems.SPAWN_SECURITY_MODULE.get().getDefaultInstance(), true, () -> {
                    dev.itsrealperson.vision_goggles.client.VisionWorldRenderer.toggleSpawnSecurity(mc);
                }));
            }

            if (goggles.hasModule(helmet, ModConstants.ID_SONAR)) {
                CompoundTag nbt = helmet.getTag();
                int currentMode = (nbt != null && nbt.contains(ModConstants.TAG_SONAR_MODE)) ? nbt.getInt(ModConstants.TAG_SONAR_MODE) : 0;
                String modeDesc = Component.translatable("gui.vision_goggles.radial.sonar." + (currentMode == 0 ? "all" : (currentMode == 1 ? "players" : "undead"))).getString();
                options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.sonar").getString(), modeDesc, 0xFF00EEEE,
                        ModItems.SONAR_MODULE.get().getDefaultInstance(), true, () -> {
                    NetworkManager.INSTANCE.sendToServer(new dev.itsrealperson.vision_goggles.network.ToggleSonarPacket());
                }));
            }

            if (goggles.hasModule(helmet, ModConstants.ID_ZOOM)) {
                boolean locked = dev.itsrealperson.vision_goggles.client.VisionRenderer.isZoomLocked();
                String zoomDesc = Component.translatable(locked ? "gui.vision_goggles.radial.zoom.locked" : "gui.vision_goggles.radial.zoom.desc").getString();
                options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.zoom").getString(), zoomDesc, 0xFF55FFFF,
                        ModItems.ZOOM_MODULE.get().getDefaultInstance(), true, () -> {
                    dev.itsrealperson.vision_goggles.client.VisionRenderer.toggleZoomLock(mc);
                }));
            }
        }

        // Linterna táctica (solo si está disponible)
        if (hasFlashlightAvailable(mc.player)) {
            options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.flashlight").getString(),
                    Component.translatable("gui.vision_goggles.radial.flashlight.desc").getString(), 0xFFFFFF88,
                    ModItems.TACTICAL_FLASHLIGHT.get().getDefaultInstance(), true, () -> {
                NetworkManager.INSTANCE.sendToServer(new ToggleFlashlightPacket());
                if (mc != null) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.FLASHLIGHT_SWITCH.get(), 1.0F));
                }
            }));
        }

        // Recarga rápida de batería
        boolean hasRechargeable = hasHelmet || hasFlashlightAvailable(mc.player);
        if (hasRechargeable) {
            options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.battery").getString(),
                    Component.translatable("gui.vision_goggles.radial.battery.desc").getString(), 0xFF55FF55,
                    ModItems.NVG_BATTERY.get().getDefaultInstance(), true, () -> {
                NetworkManager.INSTANCE.sendToServer(new BatteryPacket());
            }));
        }

        if (options.isEmpty()) {
            options.add(new RadialOption(Component.translatable("gui.vision_goggles.radial.no_gear").getString(),
                    Component.translatable("gui.vision_goggles.radial.no_gear.desc").getString(), 0xFF888888,
                    ItemStack.EMPTY, false, () -> {}));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

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
        float cx = this.width / 2.0f;
        float cy = this.height / 2.0f;
        int count = options.size();

        // 1. Viñeta oscura de fondo
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x66000000, 0x88000000);

        // 2. Encabezado de telemetría militar Alpha
        guiGraphics.drawString(this.font, Component.translatable("gui.vision_goggles.radial.header").getString(), 12, 12, 0xFFAAAAAA, false);

        // 3. Detección polar de la rebanada bajo el ratón
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist >= INNER_RADIUS - 4.0f && count > 0) {
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

        // Sonido táctico al cambiar de sector
        if (selectedIndex != lastHoverIndex) {
            if (selectedIndex >= 0 && minecraft != null && options.get(selectedIndex).enabled) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), 1.6F, 0.25F));
            }
            lastHoverIndex = selectedIndex;
        }

        Matrix4f matrix = guiGraphics.pose().last().pose();

        // 4. PASO 1: Renderizado de Geometría Táctica 2D con Blaze3D
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        // A. Cuñas Donut (Sectores Anulares)
        double sliceAngle = (2.0 * Math.PI) / count;
        for (int i = 0; i < count; i++) {
            RadialOption opt = options.get(i);
            boolean isSel = (i == selectedIndex);

            double midAngle = -Math.PI / 2.0 + (i * sliceAngle);
            double startAngle = midAngle - (sliceAngle / 2.0) + (GAP_RADIANS / 2.0);
            double endAngle = midAngle + (sliceAngle / 2.0) - (GAP_RADIANS / 2.0);

            float rOut = isSel ? (OUTER_RADIUS + 4.0f) : OUTER_RADIUS;
            float rIn = INNER_RADIUS;

            int colorInner;
            int colorOuter;

            if (!opt.enabled) {
                colorInner = 0x550A0E12;
                colorOuter = 0x5511171E;
            } else if (isSel) {
                colorInner = 0xCC112520;
                colorOuter = (opt.color & 0x00FFFFFF) | 0x88000000;
            } else {
                colorInner = 0xAA0B1116;
                colorOuter = 0xCC0E1820;
            }

            drawDonutWedge(builder, matrix, cx, cy, rIn, rOut, startAngle, endAngle, colorInner, colorOuter, 16);

            // Borde exterior acentuado
            int rimColor = !opt.enabled ? 0x33444444 : (isSel ? opt.color : 0x44336655);
            float rimThickness = isSel ? 2.5f : 1.2f;
            drawDonutWedge(builder, matrix, cx, cy, rOut - rimThickness, rOut, startAngle, endAngle, rimColor, rimColor, 16);

            // Borde interior fino
            int inRimColor = isSel ? (opt.color & 0x88FFFFFF) : 0x3322443B;
            drawDonutWedge(builder, matrix, cx, cy, rIn, rIn + 1.2f, startAngle, endAngle, inRimColor, inRimColor, 16);

            // Sockets tácticos militares centrados para el icono
            if (!opt.icon.isEmpty()) {
                float wedgeCx = (float) (cx + Math.cos(midAngle) * MID_RADIUS);
                float wedgeCy = (float) (cy + Math.sin(midAngle) * MID_RADIUS);
                drawTacticalSocket(builder, matrix, wedgeCx, wedgeCy - 6.0f, isSel, opt.color, opt.enabled);
            }
        }

        // B. Disco Central de Telemetría (Hub Interior)
        drawFilledCircle(builder, matrix, cx, cy, INNER_RADIUS - 2.0f, 0xDD090F13, 32);
        drawDonutWedge(builder, matrix, cx, cy, INNER_RADIUS - 3.5f, INNER_RADIUS - 2.0f, 0, Math.PI * 2, 0x66336655, 0x66336655, 32);

        // C. Anillo Exterior de Compás con Marcas de Grados (Ticks)
        drawCompassRingAndTicks(builder, matrix, cx, cy, COMPASS_RING_RADIUS);

        // D. Retícula / Mira Central Militar
        drawCenterReticle(builder, matrix, cx, cy, selectedIndex >= 0 ? options.get(selectedIndex).color : 0xFF00FFAA);

        tesselator.end();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        // 5. PASO 2: Renderizado de Iconos dentro del Socket Táctico
        for (int i = 0; i < count; i++) {
            RadialOption opt = options.get(i);
            if (opt.icon.isEmpty()) continue;

            boolean isSel = (i == selectedIndex);
            double midAngle = -Math.PI / 2.0 + (i * sliceAngle);

            float wedgeCx = (float) (cx + Math.cos(midAngle) * MID_RADIUS);
            float wedgeCy = (float) (cy + Math.sin(midAngle) * MID_RADIUS);

            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(wedgeCx, wedgeCy - 6.0f, 0);
            if (isSel) {
                pose.scale(1.15f, 1.15f, 1.0f);
            }
            guiGraphics.renderItem(opt.icon, -8, -8);
            pose.popPose();
        }

        // 6. PASO 3: Restaurar estado para texto 2D (Stacking Cartesiano sin Colisiones)
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float availableWidth = (float) (MID_RADIUS * sliceAngle - Math.toRadians(3.5) * MID_RADIUS);

        for (int i = 0; i < count; i++) {
            RadialOption opt = options.get(i);
            boolean isSel = (i == selectedIndex);
            double midAngle = -Math.PI / 2.0 + (i * sliceAngle);

            float wedgeCx = (float) (cx + Math.cos(midAngle) * MID_RADIUS);
            float wedgeCy = (float) (cy + Math.sin(midAngle) * MID_RADIUS);

            int textColor = !opt.enabled ? 0x66888888 : (isSel ? 0xFFFFFFFF : (opt.color & 0x00FFFFFF | 0xCC000000));
            int strW = this.font.width(opt.title);

            // Auto-escala dinámica para que títulos largos nunca se salgan de su cuña
            float textScale = 1.0f;
            if (strW > availableWidth && availableWidth > 15.0f) {
                textScale = Math.max(0.70f, availableWidth / (float) strW);
            }

            if (textScale < 1.0f) {
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(wedgeCx, wedgeCy + 7.0f, 0);
                pose.scale(textScale, textScale, 1.0f);
                guiGraphics.drawString(this.font, opt.title, (int) (-strW / 2.0f), -4, textColor, false);
                pose.popPose();
            } else {
                guiGraphics.drawString(this.font, opt.title, (int) (wedgeCx - strW / 2.0f), (int) (wedgeCy + 3.0f), textColor, false);
            }
        }

        // Telemetría en el Hub Central
        if (selectedIndex >= 0 && selectedIndex < count) {
            RadialOption sel = options.get(selectedIndex);
            int subW = this.font.width(sel.subtitle);
            int titleW = this.font.width(sel.title);

            guiGraphics.drawString(this.font, sel.title, (int) (cx - titleW / 2.0f), (int) (cy - 12), sel.color, true);
            guiGraphics.drawString(this.font, sel.subtitle, (int) (cx - subW / 2.0f), (int) (cy + 2), 0xFFA0B0B8, false);

            guiGraphics.hLine((int) (cx - Math.max(titleW, subW) / 2 - 4), (int) (cx + Math.max(titleW, subW) / 2 + 4), (int) (cy + 13), sel.color);
        } else {
            guiGraphics.drawString(this.font, "VG-OS // v2.0-ALPHA", (int) (cx - this.font.width("VG-OS // v2.0-ALPHA") / 2.0f), (int) (cy - 10), 0xFF00EEEE, true);
            String readyText = Component.translatable("gui.vision_goggles.radial.ready").getString();
            guiGraphics.drawString(this.font, readyText, (int) (cx - this.font.width(readyText) / 2.0f), (int) (cy + 2), 0xFF668899, false);
        }
    }

    // =========================================================================
    // UTILIDADES DE RENDERIZADO TÁCTICO PURO BLAZE3D / JOML
    // =========================================================================

    private static void drawTacticalSocket(BufferBuilder builder, Matrix4f matrix, float x, float y, boolean isSel, int accentColor, boolean enabled) {
        float half = isSel ? 9.5f : 9.0f;
        float x1 = x - half;
        float y1 = y - half;
        float x2 = x + half;
        float y2 = y + half;

        // Placa de cristal blindado / backplate
        int backColor = !enabled ? 0x66080C10 : (isSel ? 0xDD0D1E24 : 0xAA070E14);
        drawRect(builder, matrix, x1, y1, x2, y2, backColor);

        if (isSel && enabled) {
            int glowWash = (accentColor & 0x00FFFFFF) | 0x25000000;
            drawRect(builder, matrix, x1 + 1, y1 + 1, x2 - 1, y2 - 1, glowWash);
        }

        // Bisel perimetral
        int frameColor = !enabled ? 0x22444444 : (isSel ? (accentColor & 0x00FFFFFF | 0x77000000) : 0x33224438);
        drawRect(builder, matrix, x1, y1, x2, y1 + 1.0f, frameColor);
        drawRect(builder, matrix, x1, y2 - 1.0f, x2, y2, frameColor);
        drawRect(builder, matrix, x1, y1 + 1.0f, x1 + 1.0f, y2 - 1.0f, frameColor);
        drawRect(builder, matrix, x2 - 1.0f, y1 + 1.0f, x2, y2 - 1.0f, frameColor);

        // Brackets tácticos de esquina estilo HUD militar
        int bracketColor = !enabled ? 0x44555555 : (isSel ? accentColor : 0x8800EEEE);
        float arm = isSel ? 3.5f : 3.0f;
        float th = 1.0f;

        // Esquina superior izquierda
        drawRect(builder, matrix, x1, y1, x1 + arm, y1 + th, bracketColor);
        drawRect(builder, matrix, x1, y1 + th, x1 + th, y1 + arm, bracketColor);

        // Esquina superior derecha
        drawRect(builder, matrix, x2 - arm, y1, x2, y1 + th, bracketColor);
        drawRect(builder, matrix, x2 - th, y1 + th, x2, y1 + arm, bracketColor);

        // Esquina inferior izquierda
        drawRect(builder, matrix, x1, y2 - th, x1 + arm, y2, bracketColor);
        drawRect(builder, matrix, x1, y2 - arm, x1 + th, y2 - th, bracketColor);

        // Esquina inferior derecha
        drawRect(builder, matrix, x2 - arm, y2 - th, x2, y2, bracketColor);
        drawRect(builder, matrix, x2 - th, y2 - arm, x2, y2 - th, bracketColor);
    }

    private static void drawDonutWedge(BufferBuilder builder, Matrix4f matrix, float cx, float cy,
                                       float inR, float outR, double startAngle, double endAngle,
                                       int inColor, int outColor, int segments) {
        int segs = Math.max(4, segments);
        double span = endAngle - startAngle;
        double step = span / segs;

        for (int s = 0; s < segs; s++) {
            double a1 = startAngle + s * step;
            double a2 = startAngle + (s + 1) * step;

            float cos1 = (float) Math.cos(a1);
            float sin1 = (float) Math.sin(a1);
            float cos2 = (float) Math.cos(a2);
            float sin2 = (float) Math.sin(a2);

            float inX1 = cx + inR * cos1;
            float inY1 = cy + inR * sin1;
            float outX1 = cx + outR * cos1;
            float outY1 = cy + outR * sin1;

            float inX2 = cx + inR * cos2;
            float inY2 = cy + inR * sin2;
            float outX2 = cx + outR * cos2;
            float outY2 = cy + outR * sin2;

            // Triángulo 1
            addV(builder, matrix, inX1, inY1, 0, inColor);
            addV(builder, matrix, outX1, outY1, 0, outColor);
            addV(builder, matrix, outX2, outY2, 0, outColor);

            // Triángulo 2
            addV(builder, matrix, inX1, inY1, 0, inColor);
            addV(builder, matrix, outX2, outY2, 0, outColor);
            addV(builder, matrix, inX2, inY2, 0, inColor);
        }
    }

    private static void drawFilledCircle(BufferBuilder builder, Matrix4f matrix, float cx, float cy, float radius, int color, int segments) {
        int segs = Math.max(8, segments);
        double step = (Math.PI * 2) / segs;
        for (int i = 0; i < segs; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;

            float x1 = cx + (float) Math.cos(a1) * radius;
            float y1 = cy + (float) Math.sin(a1) * radius;
            float x2 = cx + (float) Math.cos(a2) * radius;
            float y2 = cy + (float) Math.sin(a2) * radius;

            addV(builder, matrix, cx, cy, 0, color);
            addV(builder, matrix, x1, y1, 0, color);
            addV(builder, matrix, x2, y2, 0, color);
        }
    }

    private static void drawCompassRingAndTicks(BufferBuilder builder, Matrix4f matrix, float cx, float cy, float radius) {
        drawDonutWedge(builder, matrix, cx, cy, radius, radius + 1.0f, 0, Math.PI * 2, 0x4400EEEE, 0x4400EEEE, 48);

        for (int deg = 0; deg < 360; deg += 10) {
            double rad = Math.toRadians(deg);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            boolean isMajor = (deg % 30 == 0);
            boolean isCardinal = (deg % 90 == 0);

            float tickLen = isCardinal ? 8.0f : (isMajor ? 5.0f : 2.5f);
            int tickColor = isCardinal ? 0xFF00EEEE : (isMajor ? 0xAA00EEEE : 0x4400EEEE);
            float tickThickness = isCardinal ? 1.5f : 1.0f;

            float r1 = radius + 1.0f;
            float r2 = r1 + tickLen;

            float perpX = -sin * (tickThickness / 2.0f);
            float perpY = cos * (tickThickness / 2.0f);

            float x1 = cx + cos * r1;
            float y1 = cy + sin * r1;
            float x2 = cx + cos * r2;
            float y2 = cy + sin * r2;

            addV(builder, matrix, x1 + perpX, y1 + perpY, 0, tickColor);
            addV(builder, matrix, x2 + perpX, y2 + perpY, 0, tickColor);
            addV(builder, matrix, x2 - perpX, y2 - perpY, 0, tickColor);

            addV(builder, matrix, x1 + perpX, y1 + perpY, 0, tickColor);
            addV(builder, matrix, x2 - perpX, y2 - perpY, 0, tickColor);
            addV(builder, matrix, x1 - perpX, y1 - perpY, 0, tickColor);
        }
    }

    private static void drawCenterReticle(BufferBuilder builder, Matrix4f matrix, float cx, float cy, int accentColor) {
        float armStart = 3.5f;
        float armEnd = 8.5f;
        float thickness = 1.0f;
        int reticleColor = (accentColor & 0x00FFFFFF) | 0xAA000000;

        drawRect(builder, matrix, cx - armEnd, cy - thickness / 2.0f, cx - armStart, cy + thickness / 2.0f, reticleColor);
        drawRect(builder, matrix, cx + armStart, cy - thickness / 2.0f, cx + armEnd, cy + thickness / 2.0f, reticleColor);
        drawRect(builder, matrix, cx - thickness / 2.0f, cy - armEnd, cx + thickness / 2.0f, cy - armStart, reticleColor);
        drawRect(builder, matrix, cx - thickness / 2.0f, cy + armStart, cx + thickness / 2.0f, cy + armEnd, reticleColor);

        drawRect(builder, matrix, cx - 0.75f, cy - 0.75f, cx + 0.75f, cy + 0.75f, accentColor);
    }

    private static void drawRect(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2, int color) {
        addV(builder, matrix, x1, y1, 0, color);
        addV(builder, matrix, x1, y2, 0, color);
        addV(builder, matrix, x2, y2, 0, color);

        addV(builder, matrix, x1, y1, 0, color);
        addV(builder, matrix, x2, y2, 0, color);
        addV(builder, matrix, x2, y1, 0, color);
    }

    private static void addV(VertexConsumer builder, Matrix4f matrix, float x, float y, float z, int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        builder.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
    }
}
