package dev.itsrealperson.vision_goggles.scripting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;
import java.util.List;

public class LuaHudCanvas {

    public interface DrawCommand {
        void execute(GuiGraphics g, Minecraft mc);
    }

    private static final List<DrawCommand> FRAME_COMMANDS = new ArrayList<>();

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

                synchronized (FRAME_COMMANDS) {
                    FRAME_COMMANDS.add((g, mc) -> {
                        g.pose().pushPose();
                        g.pose().translate(x, y, 0);
                        if (scale != 1.0f) {
                            g.pose().scale(scale, scale, 1.0f);
                        }
                        g.drawString(mc.font, text, 0, 0, color, false);
                        g.pose().popPose();
                    });
                }
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

                synchronized (FRAME_COMMANDS) {
                    FRAME_COMMANDS.add((g, mc) -> g.fill(x, y, x + w, y + h, color));
                }
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

                synchronized (FRAME_COMMANDS) {
                    FRAME_COMMANDS.add((g, mc) -> {
                        // Background outline
                        g.fill(x, y, x + w, y + h, 0x88111111);
                        // Progress fill
                        int fillW = (int) (w * progress);
                        if (fillW > 0) {
                            g.fill(x, y, x + fillW, y + h, color);
                        }
                    });
                }
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

        // Draw a line between two screen points (approximated as thin rectangles)
        hud.set("drawLine", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                final int x1 = args.checkint(1);
                final int y1 = args.checkint(2);
                final int x2 = args.checkint(3);
                final int y2 = args.checkint(4);
                final int color = parseColor(args.optjstring(5, "#FFFFFF"));

                synchronized (FRAME_COMMANDS) {
                    FRAME_COMMANDS.add((g, mc) -> {
                        int dx = x2 - x1;
                        int dy = y2 - y1;
                        float len = (float) Math.sqrt(dx * dx + dy * dy);
                        if (len < 1) return;
                        int steps = (int) Math.ceil(len);
                        for (int i = 0; i <= steps; i++) {
                            float t = i / (float) steps;
                            int px = Math.round(x1 + dx * t);
                            int py = Math.round(y1 + dy * t);
                            g.fill(px, py, px + 1, py + 1, color);
                        }
                    });
                }
                return LuaValue.NIL;
            }
        });

        // Draw an approximated circle outline at (cx, cy) with radius
        hud.set("drawCircle", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                final int cx = args.checkint(1);
                final int cy = args.checkint(2);
                final int radius = args.checkint(3);
                final int color = parseColor(args.optjstring(4, "#FFFFFF"));

                synchronized (FRAME_COMMANDS) {
                    FRAME_COMMANDS.add((g, mc) -> {
                        int segments = Math.max(16, radius * 2);
                        double step = 2.0 * Math.PI / segments;
                        for (int i = 0; i < segments; i++) {
                            double angle = i * step;
                            int px = cx + (int) Math.round(Math.cos(angle) * radius);
                            int py = cy + (int) Math.round(Math.sin(angle) * radius);
                            g.fill(px, py, px + 1, py + 1, color);
                        }
                    });
                }
                return LuaValue.NIL;
            }
        });

        // Draw a filled circle (disc)
        hud.set("drawDisc", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                final int cx = args.checkint(1);
                final int cy = args.checkint(2);
                final int radius = args.checkint(3);
                final int color = parseColor(args.optjstring(4, "#FFFFFF"));

                synchronized (FRAME_COMMANDS) {
                    FRAME_COMMANDS.add((g, mc) -> {
                        for (int dy2 = -radius; dy2 <= radius; dy2++) {
                            int halfW = (int) Math.round(Math.sqrt(radius * radius - dy2 * dy2));
                            g.fill(cx - halfW, cy + dy2, cx + halfW + 1, cy + dy2 + 1, color);
                        }
                    });
                }
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
        if (hex == null || hex.isEmpty()) return 0xFFFFFFFF;
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            if (hex.length() == 6) {
                return (int) (0xFF000000L | Long.parseLong(hex, 16));
            } else if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (Exception ignored) {}
        return 0xFFFFFFFF;
    }
}
