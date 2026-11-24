package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/**
 * Reflection-only helper to detect active Iris/Oculus shaderpacks at runtime
 * without introducing any compile-time dependency.
 */
public final class IrisCompat {
    private static boolean lookupAttempted = false;
    private static Object irisApiInstance = null;
    private static Method isShaderPackInUseMethod = null;
    private static boolean warnedThisSession = false;

    private IrisCompat() {}

    public static boolean isShaderPackInUse() {
        if (!lookupAttempted) {
            lookupAttempted = true;
            resolveApi("net.irisshaders.iris.api.v0.IrisApi");
            if (irisApiInstance == null) {
                resolveApi("net.coderbot.iris.api.v0.IrisApi");
            }
        }
        if (irisApiInstance == null || isShaderPackInUseMethod == null) {
            return false;
        }
        try {
            Object result = isShaderPackInUseMethod.invoke(irisApiInstance);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void warnIfShaderPackActiveOnce(Player player) {
        if (warnedThisSession || player == null) {
            return;
        }
        if (isShaderPackInUse()) {
            warnedThisSession = true;
            player.displayClientMessage(
                    Component.translatable("message.vision_goggles.iris_shaderpack_warning")
                            .withStyle(ChatFormatting.YELLOW),
                    false
            );
            Vision_goggles.LOGGER.info("[Vision Goggles] Active Iris/Oculus shaderpack detected; warned user once.");
        }
    }

    private static void resolveApi(String className) {
        try {
            Class<?> apiClass = Class.forName(className);
            Method getInstance = apiClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            if (instance != null) {
                Method inUse = apiClass.getMethod("isShaderPackInUse");
                irisApiInstance = instance;
                isShaderPackInUseMethod = inUse;
            }
        } catch (Throwable ignored) {
            // Iris/Oculus not installed or API class not present
        }
    }
}
