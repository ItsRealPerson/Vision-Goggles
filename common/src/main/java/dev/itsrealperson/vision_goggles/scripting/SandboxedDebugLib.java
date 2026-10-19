package dev.itsrealperson.vision_goggles.scripting;

import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;

public class SandboxedDebugLib extends DebugLib {
    public static final int MAX_INSTRUCTIONS = 100_000;
    private static final ThreadLocal<Integer> INSTRUCTION_COUNT = ThreadLocal.withInitial(() -> 0);

    public static class ScriptQuotaExceededError extends Error {
        public ScriptQuotaExceededError(String message) {
            super(message);
        }
    }

    public static void resetInstructionCount() {
        INSTRUCTION_COUNT.set(0);
    }

    @Override
    public void onInstruction(int pc, Varargs varargs, int top) {
        int count = INSTRUCTION_COUNT.get() + 1;
        if (count > MAX_INSTRUCTIONS) {
            INSTRUCTION_COUNT.set(0);
            throw new ScriptQuotaExceededError("Script execution exceeded limit of " + MAX_INSTRUCTIONS + " instructions (potential infinite loop or DoS).");
        }
        INSTRUCTION_COUNT.set(count);
        super.onInstruction(pc, varargs, top);
    }
}
