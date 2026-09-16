package dev.itsrealperson.vision_goggles.client.gui;

import dev.architectury.platform.Platform;
import dev.itsrealperson.vision_goggles.scripting.LuaModuleManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LuaScriptViewerScreen extends Screen {

    private final List<File> scriptFiles = new ArrayList<>();
    private int selectedFileIndex = 0;
    private List<String> currentFileLines = new ArrayList<>();
    private int scrollOffset = 0;
    private String statusMessage = "Listo. Selecciona un archivo para ver o editar su código.";
    private int statusColor = 0xFF55FF55;

    // In-game editor
    private MultiLineEditBox codeEditor;
    private boolean isEditMode = false;
    private Button btnEditarGuardar;

    public LuaScriptViewerScreen() {
        super(Component.literal("Terminal de Módulos Lua"));
    }

    @Override
    protected void init() {
        super.init();
        refreshFileList();

        int leftW = 145;
        int rightX = 15 + leftW + 10;
        int rightW = this.width - rightX - 15;
        int codeAreaY = 38;
        int codeAreaH = this.height - 75;

        // Editor multilinea para editar código directamente in-game
        this.codeEditor = new MultiLineEditBox(
            this.font,
            rightX + 2,
            codeAreaY + 18,
            rightW - 4,
            codeAreaH - 22,
            Component.empty(),
            Component.literal("Escribe código Lua aquí...")
        );
        this.codeEditor.setCharacterLimit(100000);
        this.codeEditor.setValue(String.join("\n", currentFileLines));
        this.codeEditor.visible = isEditMode;
        this.addRenderableWidget(this.codeEditor);

        int buttonY = this.height - 30;

        // Botón: Recargar
        this.addRenderableWidget(Button.builder(Component.literal("🔄 Recargar"), btn -> {
            LuaModuleManager.reload();
            refreshFileList();
            statusMessage = "¡Módulos Lua recargados con éxito!";
            statusColor = 0xFF55FF55;
        }).bounds(15, buttonY, 85, 20).build());

        // Botón: Abrir Carpeta en Windows
        this.addRenderableWidget(Button.builder(Component.literal("📂 Carpeta"), btn -> {
            try {
                Path dir = Platform.getConfigFolder().resolve("vision_goggles").resolve("modules");
                Util.getPlatform().openFile(dir.toFile());
                statusMessage = "Abriendo carpeta en el Explorador...";
                statusColor = 0xFF55FFFF;
            } catch (Exception e) {
                statusMessage = "Error abriendo carpeta: " + e.getMessage();
                statusColor = 0xFFFF5555;
            }
        }).bounds(105, buttonY, 85, 20).build());

        // Botón: Obtener Script Físico en el Inventario
        this.addRenderableWidget(Button.builder(Component.literal("📥 Obtener Script"), btn -> {
            if (selectedFileIndex >= 0 && selectedFileIndex < scriptFiles.size()) {
                File file = scriptFiles.get(selectedFileIndex);
                if (file.getName().endsWith(".lua")) {
                    String idToGive = extractModuleId(file);
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.connection.sendCommand("vg givescript " + idToGive);
                        statusMessage = "§a¡Script de Instalación entregado! (" + idToGive + ")";
                        statusColor = 0xFF55FF55;
                    }
                } else {
                    statusMessage = "Solo se pueden generar scripts de archivos .lua";
                    statusColor = 0xFFFF8855;
                }
            }
        }).bounds(195, buttonY, 115, 20).build());

        // Botón: Editar / Guardar
        this.btnEditarGuardar = Button.builder(Component.literal(isEditMode ? "💾 Guardar" : "✏️ Editar"), btn -> {
            if (selectedFileIndex >= 0 && selectedFileIndex < scriptFiles.size()) {
                File file = scriptFiles.get(selectedFileIndex);
                if (!isEditMode) {
                    // Entrar a modo edición
                    isEditMode = true;
                    codeEditor.setValue(String.join("\n", currentFileLines));
                    codeEditor.visible = true;
                    this.setFocused(codeEditor);
                    btnEditarGuardar.setMessage(Component.literal("💾 Guardar"));
                    statusMessage = "Modo edición activado. Modifica el código y haz clic en Guardar.";
                    statusColor = 0xFFFFFF55;
                } else {
                    // Guardar los cambios en el archivo
                    try {
                        String newContent = codeEditor.getValue();
                        Files.writeString(file.toPath(), newContent, StandardCharsets.UTF_8);
                        LuaModuleManager.reload();
                        loadFileContent();
                        isEditMode = false;
                        codeEditor.visible = false;
                        btnEditarGuardar.setMessage(Component.literal("✏️ Editar"));
                        statusMessage = "§a¡Archivo guardado y módulos Lua recargados con éxito!";
                        statusColor = 0xFF55FF55;
                    } catch (Exception e) {
                        statusMessage = "Error al guardar archivo: " + e.getMessage();
                        statusColor = 0xFFFF5555;
                    }
                }
            }
        }).bounds(315, buttonY, 95, 20).build();
        this.addRenderableWidget(this.btnEditarGuardar);

        // Botón: Cerrar
        this.addRenderableWidget(Button.builder(Component.literal("✕ Salir"), btn -> {
            this.onClose();
        }).bounds(this.width - 80, buttonY, 65, 20).build());
    }

    private String extractModuleId(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("id") && trimmed.contains("=")) {
                    int firstQuote = trimmed.indexOf("\"");
                    int lastQuote = trimmed.lastIndexOf("\"");
                    if (firstQuote != -1 && lastQuote > firstQuote) {
                        String rawId = trimmed.substring(firstQuote + 1, lastQuote);
                        if (rawId.contains(":")) {
                            return rawId.split(":")[1];
                        }
                        return rawId;
                    }
                }
            }
        } catch (Exception ignored) {}

        String name = file.getName();
        if (name.endsWith(".lua")) name = name.substring(0, name.length() - 4);
        if (name.contains("_")) {
            return name.substring(name.indexOf("_") + 1);
        }
        return name;
    }

    private void refreshFileList() {
        scriptFiles.clear();
        try {
            Path dir = Platform.getConfigFolder().resolve("vision_goggles").resolve("modules");
            File[] files = dir.toFile().listFiles();
            if (files != null) {
                Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (File f : files) {
                    if (f.isFile() && (f.getName().endsWith(".lua") || f.getName().endsWith(".txt"))) {
                        scriptFiles.add(f);
                    }
                }
            }
        } catch (Exception ignored) {}

        if (selectedFileIndex >= scriptFiles.size()) {
            selectedFileIndex = Math.max(0, scriptFiles.size() - 1);
        }
        loadFileContent();
    }

    private void loadFileContent() {
        currentFileLines.clear();
        scrollOffset = 0;
        if (selectedFileIndex >= 0 && selectedFileIndex < scriptFiles.size()) {
            try {
                currentFileLines = Files.readAllLines(scriptFiles.get(selectedFileIndex).toPath(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                currentFileLines.add("Error al leer archivo: " + e.getMessage());
            }
        }
        if (codeEditor != null) {
            codeEditor.setValue(String.join("\n", currentFileLines));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int leftListX = 15;
        int leftListY = 54;
        int listWidth = 145;

        if (mouseX >= leftListX && mouseX <= leftListX + listWidth && mouseY >= leftListY && mouseY <= this.height - 40) {
            int clickedIdx = (int) ((mouseY - leftListY) / 14);
            if (clickedIdx >= 0 && clickedIdx < scriptFiles.size()) {
                selectedFileIndex = clickedIdx;
                loadFileContent();
                if (isEditMode) {
                    isEditMode = false;
                    if (codeEditor != null) codeEditor.visible = false;
                    if (btnEditarGuardar != null) btnEditarGuardar.setMessage(Component.literal("✏️ Editar"));
                }
                statusMessage = "Archivo seleccionado: " + scriptFiles.get(selectedFileIndex).getName();
                statusColor = 0xFF55FFFF;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isEditMode) {
            if (delta > 0) {
                scrollOffset = Math.max(0, scrollOffset - 2);
            } else if (delta < 0) {
                int maxVisibleLines = (this.height - 85) / 10;
                int maxScroll = Math.max(0, currentFileLines.size() - maxVisibleLines);
                scrollOffset = Math.min(maxScroll, scrollOffset + 2);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.fill(10, 10, this.width - 10, this.height - 10, 0xEE101418);

        // Barra superior
        g.fill(10, 10, this.width - 10, 32, 0xFF1B232B);
        g.drawString(this.font, "§b§lTERMINAL DE SCRIPTS LUA §7- Vision Goggles", 20, 16, 0xFFFFFFFF, false);

        // Panel Izquierdo: Lista de Archivos
        int leftW = 145;
        g.fill(15, 38, 15 + leftW, this.height - 38, 0xFF14191E);
        g.drawString(this.font, "§eARCHIVOS DISPONIBLES:", 20, 42, 0xFFFFFFFF, false);

        for (int i = 0; i < scriptFiles.size(); i++) {
            File f = scriptFiles.get(i);
            int itemY = 56 + (i * 14);
            if (itemY > this.height - 50) break;

            boolean isSelected = (i == selectedFileIndex);
            if (isSelected) {
                g.fill(18, itemY - 2, 15 + leftW - 3, itemY + 11, 0xFF2A3D4D);
            }

            String prefix = isSelected ? "§a▶ " : "§7• ";
            String name = f.getName();
            if (name.length() > 18) name = name.substring(0, 16) + "..";
            g.drawString(this.font, prefix + name, 22, itemY, isSelected ? 0xFF00FFCC : 0xFFAAAAAA, false);
        }

        // Panel Derecho: Visor de Código Fuente
        int rightX = 15 + leftW + 10;
        int rightW = this.width - rightX - 15;
        int codeAreaY = 38;
        int codeAreaH = this.height - 75;

        g.fill(rightX, codeAreaY, rightX + rightW, codeAreaY + codeAreaH, 0xFF0A0D10);

        if (selectedFileIndex >= 0 && selectedFileIndex < scriptFiles.size()) {
            String activeName = scriptFiles.get(selectedFileIndex).getName();
            String modeIndicator = isEditMode ? " §c[MODO EDICIÓN ACTIVO]" : " §7[Solo lectura]";
            g.drawString(this.font, "§6Visualizando: §f" + activeName + modeIndicator, rightX + 6, codeAreaY + 5, 0xFFFFFFFF, false);
        }

        // Si NO estamos en modo edición, dibujamos el visor con coloreado de sintaxis y scroll
        if (!isEditMode) {
            int lineY = codeAreaY + 20;
            int maxVisible = (codeAreaH - 25) / 10;

            for (int i = scrollOffset; i < currentFileLines.size() && (i - scrollOffset) < maxVisible; i++) {
                String lineNum = String.format("%02d", i + 1);
                String lineText = currentFileLines.get(i).replace("\t", "    ");

                g.drawString(this.font, "§8" + lineNum + " ", rightX + 6, lineY, 0xFF666666, false);

                int textColor = 0xFFCCCCCC;
                if (lineText.trim().startsWith("--") || lineText.trim().startsWith("==")) {
                    textColor = 0xFF55AA55;
                } else if (lineText.contains("function") || lineText.contains("module")) {
                    textColor = 0xFF00FFAA;
                }

                int maxWidth = rightW - 35;
                String trimmedText = this.font.plainSubstrByWidth(lineText, maxWidth);
                g.drawString(this.font, trimmedText, rightX + 28, lineY, textColor, false);

                lineY += 10;
            }
        }

        // Barra de estado inferior
        g.drawString(this.font, statusMessage, 20, this.height - 45, statusColor, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
