package me.hatter.integrations.keychain;

/**
 * GnuPG config
 *
 * @author hatterjiang
 */
public class GnuPGConfig {
    /**
     * REQUIRED, GnuPG key ID
     */
    private String keyId;
    /**
     * OPTIONAL, GnuPG command path, default "gpg"
     */
    private String gnuPgCommand;
    /**
     * OPTIONAL, Encrypt key base path, default "~/.config/cryptomator/keys/"
     */
    private String encryptKeyBasePath;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getGnuPgCommand() {
        return gnuPgCommand;
    }

    public void setGnuPgCommand(String gnuPgCommand) {
        this.gnuPgCommand = gnuPgCommand;
    }

    public String getEncryptKeyBasePath() {
        return encryptKeyBasePath;
    }

    public void setEncryptKeyBasePath(String encryptKeyBasePath) {
        this.encryptKeyBasePath = encryptKeyBasePath;
    }
}
