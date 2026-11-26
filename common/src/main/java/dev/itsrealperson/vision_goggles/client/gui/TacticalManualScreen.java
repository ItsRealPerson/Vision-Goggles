package dev.itsrealperson.vision_goggles.client.gui;

import dev.itsrealperson.vision_goggles.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * In-game Tactical Field Manual GUI rendered in the classic 3D-beveled Minecraft gunmetal style
 * matching {@link ModificationStationScreen}.
 */
public class TacticalManualScreen extends Screen {

    private static final int GUI_W = 316;
    private static final int GUI_H = 218;

    private static final int TAB_GOGGLES = 0;
    private static final int TAB_MODULES = 1;
    private static final int TAB_FLASHLIGHT = 2;
    private static final int TAB_STATION = 3;

    private record Entry(Supplier<Item> icon, String titleKey, String descKey) {}

    private static final List<Entry> GOGGLES_ENTRIES = List.of(
            new Entry(ModItems.NIGHT_VISION_GOGGLES, "gui.vision_goggles.manual.goggles.nvg.title", "gui.vision_goggles.manual.goggles.nvg.desc"),
            new Entry(ModItems.THERMAL_GOGGLES, "gui.vision_goggles.manual.goggles.thermal.title", "gui.vision_goggles.manual.goggles.thermal.desc"),
            new Entry(ModItems.BIOMETRIC_GOGGLES, "gui.vision_goggles.manual.goggles.bio.title", "gui.vision_goggles.manual.goggles.bio.desc"),
            new Entry(ModItems.HYDRO_GOGGLES, "gui.vision_goggles.manual.goggles.hydro.title", "gui.vision_goggles.manual.goggles.hydro.desc"),
            new Entry(ModItems.MODULAR_GOGGLES, "gui.vision_goggles.manual.goggles.modular.title", "gui.vision_goggles.manual.goggles.modular.desc"),
            new Entry(ModItems.PRO_MODULAR_GOGGLES, "gui.vision_goggles.manual.goggles.pro.title", "gui.vision_goggles.manual.goggles.pro.desc")
    );

    private static final List<Entry> MODULE_ENTRIES = List.of(
            new Entry(ModItems.NVG_MODULE, "gui.vision_goggles.manual.mod.nvg.title", "gui.vision_goggles.manual.mod.nvg.desc"),
            new Entry(ModItems.THERMAL_MODULE, "gui.vision_goggles.manual.mod.thermal.title", "gui.vision_goggles.manual.mod.thermal.desc"),
            new Entry(ModItems.BIO_MODULE, "gui.vision_goggles.manual.mod.bio.title", "gui.vision_goggles.manual.mod.bio.desc"),
            new Entry(ModItems.HYDRO_MODULE, "gui.vision_goggles.manual.mod.hydro.title", "gui.vision_goggles.manual.mod.hydro.desc"),
            new Entry(ModItems.ZOOM_MODULE, "gui.vision_goggles.manual.mod.zoom.title", "gui.vision_goggles.manual.mod.zoom.desc"),
            new Entry(ModItems.SONAR_MODULE, "gui.vision_goggles.manual.mod.sonar.title", "gui.vision_goggles.manual.mod.sonar.desc"),
            new Entry(ModItems.SOLAR_MODULE, "gui.vision_goggles.manual.mod.solar.title", "gui.vision_goggles.manual.mod.solar.desc"),
            new Entry(ModItems.BATTERY_EXPANSION_MODULE, "gui.vision_goggles.manual.mod.battery.title", "gui.vision_goggles.manual.mod.battery.desc"),
            new Entry(ModItems.VITAL_INFO_MODULE, "gui.vision_goggles.manual.mod.vital.title", "gui.vision_goggles.manual.mod.vital.desc"),
            new Entry(ModItems.ENVIRONMENT_MODULE, "gui.vision_goggles.manual.mod.env.title", "gui.vision_goggles.manual.mod.env.desc"),
            new Entry(ModItems.FLASHLIGHT_MODULE, "gui.vision_goggles.manual.mod.flashlight.title", "gui.vision_goggles.manual.mod.flashlight.desc"),
            new Entry(ModItems.PROGRAMMABLE_MODULE, "gui.vision_goggles.manual.mod.lua.title", "gui.vision_goggles.manual.mod.lua.desc")
    );

