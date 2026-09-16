package dev.itsrealperson.vision_goggles.scripting;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.PostPass;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.List;

public class LuaUniformBridge {

    public static LuaTable createBridge(List<PostPass> passes) {
        LuaTable uniforms = new LuaTable();

        uniforms.set("setFloat", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String name = args.checkjstring(1);
                float val = (float) args.checkdouble(2);
                for (PostPass pass : passes) {
                    Uniform u = pass.getEffect().getUniform(name);
                    if (u != null) {
                        u.set(val);
                    }
                }
                return LuaValue.NIL;
            }
        });

        uniforms.set("setVec2", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String name = args.checkjstring(1);
                float x = (float) args.checkdouble(2);
                float y = (float) args.checkdouble(3);
                for (PostPass pass : passes) {
                    Uniform u = pass.getEffect().getUniform(name);
                    if (u != null) {
                        u.set(x, y);
                    }
                }
                return LuaValue.NIL;
            }
        });

        uniforms.set("setVec3", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String name = args.checkjstring(1);
                float r = (float) args.checkdouble(2);
                float g = (float) args.checkdouble(3);
                float b = (float) args.checkdouble(4);
                for (PostPass pass : passes) {
                    Uniform u = pass.getEffect().getUniform(name);
                    if (u != null) {
                        u.set(r, g, b);
                    }
                }
                return LuaValue.NIL;
            }
        });

        uniforms.set("setVec4", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String name = args.checkjstring(1);
                float r = (float) args.checkdouble(2);
                float g = (float) args.checkdouble(3);
                float b = (float) args.checkdouble(4);
                float a = (float) args.checkdouble(5);
                for (PostPass pass : passes) {
                    Uniform u = pass.getEffect().getUniform(name);
                    if (u != null) {
                        u.set(r, g, b, a);
                    }
                }
                return LuaValue.NIL;
            }
        });

        return uniforms;
    }
}
