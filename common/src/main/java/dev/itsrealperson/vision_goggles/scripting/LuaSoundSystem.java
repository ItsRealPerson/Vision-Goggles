package dev.itsrealperson.vision_goggles.scripting;

import dev.itsrealperson.vision_goggles.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class LuaSoundSystem {

    public static void register(Globals globals) {
        LuaTable sound = new LuaTable();

        sound.set("play", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String soundId = args.checkjstring(1);
                float pitch = (float) args.optdouble(2, 1.0);
                float volume = (float) args.optdouble(3, 1.0);

                Minecraft mc = Minecraft.getInstance();
                try {
                    ResourceLocation loc = new ResourceLocation(soundId);
                    SoundEvent evt = BuiltInRegistries.SOUND_EVENT.get(loc);
                    if (evt != null) {
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(evt, pitch, volume));
                    }
                } catch (Exception ignored) {}
                return LuaValue.NIL;
            }
        });

        sound.set("playBeep", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                float pitch = arg.isnumber() ? (float) arg.todouble() : 1.0f;
                Minecraft mc = Minecraft.getInstance();
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BLIP.get(), pitch, 0.7f));
                return LuaValue.NIL;
            }
        });

        sound.set("playAlert", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.8f, 1.0f));
                return LuaValue.NIL;
            }
        });

        sound.set("playClick", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                Minecraft mc = Minecraft.getInstance();
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.4f));
                return LuaValue.NIL;
            }
        });

        globals.set("sound", sound);
    }
}