    private static final List<Entry> FLASHLIGHT_ENTRIES = List.of(
            new Entry(ModItems.TACTICAL_FLASHLIGHT, "gui.vision_goggles.manual.flash.item.title", "gui.vision_goggles.manual.flash.item.desc"),
            new Entry(ModItems.FLASHLIGHT_MODULE, "gui.vision_goggles.manual.flash.focused.title", "gui.vision_goggles.manual.flash.focused.desc"),
            new Entry(ModItems.OPTICAL_LENS, "gui.vision_goggles.manual.flash.wide_close.title", "gui.vision_goggles.manual.flash.wide_close.desc"),
            new Entry(ModItems.TACTICAL_PROCESSOR, "gui.vision_goggles.manual.flash.strobe.title", "gui.vision_goggles.manual.flash.strobe.desc")
    );

    private static final List<Entry> STATION_ENTRIES = List.of(
            new Entry(ModItems.MODIFICATION_STATION, "gui.vision_goggles.manual.station.assemble.title", "gui.vision_goggles.manual.station.assemble.desc"),
            new Entry(ModItems.ELECTRICAL_PARTS, "gui.vision_goggles.manual.station.repair.title", "gui.vision_goggles.manual.station.repair.desc"),
            new Entry(ModItems.INSTALLATION_SCRIPT, "gui.vision_goggles.manual.station.lua.title", "gui.vision_goggles.manual.station.lua.desc"),
            new Entry(ModItems.NVG_BATTERY, "gui.vision_goggles.manual.station.controls.title", "gui.vision_goggles.manual.station.controls.desc")
    );

    private int activeTab = TAB_GOGGLES;
    private int modulePage = 0;
    private int leftPos;
    private int topPos;

    private ManualButton btnPrev;
    private ManualButton btnNext;

    public TacticalManualScreen() {
        super(Component.translatable("item.vision_goggles.tactical_manual"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new TacticalManualScreen());
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - GUI_W) / 2;
        this.topPos = (this.height - GUI_H) / 2;

        int x = this.leftPos;
        int y = this.topPos;
        int tabW = 73;
        this.addRenderableWidget(new ManualButton(x + 8, y + 20, tabW, 14,
                Component.translatable("gui.vision_goggles.manual.tab.goggles"), b -> selectTab(TAB_GOGGLES)));
        this.addRenderableWidget(new ManualButton(x + 83, y + 20, tabW, 14,
                Component.translatable("gui.vision_goggles.manual.tab.modules"), b -> selectTab(TAB_MODULES)));
        this.addRenderableWidget(new ManualButton(x + 158, y + 20, tabW, 14,
                Component.translatable("gui.vision_goggles.manual.tab.flashlight"), b -> selectTab(TAB_FLASHLIGHT)));
        this.addRenderableWidget(new ManualButton(x + 233, y + 20, tabW, 14,
                Component.translatable("gui.vision_goggles.manual.tab.station"), b -> selectTab(TAB_STATION)));

        this.btnPrev = this.addRenderableWidget(new ManualButton(x + 8, y + GUI_H - 22, 54, 14,
                Component.literal("< Prev"), b -> { if (modulePage > 0) modulePage--; updateNavState(); }));
        this.btnNext = this.addRenderableWidget(new ManualButton(x + 66, y + GUI_H - 22, 54, 14,
                Component.literal("Next >"), b -> { if (modulePage < 1) modulePage++; updateNavState(); }));

        this.addRenderableWidget(new ManualButton(x + GUI_W - 62, y + GUI_H - 22, 54, 14,
                Component.translatable("gui.done"), b -> this.onClose()));

        updateNavState();
    }

    private void selectTab(int tab) {
        this.activeTab = tab;
        if (tab != TAB_MODULES) {
            this.modulePage = 0;
        }
        updateNavState();
    }

