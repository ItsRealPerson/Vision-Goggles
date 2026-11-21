package dev.itsrealperson.vision_goggles.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.itsrealperson.vision_goggles.block.entity.ModificationStationBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.List;

/**
 * Left-docked tactical workbench UI that keeps the center 3D green cutting mat
 * and the player's first-person hands completely unobstructed while placing parts
 * step by step and assembling goggles.
 */
public class ModificationStationScreen extends AbstractContainerScreen<ModificationStationMenu> {

    private static final int GUI_W = 176;
    private static final int GUI_H = 188;

    private Button btnAssembleTab;
    private Button btnModifyTab;
    private Button btnAssembleAction;

    public ModificationStationScreen(ModificationStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 93;
    }

    @Override
    protected void init() {
        super.init();
        // Dock left so the center 3D table and both player hands stay completely visible
        this.leftPos = 6;
        this.topPos = Math.max(6, (this.height - this.imageHeight) / 2);

        StationWorkAnimator.setWorkbenchOpen(true, this.menu.getStationPos());

        int x = this.leftPos;
        int y = this.topPos;

        this.btnAssembleTab = this.addRenderableWidget(
                new WorkstationButton(x + 6, y + 6, 76, 15, Component.translatable("gui.vision_goggles.tab.assemble"), b -> switchTab(ModificationStationMenu.TAB_ASSEMBLE), true)
        );
        this.btnModifyTab = this.addRenderableWidget(
                new WorkstationButton(x + 84, y + 6, 86, 15, Component.translatable("gui.vision_goggles.tab.modify"), b -> switchTab(ModificationStationMenu.TAB_MODIFY), true)
        );
        this.btnAssembleAction = this.addRenderableWidget(
                new WorkstationButton(x + 6, y + 77, GUI_W - 12, 14, Component.translatable("gui.vision_goggles.button.assemble"), b -> triggerFinalizeAssembly(), false)
        );
        updateButtonStates();
    }

    @Override
    public void removed() {
        super.removed();
        StationWorkAnimator.setWorkbenchOpen(false, null);
    }

    public boolean isModifyTabActive() {
        return this.menu.getCurrentTab() == ModificationStationMenu.TAB_MODIFY;
    }

    private ModificationStationBlockEntity getStation() {
        if (this.minecraft == null || this.minecraft.level == null) return null;
        BlockEntity be = this.minecraft.level.getBlockEntity(this.menu.getStationPos());
        return (be instanceof ModificationStationBlockEntity station) ? station : null;
    }

    private StationBlueprints.Blueprint getSelectedBlueprint(ModificationStationBlockEntity station) {
        String id = (station != null) ? station.getActiveBlueprintId() : "night_vision_goggles";
        StationBlueprints.Blueprint bp = StationBlueprints.byId(id);
        return (bp != null) ? bp : StationBlueprints.getAll().get(0);
    }

