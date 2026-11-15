package dev.itsrealperson.vision_goggles.scripting;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class LuaHudCanvas {

    public interface DrawCommand {
        void execute(GuiGraphics g, Minecraft mc);
    }

    public static final int MAX_FRAME_COMMANDS = 500;
    private static final List<DrawCommand> FRAME_COMMANDS = new ArrayList<>();

    private static boolean queueCommand(DrawCommand cmd) {
        synchronized (FRAME_COMMANDS) {
            if (FRAME_COMMANDS.size() < MAX_FRAME_COMMANDS) {
                FRAME_COMMANDS.add(cmd);
                return true;
            }
            return false;
        }
    }

    // Alert banner state
    private static String currentAlertText = null;
    private static int currentAlertColor = 0xFFFF2222;
    private static long alertEndTime = 0;

    public static void register(Globals globals) {
        LuaTable hud = new LuaTable();

        hud.set("getWidth", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.getWindow().getGuiScaledWidth());
            }
        });

        hud.set("getHeight", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                return LuaValue.valueOf(mc.getWindow().getGuiScaledHeight());
            }
        });

        hud.set("drawText", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int x = args.checkint(1);
                int y = args.checkint(2);
                String text = args.checkjstring(3);
                int color = parseColor(args.optjstring(4, "#00FF00"));
                float scale = (float) args.optdouble(5, 1.0);

                queueCommand((g, mc) -> {
                    g.pose().pushPose();
                    g.pose().translate(x, y, 0);
                    if (scale != 1.0f) {
                        g.pose().scale(scale, scale, 1.0f);
                    }
                    g.drawString(mc.font, text, 0, 0, color, false);
                    g.pose().popPose();
                });
                return LuaValue.NIL;
            }
        });

        hud.set("drawRect", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int x = args.checkint(1);
                int y = args.checkint(2);
                int w = args.checkint(3);
                int h = args.checkint(4);
                int color = parseColor(args.optjstring(5, "#FFFFFF"));

                queueCommand((g, mc) -> g.fill(x, y, x + w, y + h, color));
                return LuaValue.NIL;
            }
        });

        hud.set("drawProgressBar", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int x = args.checkint(1);
                int y = args.checkint(2);
                int w = args.checkint(3);
                int h = args.checkint(4);
                float progress = Math.max(0.0f, Math.min(1.0f, (float) args.checkdouble(5)));
                int color = parseColor(args.optjstring(6, "#00FF88"));

                queueCommand((g, mc) -> {
                    // Background outline
                    g.fill(x, y, x + w, y + h, 0x88111111);
                    // Progress fill
                    int fillW = (int) (w * progress);
                    if (fillW > 0) {
                        g.fill(x, y, x + fillW, y + h, color);
                    }
                });
                return LuaValue.NIL;
            }
        });

        hud.set("showAlert", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String text = args.checkjstring(1);
                int color = parseColor(args.optjstring(2, "#FF3333"));
                double durationSec = args.optdouble(3, 2.5);

                currentAlertText = text;
                currentAlertColor = color;
                alertEndTime = System.currentTimeMillis() + (long)(durationSec * 1000);

                return LuaValue.NIL;
            }
        });

        // Draw a line between two screen points as a single 1px-thick oriented quad
        hud.set("drawLine", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                final int x1 = args.checkint(1);
                final int y1 = args.checkint(2);
                final int x2 = args.checkint(3);
                final int y2 = args.checkint(4);
                final int color = parseColor(args.optjstring(5, "#FFFFFF"));

                queueCommand((g, mc) -> {
                    float dx = x2 - x1;
                    float dy = y2 - y1;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len < 0.001f) return;

                    float nx = -dy / len * 0.5f;
                    float ny = dx / len * 0.5f;

                    int a = (color >> 24) & 0xFF;
                    int r = (color >> 16) & 0xFF;
                    int gr = (color >> 8) & 0xFF;
                    int b = color & 0xFF;

                    Matrix4f matrix = g.pose().last().pose();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableCull();
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);

                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder builder = tesselator.getBuilder();
                    builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    builder.vertex(matrix, x1 + nx, y1 + ny, 0).color(r, gr, b, a).endVertex();
                    builder.vertex(matrix, x1 - nx, y1 - ny, 0).color(r, gr, b, a).endVertex();
                    builder.vertex(matrix, x2 - nx, y2 - ny, 0).color(r, gr, b, a).endVertex();
                    builder.vertex(matrix, x2 + nx, y2 + ny, 0).color(r, gr, b, a).endVertex();
                    tesselator.end();

                    RenderSystem.enableCull();
                    RenderSystem.disableBlend();
                });
                return LuaValue.NIL;
            }
        });

        // Draw a 32-segment polygon circle outline at (cx, cy) with radius in a single BufferBuilder batch
        hud.set("drawCircle", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                final int cx = args.checkint(1);
                final int cy = args.checkint(2);
                int r = args.checkint(3);
                final int radius = Math.min(512, Math.max(1, r));
                final int color = parseColor(args.optjstring(4, "#FFFFFF"));

                queueCommand((g, mc) -> {
                    final int segments = 32;
                    final float rIn = radius - 0.5f;
                    final float rOut = radius + 0.5f;
                    final double step = (2.0 * Math.PI) / segments;

                    int a = (color >> 24) & 0xFF;
                    int red = (color >> 16) & 0xFF;
                    int gr = (color >> 8) & 0xFF;
                    int b = color & 0xFF;

                    Matrix4f matrix = g.pose().last().pose();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableCull();
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);

                    Tesselator tesselator = Tesselator.getInstance();
                    BufferBuilder builder = tesselator.getBuilder();
                    builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    for (int i = 0; i < segments; i++) {
                        double a1 = i * step;
                        double a2 = (i + 1) * step;
                        float cos1 = (float) Math.cos(a1);
                        float sin1 = (float) Math.sin(a1);
                        float cos2 = (float) Math.cos(a2);
                        float sin2 = (float) Math.sin(a2);

                        builder.vertex(matrix, cx + cos1 * rIn, cy + sin1 * rIn, 0).color(red, gr, b, a).endVertex();
                        builder.vertex(matrix, cx + cos1 * rOut, cy + sin1 * rOut, 0).color(red, gr, b, a).endVertex();
                        builder.vertex(matrix, cx + cos2 * rOut, cy + sin2 * rOut, 0).color(red, gr, b, a).endVertex();
                        builder.vertex(matrix, cx + cos2 * rIn, cy + sin2 * rIn, 0).color(red, gr, b, a).endVertex();
                    }
                    tesselator.end();

                    RenderSystem.enableCull();
                    RenderSystem.disableBlend();
                });
                return LuaValue.NIL;
            }
        });

        // Draw a filled circle (disc)
        hud.set("drawDisc", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                final int cx = args.checkint(1);
                final int cy = args.checkint(2);
                int r = args.checkint(3);
                final int radius = Math.min(512, Math.max(1, r));
                final int color = parseColor(args.optjstring(4, "#FFFFFF"));

                queueCommand((g, mc) -> {
                    for (int dy2 = -radius; dy2 <= radius; dy2++) {
                        int halfW = (int) Math.round(Math.sqrt(radius * radius - dy2 * dy2));
                        g.fill(cx - halfW, cy + dy2, cx + halfW + 1, cy + dy2 + 1, color);
                    }
                });
                return LuaValue.NIL;
            }
        });

        globals.set("hud", hud);
    }

    public static void render(GuiGraphics g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Render queued frame commands
        synchronized (FRAME_COMMANDS) {
            for (DrawCommand cmd : FRAME_COMMANDS) {
                try {
                    cmd.execute(g, mc);
                } catch (Exception ignored) {}
            }
            FRAME_COMMANDS.clear();
        }

        // Render active tactical alert banner
        if (currentAlertText != null && System.currentTimeMillis() < alertEndTime) {
            long remaining = alertEndTime - System.currentTimeMillis();
            // Blinking effect (cycle every 300ms)
            boolean blinkOn = (remaining / 300) % 2 == 0;
            if (blinkOn) {
                int screenWidth = mc.getWindow().getGuiScaledWidth();
                int textW = mc.font.width(currentAlertText);
                int cx = (screenWidth - textW) / 2;
                int cy = 35;

                // Translucent background
                g.fill(cx - 10, cy - 4, cx + textW + 10, cy + 14, 0xAA000000);
                // Warning text
                g.drawString(mc.font, currentAlertText, cx, cy, currentAlertColor, true);
            }
        } else {
            currentAlertText = null;
        }
    }

    public static int parseColor(String hex) {
        return LuaCustomModule.parseColor(hex);
    }
}
