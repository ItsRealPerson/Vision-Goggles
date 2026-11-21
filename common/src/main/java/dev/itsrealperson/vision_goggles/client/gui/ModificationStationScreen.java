package dev.itsrealperson.vision_goggles.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.itsrealperson.vision_goggles.client.StationWorkAnimator;
import dev.itsrealperson.vision_goggles.menu.ModificationStationMenu;
import dev.itsrealperson.vision_goggles.network.AssembleBlueprintPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.recipe.StationBlueprints;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ModificationStationScreen extends AbstractContainerScreen<ModificationStationMenu> {

    private static final int GUI_W = 220;
    private static final int GUI_H = 196;

    // Blueprint 2x4 selector grid (Tab 0 left panel)
    private static final int GRID_X = 8;
    private static final int GRID_Y = 26;
    private static final int CELL_W = 42;
    private static final int CELL_H = 19;
    private static final int CELL_GAP = 2;

    // Right monitor panel (Tab 0)
    private static final int MON_X = 98;
    private static final int MON_Y = 26;
    private static final int MON_W = 114;
    private static final int MON_H = 82;

    private static final String[] SHORT_CODES = {
            "NVG", "THRM", "HYDR", "BIO", "MOD", "PRO", "FLSH"
    };

    private int selectedIndex = 0;
    private Button btnAssembleTab;
    private Button btnModifyTab;
    private Button btnAssembleAction;

    public ModificationStationScreen(ModificationStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelX = 30;
        this.inventoryLabelY = 103;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        this.btnAssembleTab = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.vision_goggles.tab.assemble"), b -> switchTab(ModificationStationMenu.TAB_ASSEMBLE))
                        .bounds(x + 8, y + 6, 88, 16)
                        .build()
        );
        this.btnModifyTab = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.vision_goggles.tab.modify"), b -> switchTab(ModificationStationMenu.TAB_MODIFY))
                        .bounds(x + 100, y + 6, 112, 16)
                        .build()
        );
        this.btnAssembleAction = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.vision_goggles.button.assemble"), b -> triggerAssemble())
                        .bounds(x + MON_X + 4, y + MON_Y + 62, MON_W - 8, 16)
                        .build()
        );
        updateButtonStates();
    }

    private void switchTab(int tab) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, tab);
        }
    }

    private void triggerAssemble() {
        List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
        if (selectedIndex < 0 || selectedIndex >= all.size()) return;
        StationBlueprints.Blueprint bp = all.get(selectedIndex);
        if (this.minecraft == null || this.minecraft.player == null) return;
        if (!StationBlueprints.canAssemble(this.minecraft.player, this.menu.getStationContainer(), bp)) return;

        NetworkManager.INSTANCE.sendToServer(new AssembleBlueprintPacket(this.menu.getStationPos(), bp.id()));
        StationWorkAnimator.startLocalWork(this.menu.getStationPos());
        this.onClose();
    }

    private void updateButtonStates() {
        boolean isAssemble = (this.menu.getCurrentTab() == ModificationStationMenu.TAB_ASSEMBLE);
        if (this.btnAssembleTab != null) this.btnAssembleTab.active = !isAssemble;
        if (this.btnModifyTab != null) this.btnModifyTab.active = isAssemble;
        if (this.btnAssembleAction != null) {
            this.btnAssembleAction.visible = isAssemble;
            if (isAssemble && this.minecraft != null && this.minecraft.player != null) {
                List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
                StationBlueprints.Blueprint bp = (selectedIndex >= 0 && selectedIndex < all.size()) ? all.get(selectedIndex) : null;
                this.btnAssembleAction.active = bp != null
                        && this.menu.getAssemblingTicks() == 0
                        && StationBlueprints.canAssemble(this.minecraft.player, this.menu.getStationContainer(), bp);
            } else {
                this.btnAssembleAction.active = false;
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonStates();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.getCurrentTab() == ModificationStationMenu.TAB_ASSEMBLE) {
            List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
            for (int i = 0; i < all.size(); i++) {
                int col = i % 2;
                int row = i / 2;
                int cx = this.leftPos + GRID_X + col * (CELL_W + CELL_GAP);
                int cy = this.topPos + GRID_Y + row * (CELL_H + CELL_GAP);
                if (mouseX >= cx && mouseX < cx + CELL_W && mouseY >= cy && mouseY < cy + CELL_H) {
                    this.selectedIndex = i;
                    updateButtonStates();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        updateButtonStates();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderExtraTooltips(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF9AA3AE, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;

        // Tactical Charcoal Chassis
        guiGraphics.fill(x, y, x + GUI_W, y + GUI_H, 0xFF1A1D22);
        drawBorder(guiGraphics, x, y, GUI_W, GUI_H, 0xFF3E444E);
        guiGraphics.fill(x + 2, y + 2, x + GUI_W - 2, y + 24, 0xFF121519);

        if (this.menu.getCurrentTab() == ModificationStationMenu.TAB_ASSEMBLE) {
            renderAssembleTab(guiGraphics, x, y);
        } else {
            renderModifyTab(guiGraphics, x, y);
        }

        // Player Inventory & Hotbar Slot Frames
        int invX = x + 29;
        int invY = y + 113;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(guiGraphics, invX + col * 18, invY + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBox(guiGraphics, invX + col * 18, invY + 58);
        }
    }

    private void renderAssembleTab(GuiGraphics guiGraphics, int x, int y) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();

        // Left Blueprint Cards (2 columns x 4 rows)
        for (int i = 0; i < all.size(); i++) {
            StationBlueprints.Blueprint bp = all.get(i);
            int col = i % 2;
            int row = i / 2;
            int cx = x + GRID_X + col * (CELL_W + CELL_GAP);
            int cy = y + GRID_Y + row * (CELL_H + CELL_GAP);

            boolean selected = (i == selectedIndex);
            boolean craftable = StationBlueprints.canAssemble(this.minecraft.player, this.menu.getStationContainer(), bp);
            int bg = selected ? 0xFF213128 : (craftable ? 0xFF1E2429 : 0xFF14171B);
            int border = selected ? 0xFF3CFF82 : (craftable ? 0xFF2E6B47 : 0xFF2D323A);

            guiGraphics.fill(cx, cy, cx + CELL_W, cy + CELL_H, bg);
            drawBorder(guiGraphics, cx, cy, CELL_W, CELL_H, border);

            ItemStack icon = bp.createDefaultResult();
            guiGraphics.renderItem(icon, cx + 2, cy + 1);

            String tag = (i < SHORT_CODES.length) ? SHORT_CODES[i] : "ITEM";
            int textColor = craftable ? 0xFFB8FFD2 : 0xFF7D8590;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(cx + 20, cy + 6, 0);
            guiGraphics.pose().scale(0.72f, 0.72f, 1.0f);
            guiGraphics.drawString(this.font, tag, 0, 0, textColor, false);
            guiGraphics.pose().popPose();
        }

        // Right Tactical Monitor
        int mx = x + MON_X;
        int my = y + MON_Y;
        guiGraphics.fill(mx, my, mx + MON_W, my + MON_H, 0xFF0E1412);
        drawBorder(guiGraphics, mx, my, MON_W, MON_H, 0xFF2B6E4C);

        if (selectedIndex < 0 || selectedIndex >= all.size()) return;
        StationBlueprints.Blueprint bp = all.get(selectedIndex);
        ItemStack result = bp.createDefaultResult();

        // Selected blueprint header
        guiGraphics.renderItem(result, mx + 4, my + 4);
        String nameStr = result.getHoverName().getString();
        if (nameStr.length() > 17) {
            nameStr = nameStr.substring(0, 16) + "..";
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(mx + 23, my + 5, 0);
        guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
        guiGraphics.drawString(this.font, nameStr, 0, 0, 0xFF5CFF9D, false);
        guiGraphics.pose().popPose();

        Component cat = Component.translatable(bp.categoryKey());
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(mx + 23, my + 13, 0);
        guiGraphics.pose().scale(0.68f, 0.68f, 1.0f);
        guiGraphics.drawString(this.font, cat, 0, 0, 0xFF56B4D3, false);
        guiGraphics.pose().popPose();

        // Ingredient requirements (up to 6 ingredients in 3x2 grid)
        List<StationBlueprints.IngredientReq> reqs = bp.ingredients();
        int startIx = mx + 6;
        int startIy = my + 24;
        for (int idx = 0; idx < reqs.size(); idx++) {
            StationBlueprints.IngredientReq req = reqs.get(idx);
            int rCol = idx % 3;
            int rRow = idx / 3;
            int ix = startIx + rCol * 35;
            int iy = startIy + rRow * 18;

            int have = this.minecraft.player.isCreative()
                    ? req.count()
                    : StationBlueprints.countAvailable(this.minecraft.player, this.menu.getStationContainer(), req.item().get());
            boolean ok = have >= req.count();

            guiGraphics.fill(ix, iy, ix + 33, iy + 16, ok ? 0xFF15261C : 0xFF281618);
            guiGraphics.renderItem(req.asStack(), ix + 1, iy);

            String countLabel = Math.min(99, have) + "/" + req.count();
            int cColor = ok ? 0xFF66FF99 : 0xFFFF6B6B;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(ix + 18, iy + 5, 200);
            guiGraphics.pose().scale(0.62f, 0.62f, 1.0f);
            guiGraphics.drawString(this.font, countLabel, 0, 0, cColor, true);
            guiGraphics.pose().popPose();
        }
    }

    private void renderModifyTab(GuiGraphics guiGraphics, int x, int y) {
        // Tactical Modify / Flash / Repair Panel
        guiGraphics.fill(x + 8, y + 26, x + GUI_W - 8, y + 98, 0xFF101618);
        drawBorder(guiGraphics, x + 8, y + 26, GUI_W - 16, 72, 0xFF2F4F58);

        // Station Slots at exact Menu coordinates (56, 39), (92, 39), (128, 39)
        drawSlotBox(guiGraphics, x + 55, y + 38);
        drawSlotBox(guiGraphics, x + 91, y + 38);
        drawSlotBox(guiGraphics, x + 127, y + 38);

        // Plus & Arrow indicators (Arrow at 110, 40 for JEI click area)
        guiGraphics.drawString(this.font, "+", x + 79, y + 43, 0xFF56B4D3, false);
        guiGraphics.drawString(this.font, "->", x + 111, y + 43, 0xFF3CFF82, false);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 14, y + 66, 0);
        guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
        guiGraphics.drawString(this.font, Component.translatable("gui.vision_goggles.modify.hint1"), 0, 0, 0xFF9FB3C0, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vision_goggles.modify.hint2"), 0, 12, 0xFF6E828E, false);
        guiGraphics.pose().popPose();
    }

    private void renderExtraTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu.getCurrentTab() != ModificationStationMenu.TAB_ASSEMBLE) return;
        List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
        for (int i = 0; i < all.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            int cx = this.leftPos + GRID_X + col * (CELL_W + CELL_GAP);
            int cy = this.topPos + GRID_Y + row * (CELL_H + CELL_GAP);
            if (mouseX >= cx && mouseX < cx + CELL_W && mouseY >= cy && mouseY < cy + CELL_H) {
                guiGraphics.renderTooltip(this.font, all.get(i).createDefaultResult(), mouseX, mouseY);
                return;
            }
        }
        if (selectedIndex >= 0 && selectedIndex < all.size()) {
            StationBlueprints.Blueprint bp = all.get(selectedIndex);
            int mx = this.leftPos + MON_X;
            int my = this.topPos + MON_Y;
            if (mouseX >= mx + 4 && mouseX < mx + 22 && mouseY >= my + 4 && mouseY < my + 22) {
                guiGraphics.renderTooltip(this.font, bp.createDefaultResult(), mouseX, mouseY);
                return;
            }
            List<StationBlueprints.IngredientReq> reqs = bp.ingredients();
            for (int idx = 0; idx < reqs.size(); idx++) {
                int rCol = idx % 3;
                int rRow = idx / 3;
                int ix = mx + 6 + rCol * 35;
                int iy = my + 24 + rRow * 18;
                if (mouseX >= ix && mouseX < ix + 33 && mouseY >= iy && mouseY < iy + 16) {
                    guiGraphics.renderTooltip(this.font, reqs.get(idx).asStack(), mouseX, mouseY);
                    return;
                }
            }
        }
    }

    private static void drawSlotBox(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF0E1013);
        drawBorder(guiGraphics, x, y, 18, 18, 0xFF3A4049);
    }

    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int w, int h, int color) {
        guiGraphics.fill(x, y, x + w, y + 1, color);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, color);
        guiGraphics.fill(x, y, x + 1, y + h, color);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
