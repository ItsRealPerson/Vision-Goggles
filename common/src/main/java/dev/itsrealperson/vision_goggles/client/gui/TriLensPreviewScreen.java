package dev.itsrealperson.vision_goggles.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensGeoRenderer;
import dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensPreviewState;
import dev.itsrealperson.vision_goggles.client.preview.ProtoTriLensVariant;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

/**
 * Isolated 3D turntable inspector for the Tactical Tri-Lens prototype variants.
 * Uses authentic Minecraft 3D-beveled gunmetal framing and {@link Component#literal(String)}
 * labels to keep 16-locale production translation files untouched.
 */
@Environment(EnvType.CLIENT)
public class TriLensPreviewScreen extends Screen {

    private static final float DEFAULT_YAW = 205.0f;
    private static final float DEFAULT_PITCH = 18.0f;
    private static final float DEFAULT_ZOOM = 1.0f;

    private float cameraYaw = DEFAULT_YAW;
    private float cameraPitch = DEFAULT_PITCH;
    private float cameraZoom = DEFAULT_ZOOM;
    private boolean draggingViewport = false;
    private long lastSpinNanos = 0L;

    private int panelX, panelY, panelW, panelH;
    private int vpX, vpY, vpW, vpH;
    private boolean compactVertical;
    private boolean compactToggles;

    private final Map<ProtoTriLensVariant, BevelButton> variantButtons = new EnumMap<>(ProtoTriLensVariant.class);
    private BevelButton btnVisor;
    private BevelButton btnGlow;
    private BevelButton btnHeadGuide;
    private BevelButton btnAutoSpin;
    private BevelButton btnHeadPreview;

    public TriLensPreviewScreen() {
        super(Component.literal("Tri-Lens 3D Prototype Inspector"));
    }

