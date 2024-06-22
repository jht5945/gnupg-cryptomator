package me.hatter.integrations.keychain;

import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hatterjiang
 */
public class GnuPGAccessProvider implements KeychainAccessProvider {

    private static final Logger LOG = LoggerFactory.getLogger(GnuPGAccessProvider.class);

    private GnuPGConfig gnuPGConfig;

    public GnuPGAccessProvider() {
        try {
            gnuPGConfig = Utils.loadGnuPGConfig();
            if (!Utils.checkGnuPGReady(gnuPGConfig)) {
                LOG.error("Check GnuPG command failed");
                gnuPGConfig = null;
            }
        } catch (KeychainAccessException e) {
            gnuPGConfig = null;
            LOG.error("Load GnuPG config failed", e);
        }
    }

    @Override
    public String displayName() {
        return "GnuPG";
    }

    @Override
    public boolean isSupported() {
        return gnuPGConfig != null;
    }

    @Override
    public boolean isLocked() {
        // No lock status
        return false;
    }

    @Override
    public void storePassphrase(String vault, CharSequence password) throws KeychainAccessException {
        storePassphrase(vault, "Vault", password);
    }

    @Override
    public void storePassphrase(String vault, String name, CharSequence password) throws KeychainAccessException {
        Utils.storePassword(gnuPGConfig, vault, name, password);
    }

    @Override
    public char[] loadPassphrase(String vault) throws KeychainAccessException {
        final String password = Utils.loadPassword(gnuPGConfig, vault);
        return password.toCharArray();
    }

    @Override
    public void deletePassphrase(String vault) throws KeychainAccessException {
        if (isLocked()) {
            LOG.info("Failed to delete password. KeePassXC database is locked. Needs to be unlocked first.");
            return;
        }
        Utils.deletePassword(gnuPGConfig, vault);
    }

    @Override
    public void changePassphrase(String vault, CharSequence password) throws KeychainAccessException {
        changePassphrase(vault, "Vault", password);
    }

    @Override
    public void changePassphrase(String vault, String name, CharSequence password) throws KeychainAccessException {
        LOG.info("Change password for: " + vault);
        Utils.storePassword(gnuPGConfig, vault, name, password);
    }
}
