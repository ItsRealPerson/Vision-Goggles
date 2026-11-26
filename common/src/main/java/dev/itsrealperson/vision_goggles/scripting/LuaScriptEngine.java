package dev.itsrealperson.vision_goggles.scripting;

import dev.architectury.platform.Platform;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.fabricmc.api.EnvType;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class LuaScriptEngine {

    private static Globals globals;
    private static final Map<String, Map<String, Object>> EXTERNAL_GLOBAL_TABLES = new ConcurrentHashMap<>();

    public static synchronized void init() {
        try {
            globals = JsePlatform.standardGlobals();
            patchStringFormat(globals);

            // Enforce instruction count hook (100,000 instructions limit per execution)
            SandboxedDebugLib debugLib = new SandboxedDebugLib();
            globals.load(debugLib);

            // Sanitize globals: nullify dangerous tables and functions
            globals.set("luajava", LuaValue.NIL);
            globals.set("package", LuaValue.NIL);
            globals.set("require", LuaValue.NIL);
            globals.set("io", LuaValue.NIL);
            globals.set("loadfile", LuaValue.NIL);
            globals.set("dofile", LuaValue.NIL);
            globals.set("debug", LuaValue.NIL);

            // Sanitize 'os' table: retain only safe clock/time functions
            LuaValue os = globals.get("os");
            if (os.istable()) {
                LuaTable safeOs = new LuaTable();
                safeOs.set("clock", os.get("clock"));
                safeOs.set("time", os.get("time"));
                safeOs.set("difftime", os.get("difftime"));
                safeOs.set("date", os.get("date"));
                globals.set("os", safeOs);
            }

            // Register global utility functions
            globals.set("getTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (Platform.getEnv() == EnvType.CLIENT) {
                        try {
                            Class<?> bridge = Class.forName("dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge");
                            return LuaValue.valueOf((Double) bridge.getMethod("getClientTime").invoke(null));
                        } catch (Throwable ignored) {}
                    }
                    return LuaValue.valueOf(System.currentTimeMillis() / 1000.0);
                }
            });

            globals.set("printLog", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Vision_goggles.LOGGER.info("[Vision Goggles Lua] " + arg.tojstring());
                    return LuaValue.NIL;
                }
            });

            // Register client-specific sub-APIs only if running on a client environment
            if (Platform.getEnv() == EnvType.CLIENT) {
                try {
                    Class<?> bridge = Class.forName("dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge");
                    bridge.getMethod("registerClientApis", Globals.class).invoke(null, globals);
                } catch (Throwable t) {
                    Vision_goggles.LOGGER.error("[Vision Goggles] Failed to register client Lua APIs: " + t.getMessage());
                }
            }

            // Re-apply any external global tables registered by addon mods
            for (Map.Entry<String, Map<String, Object>> entry : EXTERNAL_GLOBAL_TABLES.entrySet()) {
                applyGlobalTableToGlobals(globals, entry.getKey(), entry.getValue());
            }

            Vision_goggles.LOGGER.info("[Vision Goggles] LuaScriptEngine initialized successfully with sandbox protection.");
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to initialize LuaScriptEngine: " + e.getMessage(), e);
        }
    }

    public static Globals getGlobals() {
        if (globals == null) {
            init();
        }
        return globals;
    }

    /**
     * Registers a global Lua table using standard Java types ({@link Map}, {@link Supplier}, {@link Function})
     * so external addon mods never need to link directly against {@code org.luaj.vm2.*} (which is shaded and
     * relocated in production JARs).
     */
    public static void registerGlobalTable(String tableName, Map<String, Object> members) {
        if (tableName == null || tableName.isBlank() || members == null) {
            return;
        }
        EXTERNAL_GLOBAL_TABLES.put(tableName, members);
        Globals g = getGlobals();
        if (g != null) {
            applyGlobalTableToGlobals(g, tableName, members);
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyGlobalTableToGlobals(Globals g, String tableName, Map<String, Object> members) {
        LuaTable table = new LuaTable();
        for (Map.Entry<String, Object> entry : members.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (key == null || val == null) continue;
            if (val instanceof Supplier<?> supplier) {
                table.set(key, new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        try {
                            return toLuaValue(supplier.get());
                        } catch (Throwable t) {
                            return LuaValue.NIL;
                        }
                    }
                });
            } else if (val instanceof Function<?, ?> fn) {
                Function<Object, Object> rawFn = (Function<Object, Object>) fn;
                table.set(key, new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        try {
                            return toLuaValue(rawFn.apply(fromLuaValue(arg)));
                        } catch (Throwable t) {
                            return LuaValue.NIL;
                        }
                    }
                });
            } else {
                table.set(key, toLuaValue(val));
            }
        }
        g.set(tableName, table);
    }

    public static Object fromLuaValue(LuaValue v) {
        if (v == null || v.isnil()) return null;
        if (v.isboolean()) return v.toboolean();
        if (v.isnumber()) return v.todouble();
        if (v.isstring()) return v.tojstring();
        return v.tojstring();
    }

    public static LuaValue toLuaValue(Object obj) {
        if (obj == null) return LuaValue.NIL;
        if (obj instanceof LuaValue lv) return lv;
        if (obj instanceof Boolean b) return LuaValue.valueOf(b);
        if (obj instanceof Integer i) return LuaValue.valueOf(i);
        if (obj instanceof Long l) return LuaValue.valueOf(l.doubleValue());
        if (obj instanceof Float f) return LuaValue.valueOf(f.doubleValue());
        if (obj instanceof Double d) return LuaValue.valueOf(d);
        if (obj instanceof Number n) return LuaValue.valueOf(n.doubleValue());
        if (obj instanceof String s) return LuaValue.valueOf(s);
        if (obj instanceof Map<?, ?> map) {
            LuaTable t = new LuaTable();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null) {
                    t.set(String.valueOf(e.getKey()), toLuaValue(e.getValue()));
                }
            }
            return t;
        }
        if (obj instanceof List<?> list) {
            LuaTable t = new LuaTable();
            for (int i = 0; i < list.size(); i++) {
                t.set(i + 1, toLuaValue(list.get(i)));
            }
            return t;
        }
        return LuaValue.valueOf(String.valueOf(obj));
    }

    public static LuaValue executeString(String code) {
        try {
            SandboxedDebugLib.resetInstructionCount();
            LuaValue chunk = getGlobals().load(code);
            return chunk.call();
        } catch (LuaError error) {
            Vision_goggles.LOGGER.error("[Vision Goggles Lua Error] " + error.getMessage());
            return LuaValue.valueOf("ERROR: " + error.getMessage());
        } catch (Throwable e) {
            Vision_goggles.LOGGER.error("[Vision Goggles Lua Error/Exception] " + e.getMessage());
            return LuaValue.valueOf("ERROR: " + e.getMessage());
        }
    }

    /**
     * LuaJ 3.0.1's built-in StringLib ignores floating-point precision in {@code string.format}
     * (e.g. {@code "%.0fm"} formats {@code 25.557666} as {@code "25.557666361660257m"}).
     * Replace {@code string.format} with a Locale.ROOT formatter preserving Lua semantics.
     */
    public static void patchStringFormat(Globals g) {
        LuaValue strVal = g.get("string");
        if (!strVal.istable()) return;
        LuaTable strTable = strVal.checktable();
        final LuaValue origFormat = strTable.get("format");

        strTable.set("format", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String fmt = args.checkjstring(1);
                StringBuilder sb = new StringBuilder(fmt.length() + 16);
                int argIdx = 2;
                int i = 0;
                int len = fmt.length();

                while (i < len) {
                    int pct = fmt.indexOf('%', i);
                    if (pct < 0) {
                        sb.append(fmt, i, len);
                        break;
                    }
                    sb.append(fmt, i, pct);
                    if (pct + 1 >= len) {
                        throw new LuaError("invalid option '%' to 'format'");
                    }
                    char next = fmt.charAt(pct + 1);
                    if (next == '%') {
                        sb.append('%');
                        i = pct + 2;
                        continue;
                    }

                    int pos = pct + 1;
                    StringBuilder flags = new StringBuilder();
                    while (pos < len && "-+ #0".indexOf(fmt.charAt(pos)) >= 0) {
                        flags.append(fmt.charAt(pos++));
                    }
                    StringBuilder width = new StringBuilder();
                    while (pos < len && Character.isDigit(fmt.charAt(pos))) {
                        width.append(fmt.charAt(pos++));
                    }
                    StringBuilder prec = new StringBuilder();
                    boolean hasPrec = false;
                    if (pos < len && fmt.charAt(pos) == '.') {
                        hasPrec = true;
                        pos++;
                        while (pos < len && Character.isDigit(fmt.charAt(pos))) {
                            prec.append(fmt.charAt(pos++));
                        }
                        if (prec.length() == 0) {
                            prec.append('0');
                        }
                    }
                    if (pos >= len) {
                        throw new LuaError("invalid conversion to 'format'");
                    }
                    char conv = fmt.charAt(pos++);
                    if (argIdx > args.narg()) {
                        throw new LuaError("bad argument #" + argIdx + " to 'format' (no value)");
                    }

                    StringBuilder spec = new StringBuilder("%").append(flags).append(width);
                    try {
                        switch (conv) {
                            case 'd': case 'i': {
                                long v = (long) args.checkdouble(argIdx++);
                                if (hasPrec && width.length() == 0 && flags.indexOf("0") < 0) {
                                    spec = new StringBuilder("%").append(flags).append('0').append(prec);
                                }
                                spec.append('d');
                                sb.append(String.format(Locale.ROOT, spec.toString(), v));
                                break;
                            }
                            case 'u': {
                                long v = (long) args.checkdouble(argIdx++);
                                if (v < 0) v = v & 0xFFFFFFFFL;
                                if (hasPrec && width.length() == 0 && flags.indexOf("0") < 0) {
                                    spec = new StringBuilder("%").append(flags).append('0').append(prec);
                                }
                                spec.append('d');
                                sb.append(String.format(Locale.ROOT, spec.toString(), v));
                                break;
                            }
                            case 'o': case 'x': case 'X': {
                                long v = (long) args.checkdouble(argIdx++);
                                if (hasPrec && width.length() == 0 && flags.indexOf("0") < 0) {
                                    spec = new StringBuilder("%").append(flags).append('0').append(prec);
                                }
                                spec.append(conv);
                                sb.append(String.format(Locale.ROOT, spec.toString(), v));
                                break;
                            }
                            case 'f': case 'e': case 'E': case 'g': case 'G': {
                                if (hasPrec) spec.append('.').append(prec);
                                spec.append(conv);
                                double v = args.checkdouble(argIdx++);
                                sb.append(String.format(Locale.ROOT, spec.toString(), v));
                                break;
                            }
                            case 'c': {
                                int ch = (int) args.checkdouble(argIdx++);
                                spec.append('c');
                                sb.append(String.format(Locale.ROOT, spec.toString(), (char) ch));
                                break;
                            }
                            case 's': {
                                LuaValue v = args.arg(argIdx++);
                                String s = v.isstring() ? v.tojstring() : g.get("tostring").call(v).tojstring();
                                if (flags.length() == 0 && width.length() == 0 && !hasPrec) {
                                    sb.append(s);
                                } else {
                                    if (hasPrec) spec.append('.').append(prec);
                                    spec.append('s');
                                    sb.append(String.format(Locale.ROOT, spec.toString(), s));
                                }
                                break;
                            }
                            case 'q': {
                                LuaValue v = args.arg(argIdx++);
                                if (!origFormat.isnil()) {
                                    sb.append(origFormat.call(LuaValue.valueOf("%q"), v).tojstring());
                                } else {
                                    String raw = v.tojstring().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\\n");
                                    sb.append('"').append(raw).append('"');
                                }
                                break;
                            }
                            default:
                                throw new LuaError("invalid option '%" + conv + "' to 'format'");
                        }
                    } catch (LuaError le) {
                        throw le;
                    } catch (Exception ex) {
                        throw new LuaError("invalid format specifier '%" + conv + "': " + ex.getMessage());
                    }
                    i = pos;
                }
                return LuaValue.valueOf(sb.toString());
            }
        });
    }
}
