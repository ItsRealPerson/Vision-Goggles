package dev.itsrealperson.vision_goggles.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.platform.Platform;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.network.OpenLuaScreenPacket;
import dev.itsrealperson.vision_goggles.scripting.LuaCustomModule;
import dev.itsrealperson.vision_goggles.scripting.LuaModuleManager;
import dev.itsrealperson.vision_goggles.scripting.LuaScriptEngine;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.LuaValue;

import java.nio.file.Path;
import java.util.Collection;

public class CommandVisionGoggles {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vg")
            .executes(context -> openGui(context.getSource()))
            .then(Commands.literal("gui")
                .executes(context -> openGui(context.getSource()))
            )
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    LuaModuleManager.reload();
                    context.getSource().sendSuccess(() -> Component.literal("§a[Vision Goggles] Módulos Lua recargados con éxito."), false);
                    return 1;
                })
            )
            .then(Commands.literal("list")
                .executes(context -> {
                    Collection<LuaCustomModule> modules = LuaModuleManager.getLoadedModules();
                    context.getSource().sendSuccess(() -> Component.literal("§b=== MÓDULOS LUA CARGADOS (" + modules.size() + ") ==="), false);
                    if (modules.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("§7(No hay módulos activos en config/vision_goggles/modules/)"), false);
                    } else {
                        for (LuaCustomModule m : modules) {
                            context.getSource().sendSuccess(() -> Component.literal("§a• §f" + m.getName() + " §7[" + m.getType() + "] §8(" + m.getId() + ")"), false);
                        }
                    }
                    return 1;
                })
            )
            .then(Commands.literal("folder")
                .executes(context -> {
                    if (Platform.getEnv() != net.fabricmc.api.EnvType.CLIENT) {
                        context.getSource().sendFailure(Component.literal("§c[Vision Goggles] Este comando solo puede abrir carpetas en el cliente local."));
                        return 0;
                    }
                    try {
                        Path dir = Platform.getConfigFolder().resolve("vision_goggles").resolve("modules");
                        Util.getPlatform().openFile(dir.toFile());
                        context.getSource().sendSuccess(() -> Component.literal("§a[Vision Goggles] Abriendo carpeta de scripts en el Explorador..."), false);
                    } catch (Exception e) {
                        context.getSource().sendFailure(Component.literal("§cError abriendo carpeta: " + e.getMessage()));
                    }
                    return 1;
                })
            )
            .then(Commands.literal("slots")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("modular")
                    .executes(ctx -> {
                        int current = dev.itsrealperson.vision_goggles.item.ModularGogglesItem.getCustomModularMax();
                        if (current <= 0) current = dev.itsrealperson.vision_goggles.util.ModConfig.getModularMaxModules();
                        final int c = current;
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[Vision Goggles] Capacidad de Gafas Modulares: §e" + c + " ranuras."), false);
                        return 1;
                    })
                    .then(Commands.argument("cantidad", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 16))
                        .executes(ctx -> {
                            int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "cantidad");
                            dev.itsrealperson.vision_goggles.item.ModularGogglesItem.setCustomModularMax(count);
                            dev.itsrealperson.vision_goggles.util.ModConfig.common.modularMaxModules = count;
                            dev.itsrealperson.vision_goggles.util.ModConfig.save();
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Vision Goggles] Capacidad de Gafas Modulares actualizada a: §e" + count + " ranuras."), true);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("pro")
                    .executes(ctx -> {
                        int current = dev.itsrealperson.vision_goggles.item.ModularGogglesItem.getCustomProMax();
                        if (current <= 0) current = dev.itsrealperson.vision_goggles.util.ModConfig.getProModularMaxModules();
                        final int c = current;
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[Vision Goggles] Capacidad de Gafas Pro: §e" + c + " ranuras."), false);
                        return 1;
                    })
                    .then(Commands.argument("cantidad", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 16))
                        .executes(ctx -> {
                            int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "cantidad");
                            dev.itsrealperson.vision_goggles.item.ModularGogglesItem.setCustomProMax(count);
                            dev.itsrealperson.vision_goggles.util.ModConfig.common.proModularMaxModules = count;
                            dev.itsrealperson.vision_goggles.util.ModConfig.save();
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Vision Goggles] Capacidad de Gafas Pro actualizada a: §e" + count + " ranuras."), true);
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("run")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("code", StringArgumentType.greedyString())
                    .executes(context -> {
                        String code = StringArgumentType.getString(context, "code");
                        LuaValue result = LuaScriptEngine.executeString(code);
                        context.getSource().sendSuccess(() -> Component.literal("§e[Lua Result] §f" + result.tojstring()), false);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("givescript")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (LuaCustomModule mod : LuaModuleManager.getLoadedModules()) {
                            if (mod.getId() != null) {
                                builder.suggest(mod.getId().getPath(), Component.literal(mod.getName()));
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String id = StringArgumentType.getString(ctx, "id");
                        return giveScript(ctx.getSource(), id, id, "#00FFCC");
                    })
                    .then(Commands.argument("nombre", StringArgumentType.string())
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            String name = StringArgumentType.getString(ctx, "nombre");
                            return giveScript(ctx.getSource(), id, name, "#00FFCC");
                        })
                        .then(Commands.argument("color", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                builder.suggest("#00FF88", Component.literal("Verde Táctico"));
                                builder.suggest("#00FFCC", Component.literal("Cian Táctico"));
                                builder.suggest("#00DDFF", Component.literal("Azul Información"));
                                builder.suggest("#FFAA00", Component.literal("Naranja Advertencia"));
                                builder.suggest("#FF2200", Component.literal("Rojo Peligro"));
                                builder.suggest("#FF00FF", Component.literal("Magenta"));
                                builder.suggest("#FFFF00", Component.literal("Amarillo"));
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                String id = StringArgumentType.getString(ctx, "id");
                                String name = StringArgumentType.getString(ctx, "nombre");
                                String color = StringArgumentType.getString(ctx, "color");
                                return giveScript(ctx.getSource(), id, name, color);
                            })
                        )
                    )
                )
            )
        );
    }

    private static int giveScript(CommandSourceStack source, String id, String name, String color) {
        try {
            if (source.getEntity() instanceof ServerPlayer player) {
                // Auto-fill module name if not specified
                if (name == null || name.isEmpty() || name.equals(id)) {
                    LuaCustomModule loaded = LuaModuleManager.findModuleByIdOrPath(id);
                    if (loaded != null && loaded.getName() != null) {
                        name = loaded.getName();
                    }
                }
                ItemStack scriptStack = dev.itsrealperson.vision_goggles.item.InstallationScriptItem.create(id, name, color);
                if (!player.getInventory().add(scriptStack)) {
                    player.drop(scriptStack, false);
                }
                final String finalName = name;
                source.sendSuccess(() -> Component.literal("§a[Vision Goggles] Se ha generado el Script de Instalación: §e" + finalName + " §7(" + id + ")"), false);
                return 1;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError generando script: " + e.getMessage()));
        }
        return 0;
    }

    private static int openGui(CommandSourceStack source) {
        try {
            if (source.getEntity() instanceof ServerPlayer player) {
                NetworkManager.INSTANCE.sendToPlayer(player, new OpenLuaScreenPacket());
                return 1;
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError al abrir la terminal: " + e.getMessage()));
        }
        return 0;
    }
}