    private void updateNavState() {
        boolean showPager = (activeTab == TAB_MODULES);
        btnPrev.visible = showPager;
        btnNext.visible = showPager;
        btnPrev.active = showPager && modulePage > 0;
        btnNext.active = showPager && modulePage < 1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int x = this.leftPos;
        int y = this.topPos;

        // Main 3D-beveled gunmetal panel
        drawBeveledPanel(g, x, y, GUI_W, GUI_H, 0xFF2B2F36, 0xFF545B66, 0xFF14171B);

        // Header title
        g.drawString(this.font, Component.translatable("item.vision_goggles.tactical_manual"), x + 10, y + 7, 0xFFE6EDF3, false);

        // Active tab indicator bar
        int tabX = x + 8 + activeTab * 75;
        g.fill(tabX, y + 34, tabX + 73, y + 36, 0xFF39C5BB);

        // Recessed content area
        int boxX = x + 8;
        int boxY = y + 38;
        int boxW = GUI_W - 16;
        int boxH = GUI_H - 64;
        drawBeveledPanel(g, boxX, boxY, boxW, boxH, 0xFF1B1E23, 0xFF111317, 0xFF3A414C);

        List<Entry> entries = switch (activeTab) {
            case TAB_MODULES -> modulePage == 0 ? MODULE_ENTRIES.subList(0, 6) : MODULE_ENTRIES.subList(6, 12);
            case TAB_FLASHLIGHT -> FLASHLIGHT_ENTRIES;
            case TAB_STATION -> STATION_ENTRIES;
            default -> GOGGLES_ENTRIES;
        };

        int rowCount = entries.size();
        int rowH = (boxH - 8) / Math.max(1, rowCount);
        for (int i = 0; i < rowCount; i++) {
            Entry e = entries.get(i);
            int ry = boxY + 4 + i * rowH;
            drawBeveledPanel(g, boxX + 4, ry, boxW - 8, rowH - 2, 0xFF23272E, 0xFF363D47, 0xFF15181D);

            // Recessed 18x18 icon slot
            int slotX = boxX + 8;
            int slotY = ry + Math.max(2, (rowH - 20) / 2);
            drawBeveledPanel(g, slotX, slotY, 18, 18, 0xFF16191E, 0xFF0E1013, 0xFF3A414C);
            g.renderItem(new ItemStack(e.icon().get()), slotX + 1, slotY + 1);

            int textX = slotX + 23;
            int textW = boxW - 40;
            g.drawString(this.font, Component.translatable(e.titleKey()), textX, ry + 3, 0xFF7BE0D6, false);

            List<FormattedCharSequence> lines = this.font.split(Component.translatable(e.descKey()), textW);
            int maxLines = Math.max(1, (rowH - 14) / 9);
            for (int l = 0; l < Math.min(maxLines, lines.size()); l++) {
                g.drawString(this.font, lines.get(l), textX, ry + 13 + l * 9, 0xFFC5CED9, false);
            }
        }

        if (activeTab == TAB_MODULES) {
            String pageStr = (modulePage + 1) + " / 2";
            g.drawString(this.font, pageStr, x + 128, y + GUI_H - 18, 0xFF9AA5B4, false);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static void drawBeveledPanel(GuiGraphics g, int x, int y, int w, int h, int fill, int hi, int sh) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, hi);
        g.fill(x, y, x + 1, y + h, hi);
        g.fill(x, y + h - 1, x + w, y + h, sh);
        g.fill(x + w - 1, y, x + w, y + h, sh);
    }

    private static class ManualButton extends Button {
        ManualButton(int x, int y, int w, int h, Component msg, OnPress onPress) {
            super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int bg = !this.active ? 0xFF1E2228 : (this.isHoveredOrFocused() ? 0xFF3E4652 : 0xFF313640);
            int hi = this.isHoveredOrFocused() && this.active ? 0xFF6A7585 : 0xFF4B525E;
            int sh = 0xFF14171B;
            drawBeveledPanel(g, getX(), getY(), getWidth(), getHeight(), bg, hi, sh);
            int col = this.active ? 0xFFE6EDF3 : 0xFF6E7681;
            g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, col);
        }
    }
}