    @Override
    protected void init() {
        super.init();
        this.variantButtons.clear();

        this.panelW = Math.min(this.width - 12, 440);
        this.panelH = Math.min(this.height - 12, 248);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        this.compactVertical = this.panelH < 200;
        this.compactToggles = this.compactVertical;

        int controlW = this.panelW < 300 ? 124 : 136;
        this.vpX = this.panelX + 6;
        this.vpY = this.panelY + (this.compactVertical ? 23 : 26);
        this.vpW = this.panelW - controlW - 18;
        this.vpH = this.panelH - (this.compactVertical ? 28 : 34);

        int colX = this.vpX + this.vpW + 6;
        int cy = this.vpY;
        int halfW = (controlW - 2) / 2;
        int btnH = this.compactVertical ? (this.panelH < 165 ? 14 : 15) : 16;
        int gap = 2;
        int step = btnH + gap;
        int sectionGap = this.compactVertical ? 4 : 6;
        int bottomPad = this.compactVertical ? 5 : 6;

        // Variant buttons (2x2 + MODULAR wide)
        ProtoTriLensVariant[] variants = ProtoTriLensVariant.values();
        for (int i = 0; i < 4; i++) {
            ProtoTriLensVariant v = variants[i];
            int bx = colX + (i % 2) * (halfW + 2);
            int by = cy + (i / 2) * step;
            BevelButton b = this.addRenderableWidget(new BevelButton(
                    bx, by, halfW, btnH,
                    Component.literal(v.getShortLabel()),
                    btn -> selectVariant(v),
                    false
            ));
            this.variantButtons.put(v, b);
        }
        ProtoTriLensVariant modular = ProtoTriLensVariant.MODULAR;
        BevelButton bMod = this.addRenderableWidget(new BevelButton(
                colX, cy + 2 * step, controlW, btnH,
                Component.literal(modular.getShortLabel()),
                btn -> selectVariant(modular),
                false
        ));
        this.variantButtons.put(modular, bMod);

        int variantsBottom = cy + 2 * step + btnH;
        int toggleY = variantsBottom + sectionGap;

        if (this.compactToggles) {
            // Compact 2-column grid (2x2 + full-width F5 Head) for small scaled heights (panelH < 200)
            this.btnVisor = this.addRenderableWidget(new BevelButton(
                    colX, toggleY, halfW, btnH,
                    Component.empty(),
                    btn -> {
                        ProtoTriLensPreviewState.toggleVisorUp();
                        refreshButtonLabels();
                    },
                    true
            ));
            this.btnGlow = this.addRenderableWidget(new BevelButton(
                    colX + halfW + 2, toggleY, halfW, btnH,
                    Component.empty(),
                    btn -> {
                        ProtoTriLensPreviewState.toggleGlowEnabled();
                        refreshButtonLabels();
                    },
                    true
            ));
            this.btnHeadGuide = this.addRenderableWidget(new BevelButton(
                    colX, toggleY + step, halfW, btnH,
                    Component.empty(),
                    btn -> {
                        ProtoTriLensPreviewState.toggleShowHeadGuide();
                        refreshButtonLabels();
                    },
                    true
            ));
            this.btnAutoSpin = this.addRenderableWidget(new BevelButton(
                    colX + halfW + 2, toggleY + step, halfW, btnH,
                    Component.empty(),
                    btn -> {
                        ProtoTriLensPreviewState.toggleAutoSpin();
                        refreshButtonLabels();
                    },
                    true
            ));
            this.btnHeadPreview = this.addRenderableWidget(new BevelButton(
                    colX, toggleY + step * 2, controlW, btnH,
                    Component.empty(),
                    btn -> {
                        ProtoTriLensPreviewState.toggleHeadPreviewEnabled();
                        refreshButtonLabels();
                    },
                    true
            ));
        } else {
            this.btnVisor = this.addRenderableWidget(new BevelButton(
                    colX, toggleY, controlW, btnH,
                    Component.empty(),
                    btn -> {
                        ProtoTriLensPreviewState.toggleVisorUp();
                        refreshButtonLabels();
                    },
                    true
            ));
            this.btnGlow = this.addRenderableWidget(new BevelButton(
                    colX, toggleY + step, controlW, btnH,
                    Component.empty(),
                    btn -> {
                        ProtoTriLensPreviewState.toggleGlowEnabled();
                        refreshButtonLabels();
                    },
                    true
            ));
            this.btnHeadGuide = this.addRenderableWidget(new BevelButton(
                    colX, toggleY + step * 2, controlW, btnH,
                    Component.empty(),
                    btn -> {
                        ProtoTriLensPreviewState.toggleShowHeadGuide();
                        refreshButtonLabels();
                    },
                    true
            ));
            this.btnAutoSpin = this.addRenderableWidget(new BevelButton(
                    colX, toggleY + step * 3, controlW, btnH,
                    Component.empty(),
                    btn -> {
                        ProtoTriLensPreviewState.toggleAutoSpin();
                        refreshButtonLabels();
                    },
                    true
            ));
            this.btnHeadPreview = this.addRenderableWidget(new BevelButton(
                    colX, toggleY + step * 4, controlW, btnH,
                    Component.empty(),
                    btn -> {
                        ProtoTriLensPreviewState.toggleHeadPreviewEnabled();
                        refreshButtonLabels();
                    },
                    true
            ));
        }

        int footerY = this.panelY + this.panelH - bottomPad - btnH;
        this.addRenderableWidget(new BevelButton(
                colX, footerY, halfW, btnH,
                Component.literal("Reset Cam"),
                btn -> resetCamera(),
                false
        ));
        this.addRenderableWidget(new BevelButton(
                colX + halfW + 2, footerY, halfW, btnH,
                Component.literal("Close"),
                btn -> this.onClose(),
                false
        ));

        refreshButtonLabels();
    }

    private void selectVariant(ProtoTriLensVariant variant) {
        ProtoTriLensPreviewState.setSelectedVariant(variant);
        refreshButtonLabels();
    }

    private void resetCamera() {
        this.cameraYaw = DEFAULT_YAW;
        this.cameraPitch = DEFAULT_PITCH;
        this.cameraZoom = DEFAULT_ZOOM;
    }

