package dev.itsrealperson.vision_goggles.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class ClientGuiHelper {

    public static void openLuaScreen() {
        Minecraft.getInstance().setScreen(new LuaScriptViewerScreen());
    }
}
