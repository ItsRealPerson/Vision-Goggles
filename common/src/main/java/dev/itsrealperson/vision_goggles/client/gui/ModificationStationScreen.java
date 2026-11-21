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
                Button.builder(Component.translatable("gui.vision_goggles.tab.assemble"), b -> switchTab(ModificationStationMenu.TAB_ASSEMBLE))
                        .bounds(x + 6, y + 6, 72, 15)
                        .build()
        );
        this.btnModifyTab = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.vision_goggles.tab.modify"), b -> switchTab(ModificationStationMenu.TAB_MODIFY))
                        .bounds(x + 81, y + 6, 89, 15)
                        .build()
        );
        this.btnAssembleAction = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.vision_goggles.button.assemble"), b -> triggerFinalizeAssembly())
                        .bounds(x + 6, y + 77, GUI_W - 12, 14)
                        .build()
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
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF9AA3AE, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.leftPos;
        int y = this.topPos;

        // Compact Left-Docked Tactical Panel (translucent charcoal so it blends cleanly)
        guiGraphics.fill(x, y, x + GUI_W, y + GUI_H, 0xE816191E);
        drawBorder(guiGraphics, x, y, GUI_W, GUI_H, 0xFF3A414B);

        if (this.menu.getCurrentTab() == ModificationStationMenu.TAB_ASSEMBLE) {
            renderAssembleTab(guiGraphics, x, y);
        } else {
            renderModifyTab(guiGraphics, x, y);
        }

        // Player Inventory & Hotbar Slot Frames
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

    private void renderAssembleTab(GuiGraphics guiGraphics, int x, int y) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        ModificationStationBlockEntity station = getStation();
        StationBlueprints.Blueprint activeBp = getSelectedBlueprint(station);
        List<ItemStack> staged = (station != null) ? station.getStagedItems() : Collections.emptyList();

        // 1. Blueprint Icon Strip (7 blueprints across top)
        List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
        for (int i = 0; i < all.size(); i++) {
            StationBlueprints.Blueprint bp = all.get(i);
            int bx = x + 6 + i * 24;
            int by = y + 24;
            boolean selected = bp.id().equals(activeBp.id());
            int bg = selected ? 0xFF21382B : 0xFF1D2229;
            int border = selected ? 0xFF3CFF82 : 0xFF323943;
            guiGraphics.fill(bx, by, bx + 22, by + 18, bg);
            drawBorder(guiGraphics, bx, by, 22, 18, border);
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

            int bg = done ? 0xFF153021 : (canAddMore ? 0xFF1D262D : 0xFF24171A);
            int border = done ? 0xFF3CFF82 : (canAddMore ? 0xFF3C7C96 : 0xFF522A30);
            guiGraphics.fill(ix, iy, ix + 53, iy + 14, bg);
            drawBorder(guiGraphics, ix, iy, 53, 14, border);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(ix + 1, iy, 0);
            guiGraphics.pose().scale(0.82f, 0.82f, 1.0f);
            guiGraphics.renderItem(req.asStack(), 0, 0);
            guiGraphics.pose().popPose();

            String label = placed + "/" + req.count();
            int color = done ? 0xFF5CFF9D : (canAddMore ? 0xFF8BE2FF : 0xFFFF7A7A);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(ix + 18, iy + 4, 200);
            guiGraphics.pose().scale(0.68f, 0.68f, 1.0f);
            guiGraphics.drawString(this.font, label, 0, 0, color, true);
            guiGraphics.pose().popPose();
        }
    }

    private void renderModifyTab(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x + 6, y + 25, x + GUI_W - 6, y + 90, 0xFF11161A);
        drawBorder(guiGraphics, x + 6, y + 25, GUI_W - 12, 65, 0xFF2F4F58);

        // Station Slots at exact Menu coordinates (34, 42), (70, 42), (106, 42)
        drawSlotBox(guiGraphics, x + 33, y + 41);
        drawSlotBox(guiGraphics, x + 69, y + 41);
        drawSlotBox(guiGraphics, x + 105, y + 41);

        guiGraphics.drawString(this.font, "+", x + 57, y + 46, 0xFF56B4D3, false);
        guiGraphics.drawString(this.font, "->", x + 89, y + 46, 0xFF3CFF82, false);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x + 10, y + 66, 0);
        guiGraphics.pose().scale(0.66f, 0.66f, 1.0f);
        guiGraphics.drawString(this.font, Component.translatable("gui.vision_goggles.modify.hint1"), 0, 0, 0xFF9FB3C0, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.vision_goggles.modify.hint2"), 0, 12, 0xFF6E828E, false);
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