    private void switchTab(int tab) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, tab);
        }
    }

    private void triggerFinalizeAssembly() {
        ModificationStationBlockEntity station = getStation();
        StationBlueprints.Blueprint bp = getSelectedBlueprint(station);
        List<ItemStack> staged = (station != null) ? station.getStagedItems() : Collections.emptyList();
        if (!StationBlueprints.areAllStaged(staged, bp)) return;

        NetworkManager.INSTANCE.sendToServer(
                new AssembleBlueprintPacket(this.menu.getStationPos(), AssembleBlueprintPacket.Action.FINALIZE_ASSEMBLY, bp.id(), 0)
        );
        StationWorkAnimator.startLocalWork(this.menu.getStationPos());
    }

    private void updateButtonStates() {
        boolean isAssemble = (this.menu.getCurrentTab() == ModificationStationMenu.TAB_ASSEMBLE);
        if (this.btnAssembleTab != null) this.btnAssembleTab.active = !isAssemble;
        if (this.btnModifyTab != null) this.btnModifyTab.active = isAssemble;
        if (this.btnAssembleAction != null) {
            this.btnAssembleAction.visible = isAssemble;
            if (isAssemble) {
                ModificationStationBlockEntity station = getStation();
                StationBlueprints.Blueprint bp = getSelectedBlueprint(station);
                List<ItemStack> staged = (station != null) ? station.getStagedItems() : Collections.emptyList();
                boolean ready = StationBlueprints.areAllStaged(staged, bp);
                this.btnAssembleAction.active = ready && (station == null || !station.isAssembling());
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
        if (this.menu.getCurrentTab() == ModificationStationMenu.TAB_ASSEMBLE) {
            ModificationStationBlockEntity station = getStation();
            boolean busy = (station != null && station.isAssembling());

            // 1. Blueprint selector row (7 icons across top)
            if (button == 0 && !busy) {
                List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
                for (int i = 0; i < all.size(); i++) {
                    int bx = this.leftPos + 6 + i * 24;
                    int by = this.topPos + 24;
                    if (mouseX >= bx && mouseX < bx + 22 && mouseY >= by && mouseY < by + 18) {
                        NetworkManager.INSTANCE.sendToServer(
                                new AssembleBlueprintPacket(this.menu.getStationPos(), AssembleBlueprintPacket.Action.SELECT_BLUEPRINT, all.get(i).id(), i)
                        );
                        return true;
                    }
                }
            }

            // 2. Step-by-step ingredient cards (Left-click = place 1 onto mat, Right-click = take 1 back)
            if ((button == 0 || button == 1) && !busy) {
                StationBlueprints.Blueprint bp = getSelectedBlueprint(station);
                List<StationBlueprints.IngredientReq> reqs = bp.ingredients();
                for (int idx = 0; idx < reqs.size(); idx++) {
                    int col = idx % 3;
                    int row = idx / 3;
                    int ix = this.leftPos + 6 + col * 55;
                    int iy = this.topPos + 45 + row * 15;
                    if (mouseX >= ix && mouseX < ix + 53 && mouseY >= iy && mouseY < iy + 14) {
                        AssembleBlueprintPacket.Action act = (button == 0)
                                ? AssembleBlueprintPacket.Action.ADD_INGREDIENT
                                : AssembleBlueprintPacket.Action.REMOVE_INGREDIENT;
                        NetworkManager.INSTANCE.sendToServer(
                                new AssembleBlueprintPacket(this.menu.getStationPos(), act, bp.id(), idx)
                        );
                        StationWorkAnimator.triggerPlacePart((idx % 2) == 0);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        // Intentionally do NOT darken the world center so the 3D cutting mat & hands stay bright and clear
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateButtonStates();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderExtraTooltips(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFB8C0C8, true);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;

        // Industrial Gunmetal 3D-Beveled Minecraft Workstation Frame
        drawBevelPanel(guiGraphics, x, y, GUI_W, GUI_H, 0xFF2B2F36, 0xFF545B66, 0xFF191C20, 0xFF0E1012);
        // Subtle corner rivets
        guiGraphics.fill(x + 3, y + 3, x + 5, y + 5, 0xFF424852);
        guiGraphics.fill(x + GUI_W - 5, y + 3, x + GUI_W - 3, y + 5, 0xFF424852);
        guiGraphics.fill(x + 3, y + GUI_H - 5, x + 5, y + GUI_H - 3, 0xFF424852);
        guiGraphics.fill(x + GUI_W - 5, y + GUI_H - 5, x + GUI_W - 3, y + GUI_H - 3, 0xFF424852);

        if (this.menu.getCurrentTab() == ModificationStationMenu.TAB_ASSEMBLE) {
            renderAssembleTab(guiGraphics, x, y, mouseX, mouseY);
        } else {
            renderModifyTab(guiGraphics, x, y);
        }

        // Divider groove above player inventory
        guiGraphics.fill(x + 6, y + 100, x + GUI_W - 6, y + 101, 0xFF171A1E);
        guiGraphics.fill(x + 6, y + 101, x + GUI_W - 6, y + 102, 0xFF444A54);

        // Classic Recessed Minecraft Inventory & Hotbar Slots
        int invX = x + 7;
        int invY = y + 103;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(guiGraphics, invX + col * 18, invY + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBox(guiGraphics, invX + col * 18, invY + 58);
        }
    }

    private void renderAssembleTab(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        ModificationStationBlockEntity station = getStation();
        StationBlueprints.Blueprint activeBp = getSelectedBlueprint(station);
        List<ItemStack> staged = (station != null) ? station.getStagedItems() : Collections.emptyList();

        // 1. Recessed Blueprint Rack (7 blueprints across top)
        drawRecessedBox(guiGraphics, x + 5, y + 23, GUI_W - 10, 20, 0xFF1A1D22);
        List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
        for (int i = 0; i < all.size(); i++) {
            StationBlueprints.Blueprint bp = all.get(i);
            int bx = x + 6 + i * 24;
            int by = y + 24;
            boolean selected = bp.id().equals(activeBp.id());
            boolean hovered = mouseX >= bx && mouseX < bx + 22 && mouseY >= by && mouseY < by + 18;
            if (selected) {
                drawBevelPanel(guiGraphics, bx, by, 22, 18, 0xFF28382E, 0xFF4F785D, 0xFF18221B, 0xFF111417);
                guiGraphics.fill(bx + 2, by + 15, bx + 20, by + 17, 0xFF52B778);
            } else {
                int fill = hovered ? 0xFF2B3038 : 0xFF21252B;
                drawRecessedBox(guiGraphics, bx, by, 22, 18, fill);
            }
            guiGraphics.renderItem(bp.createDefaultResult(), bx + 3, by + 1);
        }

        // 2. Step-by-step Ingredient Cards (click to place onto mat, right-click to remove)
        List<StationBlueprints.IngredientReq> reqs = activeBp.ingredients();
        for (int idx = 0; idx < reqs.size(); idx++) {
            StationBlueprints.IngredientReq req = reqs.get(idx);
            int col = idx % 3;
            int row = idx / 3;
            int ix = x + 6 + col * 55;
            int iy = y + 45 + row * 15;

            int placed = StationBlueprints.countInList(staged, req.item().get());
            int invHave = this.minecraft.player.isCreative()
                    ? req.count()
                    : StationBlueprints.countAvailable(this.minecraft.player, this.menu.getStationContainer(), req.item().get());
            boolean done = placed >= req.count();
            boolean canAddMore = !done && invHave > 0;
            boolean hovered = mouseX >= ix && mouseX < ix + 53 && mouseY >= iy && mouseY < iy + 14;

            int fill = done ? 0xFF202B24 : (hovered && canAddMore ? 0xFF2A3038 : 0xFF1D2026);
            drawRecessedBox(guiGraphics, ix, iy, 53, 14, fill);

            // Subtle 2px status indicator bar on left edge
            int pipColor = done ? 0xFF47A866 : (canAddMore ? 0xFFD99B38 : 0xFF8C3F3F);
            guiGraphics.fill(ix + 1, iy + 2, ix + 3, iy + 12, pipColor);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(ix + 4, iy + 1, 0);
            guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
            guiGraphics.renderItem(req.asStack(), 0, 0);
            guiGraphics.pose().popPose();

            String label = placed + "/" + req.count();
            int textColor = done ? 0xFF8BE0A4 : (canAddMore ? 0xFFE5E9F0 : 0xFFCC7676);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(ix + 21, iy + 4, 200);
            guiGraphics.pose().scale(0.72f, 0.72f, 1.0f);
            guiGraphics.drawString(this.font, label, 0, 0, textColor, true);
            guiGraphics.pose().popPose();
        }
    }

    private void renderModifyTab(GuiGraphics guiGraphics, int x, int y) {
        drawRecessedBox(guiGraphics, x + 6, y + 25, GUI_W - 12, 65, 0xFF1B1E23);

        // Station Slots at exact Menu coordinates (34, 42), (70, 42), (106, 42)
        drawSlotBox(guiGraphics, x + 33, y + 41);
        drawSlotBox(guiGraphics, x + 69, y + 41);
        drawSlotBox(guiGraphics, x + 105, y + 41);

        // Clean pixel "+" and "->" arrow
        guiGraphics.fill(x + 56, y + 49, x + 63, y + 51, 0xFF8B95A1);
        guiGraphics.fill(x + 59, y + 46, x + 60, y + 54, 0xFF8B95A1);

        guiGraphics.fill(x + 90, y + 49, x + 99, y + 51, 0xFF52B778);
        guiGraphics.fill(x + 97, y + 47, x + 99, y + 53, 0xFF52B778);
        guiGraphics.fill(x + 99, y + 48, x + 101, y + 52, 0xFF52B778);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 10, y + 66, 0);
        guiGraphics.pose().scale(0.68f, 0.68f, 1.0f);
        guiGraphics.drawString(this.font, Component.translatable("gui.vision_goggles.modify.hint1"), 0, 0, 0xFFB0B8C2, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vision_goggles.modify.hint2"), 0, 12, 0xFF7D8794, false);
        guiGraphics.pose().popPose();
    }

    private void renderExtraTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu.getCurrentTab() != ModificationStationMenu.TAB_ASSEMBLE) return;
        List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
        for (int i = 0; i < all.size(); i++) {
            int bx = this.leftPos + 6 + i * 24;
            int by = this.topPos + 24;
            if (mouseX >= bx && mouseX < bx + 22 && mouseY >= by && mouseY < by + 18) {
                guiGraphics.renderTooltip(this.font, all.get(i).createDefaultResult(), mouseX, mouseY);
                return;
            }
        }
        ModificationStationBlockEntity station = getStation();
        StationBlueprints.Blueprint bp = getSelectedBlueprint(station);
        List<StationBlueprints.IngredientReq> reqs = bp.ingredients();
        for (int idx = 0; idx < reqs.size(); idx++) {
            int col = idx % 3;
            int row = idx / 3;
            int ix = this.leftPos + 6 + col * 55;
            int iy = this.topPos + 45 + row * 15;
            if (mouseX >= ix && mouseX < ix + 53 && mouseY >= iy && mouseY < iy + 14) {
                guiGraphics.renderTooltip(this.font, reqs.get(idx).asStack(), mouseX, mouseY);
                return;
            }
        }
    }

    private static void drawSlotBox(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF1D2025);
        // Classic Minecraft recessed 3D slot border
        guiGraphics.fill(x, y, x + 17, y + 1, 0xFF101215);
        guiGraphics.fill(x, y, x + 1, y + 17, 0xFF101215);
        guiGraphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFF4A505A);
        guiGraphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFF4A505A);
    }

    private static void drawRecessedBox(GuiGraphics guiGraphics, int x, int y, int w, int h, int fill) {
        guiGraphics.fill(x, y, x + w, y + h, fill);
        guiGraphics.fill(x, y, x + w - 1, y + 1, 0xFF101215);
        guiGraphics.fill(x, y, x + 1, y + h - 1, 0xFF101215);
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

    private final class WorkstationButton extends Button {
        private final boolean isTab;

        private WorkstationButton(int x, int y, int w, int h, Component message, OnPress onPress, boolean isTab) {
            super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
            this.isTab = isTab;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int bx = this.getX();
            int by = this.getY();
            int bw = this.getWidth();
            int bh = this.getHeight();
            if (this.isTab) {
                // !this.active means this tab is currently selected
                boolean selected = !this.active;
                if (selected) {
                    drawBevelPanel(guiGraphics, bx, by, bw, bh, 0xFF343942, 0xFF5D6572, 0xFF20242A, 0xFF0E1012);
                    guiGraphics.fill(bx + 2, by + 1, bx + bw - 2, by + 3, 0xFF52B778);
                } else {
                    int fill = this.isHovered() ? 0xFF2A2F37 : 0xFF1F2329;
                    drawRecessedBox(guiGraphics, bx, by, bw, bh, fill);
                }
                int textCol = selected ? 0xFFF2F5F8 : (this.isHovered() ? 0xFFD0D6DE : 0xFF8B94A0);
                guiGraphics.drawCenteredString(ModificationStationScreen.this.font, this.getMessage(), bx + bw / 2, by + (bh - 8) / 2, textCol);
            } else {
                if (this.active) {
                    int body = this.isHovered() ? 0xFF33734B : 0xFF295D3D;
                    int light = this.isHovered() ? 0xFF5BC282 : 0xFF49A36B;
                    drawBevelPanel(guiGraphics, bx, by, bw, bh, body, light, 0xFF183825, 0xFF0E1012);
                    guiGraphics.drawCenteredString(ModificationStationScreen.this.font, this.getMessage(), bx + bw / 2, by + (bh - 8) / 2, 0xFFF4FFF7);
                } else {
                    drawRecessedBox(guiGraphics, bx, by, bw, bh, 0xFF1F2329);
                    guiGraphics.drawCenteredString(ModificationStationScreen.this.font, this.getMessage(), bx + bw / 2, by + (bh - 8) / 2, 0xFF666E78);
                }
            }
        }
    }
}
