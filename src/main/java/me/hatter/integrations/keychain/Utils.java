package me.hatter.integrations.keychain;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author hatterjiang
 */
public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    private static final String DEFAULT_GPG_COMMAND = "gpg";
    private static final String USER_HOME = System.getProperty("user.home");
    private static final File GNUPG_CONFIG_FILE1 = new File("/etc/cryptomator/config.json");
    private static final File GNUPG_CONFIG_FILE2 = new File(USER_HOME, ".config/cryptomator/config.json");
    private static final File DEFAULT_ENCRYPTION_KEY_BASE_PATH = new File(USER_HOME, ".config/cryptomator/keys/");

    public static boolean checkGnuPGReady(GnuPGConfig gnuPGConfig) {
        if (gnuPGConfig == null) {
            return false;
        }
        try {
            final UtilsCommandResult versionResult = runGpgPG(gnuPGConfig, null, "--version");
            if (versionResult.getExitValue() == 0) {
                return true;
            }
            LOG.warn("Check GnuPG not success: " + versionResult);
            return false;
        } catch (KeychainAccessException e) {
            LOG.warn("Check GnuPG failed", e);
            return false;
        }
    }

    public static GnuPGConfig loadGnuPGConfig() throws KeychainAccessException {
        final File configFile = getGnuPGConfigFile();
        final String configJson = readFile(configFile);
        final GnuPGConfig gnuPGConfig;
        try {
            gnuPGConfig = new Gson().fromJson(configJson, GnuPGConfig.class);
        } catch (Exception e) {
            throw new KeychainAccessException("Parse GnuPG config file: " + configFile + " failed", e);
        }
        if (StringUtils.isEmpty(gnuPGConfig.getKeyId())) {
            throw new KeychainAccessException("GnuPG key ID cannot be empty");
        }
        return gnuPGConfig;
    }

    public static void deletePassword(GnuPGConfig gnuPGConfig, String vault) {
        final File keyFile = getKeyFile(gnuPGConfig, vault);
        if (keyFile.exists() && keyFile.isFile()) {
            keyFile.delete();
        }
    }

    public static String loadPassword(GnuPGConfig gnuPGConfig, String vault) throws KeychainAccessException {
        final File keyFile = getKeyFile(gnuPGConfig, vault);
        if (!keyFile.isFile()) {
            throw new KeychainAccessException("Password key file: " + keyFile + " not found");
        }
        final String encryptedKey = readFile(keyFile);
        final byte[] password = decrypt(gnuPGConfig, encryptedKey);
        return new String(password, StandardCharsets.UTF_8);
    }

    public static void storePassword(GnuPGConfig gnuPGConfig, String vault, String name, CharSequence password) throws KeychainAccessException {
        final String encryptedPassword = encrypt(gnuPGConfig, password.toString().getBytes(StandardCharsets.UTF_8), name);
        final File keyFile = getKeyFile(gnuPGConfig, vault);
        writeFile(keyFile, encryptedPassword);
    }

    private static File getKeyFile(GnuPGConfig gnuPGConfig, String vault) {
        final StringBuilder sb = new StringBuilder(vault.length());
        for (char c : vault.toCharArray()) {
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || (c == '-' || c == '.')) {
                sb.append(c);
            } else if (c == '_') {
                sb.append("__");
            } else {
                sb.append('_');
                final String hex = Integer.toHexString(c);
                if (hex.length() % 2 != 0) {
                    sb.append('0');
                }
                sb.append(hex);
            }
        }
        return new File(getEncryptKeyBasePath(gnuPGConfig), sb.toString());
    }

    private static String readFile(File file) throws KeychainAccessException {
        final StringBuilder sb = new StringBuilder((int) file.length());
        try (final BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            for (int b; ((b = reader.read()) != -1); ) {
                sb.append((char) b);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new KeychainAccessException("Read file: " + file + " failed", e);
        }
    }

    private static void writeFile(File file, String content) throws KeychainAccessException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new KeychainAccessException("Write file: " + file + " failed", e);
        }
    }

    private static byte[] decrypt(GnuPGConfig gnuPGConfig, String input) throws KeychainAccessException {
        final UtilsCommandResult decryptResult = runGpgPG(
                gnuPGConfig,
                input.getBytes(StandardCharsets.UTF_8),
                "-d"
        );
        if (decryptResult.getExitValue() != 0) {
            throw new KeychainAccessException("GnuPG decrypt failed: " + decryptResult);
        }
        return decryptResult.getStdout();
    }

    private static String encrypt(GnuPGConfig gnuPGConfig, byte[] input, String name) throws KeychainAccessException {
        final UtilsCommandResult encryptResult = runGpgPG(
                gnuPGConfig,
                input,
                "-r", gnuPGConfig.getKeyId(), "-e", "-a", "--no-comment", "--comment", "Cryptomator: " + name
        );
        if (encryptResult.getExitValue() != 0) {
            throw new KeychainAccessException("GnuPG encrypt failed: " + encryptResult);
        }
        return new String(encryptResult.getStdout(), StandardCharsets.UTF_8);
    }

    private static UtilsCommandResult runGpgPG(GnuPGConfig gnuPGConfig, byte[] input, String... arguments) throws KeychainAccessException {
        final String gpgCmd = getGnuPGCommand(gnuPGConfig);
        final List<String> commands = new ArrayList<>();
        commands.add(gpgCmd);
        if ((arguments == null) || (arguments.length == 0)) {
            throw new KeychainAccessException("GnuPG not arguments");
        }
        commands.addAll(Arrays.asList(arguments));
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder(commands);
            final Process process = processBuilder.start();

            // ----- STD IN -----
            final AtomicReference<IOException> inThreadException = new AtomicReference<>();
            final Thread inThread = new Thread(() -> {
                if ((input != null) && (input.length > 0)) {
                    try (OutputStream processIn = process.getOutputStream()) {
                        processIn.write(input);
                    } catch (IOException e) {
                        inThreadException.set(e);
                    }
                }
            });
            inThread.setDaemon(true);
            inThread.setName("gpg-stdin");

            // ----- STD OUT -----
            final AtomicReference<IOException> outThreadException = new AtomicReference<>();
            final ByteArrayOutputStream outBaos = new ByteArrayOutputStream();
            final Thread outThread = getThread(process.getInputStream(), outBaos, outThreadException, "gpg-stdout");
            // ----- STD ERR -----
            final AtomicReference<IOException> errThreadException = new AtomicReference<>();
            final ByteArrayOutputStream errBaos = new ByteArrayOutputStream();
            final Thread errThread = getThread(process.getErrorStream(), errBaos, errThreadException, "gpg-stderr");

            inThread.start();
            outThread.start();
            errThread.start();

            inThread.join();
            if (inThreadException.get() != null) {
                throw inThreadException.get();
            }
            outThread.join();
            if (outThreadException.get() != null) {
                throw outThreadException.get();
            }
            errThread.join();
            if (errThreadException.get() != null) {
                throw errThreadException.get();
            }
            final int exitValue = process.waitFor();

            return new UtilsCommandResult(exitValue, outBaos.toByteArray(), errBaos.toByteArray());
        } catch (Exception e) {
            throw new KeychainAccessException("Run GnuPG command failed: " + commands, e);
        }
    }

    private static Thread getThread(InputStream is, ByteArrayOutputStream outBaos, AtomicReference<IOException> outThreadException, String name) {
        final Thread outThread = new Thread(() -> {
            int b;
            try {
                while ((b = is.read()) != -1) {
                    outBaos.write(b);
                }
            } catch (IOException e) {
                outThreadException.set(e);
            }
        });
        outThread.setDaemon(true);
        outThread.setName(name);
        return outThread;
    }

    private static String getGnuPGCommand(GnuPGConfig gnuPGConfig) {
        if ((gnuPGConfig != null) && StringUtils.isNoneEmpty(gnuPGConfig.getGnuPgCommand())) {
            return gnuPGConfig.getGnuPgCommand();
        }
        return DEFAULT_GPG_COMMAND;
    }

    private static File getEncryptKeyBasePath(GnuPGConfig gnuPGConfig) {
        final File encryptKeyBase;
        if ((gnuPGConfig != null) && StringUtils.isNoneEmpty(gnuPGConfig.getEncryptKeyBasePath())) {
            encryptKeyBase = new File(gnuPGConfig.getEncryptKeyBasePath());
        } else {
            encryptKeyBase = DEFAULT_ENCRYPTION_KEY_BASE_PATH;
        }
        if (encryptKeyBase.isDirectory()) {
            return encryptKeyBase;
        }
        LOG.info("Make dirs: " + encryptKeyBase);
        encryptKeyBase.mkdirs();
        return encryptKeyBase;
    }

    private static File getGnuPGConfigFile() throws KeychainAccessException {
        for (File configFile : Arrays.asList(GNUPG_CONFIG_FILE1, GNUPG_CONFIG_FILE2)) {
            LOG.info("Check config file: " + configFile + ": " + Arrays.asList(configFile.exists(), configFile.isFile()));
            if (configFile.exists() && configFile.isFile()) {
                return configFile;
            }
        }
        throw new KeychainAccessException("GnuPG config file not found.");
    }
}