    private void refreshButtonLabels() {
        ProtoTriLensVariant selected = ProtoTriLensPreviewState.getSelectedVariant();
        for (Map.Entry<ProtoTriLensVariant, BevelButton> entry : this.variantButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == selected);
        }
        if (this.btnVisor != null) {
            boolean up = ProtoTriLensPreviewState.isVisorUp();
            String label = this.compactToggles ? ("Visor: " + (up ? "UP" : "DN")) : ("Visor: " + (up ? "UP" : "DOWN"));
            this.btnVisor.setMessage(Component.literal(label));
            this.btnVisor.setSelected(up);
        }
        if (this.btnGlow != null) {
            boolean glow = ProtoTriLensPreviewState.isGlowEnabled();
            this.btnGlow.setMessage(Component.literal("Glow: " + (glow ? "ON" : "OFF")));
            this.btnGlow.setSelected(glow);
        }
        if (this.btnHeadGuide != null) {
            boolean guide = ProtoTriLensPreviewState.isShowHeadGuide();
            String prefix = this.compactToggles ? "Guide: " : "Head Guide: ";
            this.btnHeadGuide.setMessage(Component.literal(prefix + (guide ? "ON" : "OFF")));
            this.btnHeadGuide.setSelected(guide);
        }
        if (this.btnAutoSpin != null) {
            boolean spin = ProtoTriLensPreviewState.isAutoSpin();
            String prefix = this.compactToggles ? "Spin: " : "Auto-Spin: ";
            this.btnAutoSpin.setMessage(Component.literal(prefix + (spin ? "ON" : "OFF")));
            this.btnAutoSpin.setSelected(spin);
        }
        if (this.btnHeadPreview != null) {
            boolean f5 = ProtoTriLensPreviewState.isHeadPreviewEnabled();
            this.btnHeadPreview.setMessage(Component.literal("F5 Head: " + (f5 ? "ON" : "OFF")));
            this.btnHeadPreview.setSelected(f5);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideViewport(mouseX, mouseY)) {
            this.draggingViewport = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingViewport = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingViewport && button == 0) {
            this.cameraYaw = (this.cameraYaw + (float) dragX * 0.85f) % 360.0f;
            if (this.cameraYaw < 0.0f) this.cameraYaw += 360.0f;
            this.cameraPitch = Mth.clamp(this.cameraPitch + (float) dragY * 0.65f, -80.0f, 80.0f);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isInsideViewport(mouseX, mouseY)) {
            this.cameraZoom = Mth.clamp(this.cameraZoom + (float) delta * 0.12f, 0.45f, 2.50f);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean isInsideViewport(double mouseX, double mouseY) {
        return mouseX >= this.vpX && mouseX < this.vpX + this.vpW
                && mouseY >= this.vpY && mouseY < this.vpY + this.vpH;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        if (this.lastSpinNanos == 0L) {
            this.lastSpinNanos = now;
        } else {
            float dt = Math.min((now - this.lastSpinNanos) / 1_000_000_000.0f, 0.05f);
            this.lastSpinNanos = now;
            if (ProtoTriLensPreviewState.isAutoSpin() && !this.draggingViewport) {
                this.cameraYaw = (this.cameraYaw + dt * 42.0f) % 360.0f;
            }
        }

        this.renderBackground(guiGraphics);

        // 1. Industrial Gunmetal 3D-Beveled Frame (matching ModificationStationScreen)
        drawBevelPanel(guiGraphics, panelX, panelY, panelW, panelH, 0xFF2B2F36, 0xFF545B66, 0xFF191C20, 0xFF0E1012);
        // Corner rivets
        guiGraphics.fill(panelX + 3, panelY + 3, panelX + 5, panelY + 5, 0xFF424852);
        guiGraphics.fill(panelX + panelW - 5, panelY + 3, panelX + panelW - 3, panelY + 5, 0xFF424852);
        guiGraphics.fill(panelX + 3, panelY + panelH - 5, panelX + 5, panelY + panelH - 3, 0xFF424852);
        guiGraphics.fill(panelX + panelW - 5, panelY + panelH - 5, panelX + panelW - 3, panelY + panelH - 3, 0xFF424852);

        // 2. Top Header Strip (adapts cleanly for panelW < 320 without title/variant collision)
        int headerH = this.compactVertical ? 15 : 17;
        int headerY = panelY + 5;
        drawBevelPanel(guiGraphics, panelX + 6, headerY, panelW - 12, headerH, 0xFF22262D, 0xFF3F454F, 0xFF15181C, 0xFF0E1012);
        int headerTextY = headerY + (headerH - 8) / 2;
        int availHeaderW = panelW - 22;
        ProtoTriLensVariant variant = ProtoTriLensPreviewState.getSelectedVariant();
        String title = panelW < 340 ? "Tri-Lens" : "Tri-Lens 3D Prototype Inspector";
        String variantLabel = variant.getDisplayName();
        int titleW = this.font.width(title);
        int labelW = this.font.width(variantLabel);
        if (titleW + 8 + labelW > availHeaderW) {
            title = "Tri-Lens";
            titleW = this.font.width(title);
        }
        if (titleW + 8 + labelW > availHeaderW) {
            variantLabel = variant.getShortLabel();
            labelW = this.font.width(variantLabel);
        }
        if (titleW + 6 + labelW <= availHeaderW) {
            guiGraphics.drawString(this.font, Component.literal(title), panelX + 11, headerTextY, 0xFFE6ECEF, true);
            guiGraphics.drawString(this.font, Component.literal(variantLabel), panelX + panelW - 11 - labelW, headerTextY, variant.getAccentColor(), true);
        } else {
            String clipped = this.font.plainSubstrByWidth(variantLabel, Math.max(8, availHeaderW));
            guiGraphics.drawString(this.font, Component.literal(clipped), panelX + 11, headerTextY, variant.getAccentColor(), true);
        }

        // 3. Recessed 3D Viewport
        drawRecessedBox(guiGraphics, vpX, vpY, vpW, vpH, 0xFF13161B);

        // Subtle horizon/turntable floor lines in 2D background
        int floorY = vpY + (int) (vpH * 0.72f);
        guiGraphics.fill(vpX + 8, floorY, vpX + vpW - 8, floorY + 1, 0xFF1F252D);

        render3DModelInViewport(guiGraphics, variant);

        // Viewport telemetry & controls hint (rendered at z = 2000.0f inside scissor so 3D model at z = 1500.0f never occludes text)
        String camInfo = String.format(java.util.Locale.ROOT, "Yaw %.0f\u00B0  Pitch %.0f\u00B0  Zoom %.2fx", cameraYaw, cameraPitch, cameraZoom);
        if (vpW < 155 || this.font.width(camInfo) > vpW - 10) {
            camInfo = String.format(java.util.Locale.ROOT, "Y%.0f P%.0f %.2fx", cameraYaw, cameraPitch, cameraZoom);
        }
        String hintInfo = "Drag: Orbit | Scroll: Zoom";
        if (vpW < 155 || this.font.width(hintInfo) > vpW - 10) {
            hintInfo = "Orbit | Scroll: Zoom";
        }
        if (this.font.width(hintInfo) > vpW - 10) {
            hintInfo = "Drag/Scroll";
        }

        guiGraphics.enableScissor(vpX + 1, vpY + 1, vpX + vpW - 1, vpY + vpH - 1);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f, 0.0f, 2000.0f);
        guiGraphics.drawString(this.font, Component.literal(camInfo), vpX + 5, vpY + 5, 0xFF8E9AA8, false);
        guiGraphics.drawString(this.font, Component.literal(hintInfo), vpX + 5, vpY + vpH - 12, 0xFF6F7A87, false);

        if (ProtoTriLensGeoRenderer.getBakedModel(variant) == null) {
            String missingText = "Missing GeoModel: " + variant.getGeoModel();
            if (this.font.width(missingText) > vpW - 10) {
                missingText = "Missing: " + variant.getId();
            }
            guiGraphics.drawCenteredString(this.font, Component.literal(missingText), vpX + vpW / 2, vpY + vpH / 2 - 4, 0xFFFF6666);
        }
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void render3DModelInViewport(GuiGraphics guiGraphics, ProtoTriLensVariant variant) {
        Minecraft mc = Minecraft.getInstance();
        guiGraphics.flush();
        guiGraphics.enableScissor(vpX + 1, vpY + 1, vpX + vpW - 1, vpY + vpH - 1);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        Lighting.setupFor3DItems();

        float baseScale = Math.min(vpW, vpH) * 0.56f * this.cameraZoom;
        PoseStack poseStack = guiGraphics.pose();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        poseStack.pushPose();
        poseStack.translate(vpX + vpW * 0.5f, vpY + vpH * 0.52f, 1500.0f);
        // Cancel GuiGraphics Y-down ortho flip so +Y is up and GL CCW winding stays intact
        poseStack.scale(baseScale, -baseScale, baseScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(this.cameraPitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(this.cameraYaw));

        ProtoTriLensGeoRenderer.renderStandalonePreview(
                poseStack,
                bufferSource,
                variant,
                LightTexture.FULL_BRIGHT,
                ProtoTriLensPreviewState.isShowHeadGuide()
        );

        poseStack.popPose();
        bufferSource.endBatch();

        Lighting.setupForFlatItems();
        RenderSystem.disableDepthTest();
        guiGraphics.disableScissor();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void drawRecessedBox(GuiGraphics guiGraphics, int x, int y, int w, int h, int fill) {
        guiGraphics.fill(x, y, x + w, y + h, fill);
        guiGraphics.fill(x, y, x + w - 1, y + 1, 0xFF0E1012);
        guiGraphics.fill(x, y, x + 1, y + h - 1, 0xFF0E1012);
        guiGraphics.fill(x + 1, y + h - 1, x + w, y + h, 0xFF444A54);
        guiGraphics.fill(x + w - 1, y + 1, x + w, y + h, 0xFF444A54);
    }

    private static void drawBevelPanel(GuiGraphics guiGraphics, int x, int y, int w, int h, int body, int light, int dark, int outline) {
        guiGraphics.fill(x, y, x + w, y + h, outline);
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, body);
        guiGraphics.fill(x + 1, y + 1, x + w - 2, y + 2, light);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + h - 2, light);
        guiGraphics.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, dark);
        guiGraphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, dark);
    }

    private final class BevelButton extends Button {
        private final boolean isToggle;
        private boolean selected;

        private BevelButton(int x, int y, int w, int h, Component message, OnPress onPress, boolean isToggle) {
            super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
            this.isToggle = isToggle;
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int bx = this.getX();
            int by = this.getY();
            int bw = this.getWidth();
            int bh = this.getHeight();
            if (this.isToggle) {
                if (this.selected) {
                    int body = this.isHovered() ? 0xFF33734B : 0xFF295D3D;
                    int light = this.isHovered() ? 0xFF5BC282 : 0xFF49A36B;
                    drawBevelPanel(guiGraphics, bx, by, bw, bh, body, light, 0xFF183825, 0xFF0E1012);
                    guiGraphics.drawCenteredString(TriLensPreviewScreen.this.font, this.getMessage(), bx + bw / 2, by + (bh - 8) / 2, 0xFFF4FFF7);
                } else {
                    int body = this.isHovered() ? 0xFF2E333C : 0xFF22262D;
                    int light = this.isHovered() ? 0xFF4B5361 : 0xFF363C47;
                    drawBevelPanel(guiGraphics, bx, by, bw, bh, body, light, 0xFF15181C, 0xFF0E1012);
                    guiGraphics.drawCenteredString(TriLensPreviewScreen.this.font, this.getMessage(), bx + bw / 2, by + (bh - 8) / 2, 0xFFB8C0C8);
                }
            } else {
                if (this.selected) {
                    drawBevelPanel(guiGraphics, bx, by, bw, bh, 0xFF434A54, 0xFF8A94A3, 0xFF23272E, 0xFF0E1012);
                } else {
                    int body = this.isHovered() ? 0xFF333942 : 0xFF262A31;
                    int light = this.isHovered() ? 0xFF5A6270 : 0xFF3F454F;
                    drawBevelPanel(guiGraphics, bx, by, bw, bh, body, light, 0xFF16181D, 0xFF0E1012);
                }
                int textCol = this.selected ? 0xFFFFFFFF : (this.isHovered() ? 0xFFE6ECEF : 0xFFAAB2BC);
                guiGraphics.drawCenteredString(TriLensPreviewScreen.this.font, this.getMessage(), bx + bw / 2, by + (bh - 8) / 2, textCol);
            }
        }
    }
}
