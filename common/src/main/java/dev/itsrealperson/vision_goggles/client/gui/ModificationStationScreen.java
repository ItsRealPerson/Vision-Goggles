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
    private static final int MAX_UNPAGINATED_BLUEPRINTS = 7;
    private static final int PAGINATED_VISIBLE_BLUEPRINTS = 6;

    private Button btnAssembleTab;
    private Button btnModifyTab;
    private Button btnAssembleAction;
    private int blueprintOffset = 0;

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
        ensureActiveBlueprintVisible();
        updateButtonStates();
    }

    private static boolean isBlueprintRackPaginated(int total) {
        return total > MAX_UNPAGINATED_BLUEPRINTS;
    }

    private static int getVisibleBlueprintCount(int total) {
        return isBlueprintRackPaginated(total) ? PAGINATED_VISIBLE_BLUEPRINTS : total;
    }

    private static int getMaxBlueprintOffset(int total) {
        return Math.max(0, total - getVisibleBlueprintCount(total));
    }

    private static int getBlueprintButtonX(int guiX, int visibleSlot, boolean paginated) {
        return paginated ? (guiX + 20 + visibleSlot * 23) : (guiX + 6 + visibleSlot * 24);
    }

    private void clampBlueprintOffset(int total) {
        int max = getMaxBlueprintOffset(total);
        if (this.blueprintOffset < 0) this.blueprintOffset = 0;
        if (this.blueprintOffset > max) this.blueprintOffset = max;
    }

    private void ensureActiveBlueprintVisible() {
        List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
        if (!isBlueprintRackPaginated(all.size())) {
            this.blueprintOffset = 0;
            return;
        }
        StationBlueprints.Blueprint active = getSelectedBlueprint(getStation());
        int activeIdx = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(active.id())) {
                activeIdx = i;
                break;
            }
        }
        int visible = getVisibleBlueprintCount(all.size());
        if (activeIdx < this.blueprintOffset) {
            this.blueprintOffset = activeIdx;
        } else if (activeIdx >= this.blueprintOffset + visible) {
            this.blueprintOffset = activeIdx - visible + 1;
        }
        clampBlueprintOffset(all.size());
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

            // 1. Blueprint selector row (paginated with < / > arrows when > 7 blueprints)
            if (button == 0 && !busy) {
                List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
                boolean paginated = isBlueprintRackPaginated(all.size());
                clampBlueprintOffset(all.size());
                int by = this.topPos + 24;

                if (paginated) {
                    int maxOffset = getMaxBlueprintOffset(all.size());
                    if (mouseX >= this.leftPos + 6 && mouseX < this.leftPos + 18 && mouseY >= by && mouseY < by + 18) {
                        if (this.blueprintOffset > 0) {
                            this.blueprintOffset--;
                        }
                        return true;
                    }
                    if (mouseX >= this.leftPos + 158 && mouseX < this.leftPos + 170 && mouseY >= by && mouseY < by + 18) {
                        if (this.blueprintOffset < maxOffset) {
                            this.blueprintOffset++;
                        }
                        return true;
                    }
                }

                int visible = getVisibleBlueprintCount(all.size());
                for (int slot = 0; slot < visible; slot++) {
                    int idx = this.blueprintOffset + slot;
                    if (idx >= all.size()) break;
                    int bx = getBlueprintButtonX(this.leftPos, slot, paginated);
                    if (mouseX >= bx && mouseX < bx + 22 && mouseY >= by && mouseY < by + 18) {
                        NetworkManager.INSTANCE.sendToServer(
                                new AssembleBlueprintPacket(this.menu.getStationPos(), AssembleBlueprintPacket.Action.SELECT_BLUEPRINT, all.get(idx).id(), idx)
                        );
                        return true;
                    }
                }
            }

            // 2. Step-by-step ingredient cards (Left-click = place 1 onto mat, Right-click = take 1 back)
            if ((button == 0 || button == 1) && !busy) {
                StationBlueprints.Blueprint bp = getSelectedBlueprint(station);
                List<StationBlueprints.IngredientReq> reqs = bp.ingredients();
                List<ItemStack> staged = (station != null) ? station.getStagedItems() : Collections.emptyList();
                for (int idx = 0; idx < reqs.size(); idx++) {
                    int col = idx % 3;
                    int row = idx / 3;
                    int ix = this.leftPos + 6 + col * 55;
                    int iy = this.topPos + 45 + row * 15;
                    if (mouseX >= ix && mouseX < ix + 53 && mouseY >= iy && mouseY < iy + 14) {
                        StationBlueprints.IngredientReq req = reqs.get(idx);
                        int placed = StationBlueprints.countInList(staged, req.item().get());
                        int invHave = (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative())
                                ? req.count()
                                : (this.minecraft != null && this.minecraft.player != null
                                        ? StationBlueprints.countAvailable(this.minecraft.player, this.menu.getStationContainer(), req.item().get())
                                        : 0);
                        boolean canAct = (button == 0) ? (placed < req.count() && invHave > 0) : (placed > 0);
                        if (canAct) {
                            AssembleBlueprintPacket.Action act = (button == 0)
                                    ? AssembleBlueprintPacket.Action.ADD_INGREDIENT
                                    : AssembleBlueprintPacket.Action.REMOVE_INGREDIENT;
                            NetworkManager.INSTANCE.sendToServer(
                                    new AssembleBlueprintPacket(this.menu.getStationPos(), act, bp.id(), idx)
                            );
                            StationWorkAnimator.triggerPlacePart((idx % 2) == 0);
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.menu.getCurrentTab() == ModificationStationMenu.TAB_ASSEMBLE) {
            List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
            if (isBlueprintRackPaginated(all.size())
                    && mouseX >= this.leftPos + 5 && mouseX < this.leftPos + GUI_W - 5
                    && mouseY >= this.topPos + 23 && mouseY < this.topPos + 43) {
                this.blueprintOffset += (delta > 0 ? -1 : 1);
                clampBlueprintOffset(all.size());
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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

        // 1. Recessed Blueprint Rack (paginated with < / > arrows when > 7 blueprints)
        drawRecessedBox(guiGraphics, x + 5, y + 23, GUI_W - 10, 20, 0xFF1A1D22);
        List<StationBlueprints.Blueprint> all = StationBlueprints.getAll();
        boolean paginated = isBlueprintRackPaginated(all.size());
        clampBlueprintOffset(all.size());
        int by = y + 24;

        if (paginated) {
            int maxOffset = getMaxBlueprintOffset(all.size());
            boolean canLeft = this.blueprintOffset > 0;
            boolean canRight = this.blueprintOffset < maxOffset;
            boolean hoverLeft = mouseX >= x + 6 && mouseX < x + 18 && mouseY >= by && mouseY < by + 18;
            boolean hoverRight = mouseX >= x + 158 && mouseX < x + 170 && mouseY >= by && mouseY < by + 18;

            drawPageArrowButton(guiGraphics, x + 6, by, "<", canLeft, hoverLeft);
            drawPageArrowButton(guiGraphics, x + 158, by, ">", canRight, hoverRight);
        }

        int visible = getVisibleBlueprintCount(all.size());
        for (int slot = 0; slot < visible; slot++) {
            int idx = this.blueprintOffset + slot;
            if (idx >= all.size()) break;
            StationBlueprints.Blueprint bp = all.get(idx);
            int bx = getBlueprintButtonX(x, slot, paginated);
            boolean selected = bp.id().equals(activeBp.id());
            boolean hovered = mouseX >= bx && mouseX < bx + 22 && mouseY >= by && mouseY < by + 18;
            if (selected) {
                // Classic Minecraft Stonecutter-style selected button bevel
                drawBevelPanel(guiGraphics, bx, by, 22, 18, 0xFF434A54, 0xFF8A94A3, 0xFF23272E, 0xFF0E1012);
            } else {
                int body = hovered ? 0xFF333942 : 0xFF262A31;
                int light = hovered ? 0xFF5A6270 : 0xFF3F454F;
                drawBevelPanel(guiGraphics, bx, by, 22, 18, body, light, 0xFF16181D, 0xFF0E1012);
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

            int fill;
            if (done) {
                fill = hovered ? 0xFF28382B : 0xFF202C22;
            } else if (canAddMore) {
                fill = hovered ? 0xFF2B3038 : 0xFF1D2025;
            } else {
                fill = hovered ? 0xFF452323 : 0xFF381C1C;
            }
            drawRecessedBox(guiGraphics, ix, iy, 53, 14, fill);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(ix + 2, iy + 1, 0);
            guiGraphics.pose().scale(0.75f, 0.75f, 1.0f);
            guiGraphics.renderItem(req.asStack(), 0, 0);
            guiGraphics.pose().popPose();

            String label = placed + "/" + req.count();
            int textColor = done ? 0xFF55FF55 : (canAddMore ? 0xFFFFFFFF : 0xFFFF5555);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(ix + 19, iy + 4, 200);
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

        // Classic Minecraft Anvil/Smithing "+" (symmetrical 8x8 with 1px shadow)
        guiGraphics.fill(x + 57, y + 50, x + 65, y + 52, 0xFF101215);
        guiGraphics.fill(x + 60, y + 47, x + 62, y + 55, 0xFF101215);
        guiGraphics.fill(x + 56, y + 49, x + 64, y + 51, 0xFF8B8B8B);
        guiGraphics.fill(x + 59, y + 46, x + 61, y + 54, 0xFF8B8B8B);

        // Classic Minecraft Crafting/Furnace "->" arrow (gray with 1px shadow)
        guiGraphics.fill(x + 91, y + 50, x + 99, y + 52, 0xFF101215);
        guiGraphics.fill(x + 98, y + 48, x + 100, y + 54, 0xFF101215);
        guiGraphics.fill(x + 100, y + 49, x + 102, y + 53, 0xFF101215);
        guiGraphics.fill(x + 102, y + 50, x + 103, y + 52, 0xFF101215);

        guiGraphics.fill(x + 90, y + 49, x + 98, y + 51, 0xFF8B8B8B);
        guiGraphics.fill(x + 97, y + 47, x + 99, y + 53, 0xFF8B8B8B);
        guiGraphics.fill(x + 99, y + 48, x + 101, y + 52, 0xFF8B8B8B);
        guiGraphics.fill(x + 101, y + 49, x + 102, y + 51, 0xFF8B8B8B);

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
        boolean paginated = isBlueprintRackPaginated(all.size());
        clampBlueprintOffset(all.size());
        int visible = getVisibleBlueprintCount(all.size());
        int by = this.topPos + 24;
        for (int slot = 0; slot < visible; slot++) {
            int idx = this.blueprintOffset + slot;
            if (idx >= all.size()) break;
            int bx = getBlueprintButtonX(this.leftPos, slot, paginated);
            if (mouseX >= bx && mouseX < bx + 22 && mouseY >= by && mouseY < by + 18) {
                guiGraphics.renderTooltip(this.font, all.get(idx).createDefaultResult(), mouseX, mouseY);
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
                java.util.List<Component> lines = new java.util.ArrayList<>(
                        getTooltipFromItem(this.minecraft, reqs.get(idx).asStack())
                );
                lines.add(Component.translatable("gui.vision_goggles.tooltip.stage_controls"));
                guiGraphics.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    private void drawPageArrowButton(GuiGraphics guiGraphics, int bx, int by, String symbol, boolean enabled, boolean hovered) {
        if (enabled) {
            int body = hovered ? 0xFF3A414C : 0xFF282D35;
            int light = hovered ? 0xFF677182 : 0xFF474E5A;
            drawBevelPanel(guiGraphics, bx, by, 12, 18, body, light, 0xFF16181D, 0xFF0E1012);
            int textCol = hovered ? 0xFFFFFFFF : 0xFFD0D8E2;
            guiGraphics.drawCenteredString(this.font, symbol, bx + 6, by + 5, textCol);
        } else {
            drawRecessedBox(guiGraphics, bx, by, 12, 18, 0xFF1D2025);
            guiGraphics.drawCenteredString(this.font, symbol, bx + 6, by + 5, 0xFF4D545E);
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
                    drawBevelPanel(guiGraphics, bx, by, bw, bh, 0xFF3A404A, 0xFF6B7382, 0xFF1F2329, 0xFF0E1012);
                } else {
                    int body = this.isHovered() ? 0xFF2C313A : 0xFF22262D;
                    int light = this.isHovered() ? 0xFF474E5C : 0xFF343A44;
                    drawBevelPanel(guiGraphics, bx, by, bw, bh, body, light, 0xFF15181C, 0xFF0E1012);
                }
                int textCol = selected ? 0xFFFFFFFF : (this.isHovered() ? 0xFFE0E0E0 : 0xFFA0A0A0);
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
