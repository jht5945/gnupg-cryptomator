package me.hatter.integrations.keychain;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author hatterjiang
 */
public class UtilsCommandResult {
    private final int exitValue;
    private final byte[] stdout;
    private final byte[] stderr;

    public UtilsCommandResult(int exitValue, byte[] stdout, byte[] stderr) {
        this.exitValue = exitValue;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public int getExitValue() {
        return exitValue;
    }

    public byte[] getStdout() {
        return stdout;
    }

    public byte[] getStderr() {
        return stderr;
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "exitValue=" + exitValue +
                ", stdout=" + Arrays.toString(stdout) + " (" + new String(stdout, StandardCharsets.UTF_8) + ")" +
                ", stderr=" + Arrays.toString(stderr) + " (" + new String(stderr, StandardCharsets.UTF_8) + ")" +
                '}';
    }
}
