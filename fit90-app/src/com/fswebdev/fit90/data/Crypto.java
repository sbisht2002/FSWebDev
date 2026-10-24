package com.fswebdev.fit90.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM encryption for everything in the vault. The key is generated inside the
 * Android Keystore (hardware-backed on most phones) and never leaves it. If the
 * Keystore is unavailable (some emulators, unit tests) a random key kept in private
 * app storage is used instead.
 */
public final class Crypto {

    private static final String ALIAS = "fit90_vault_v1";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;

    public Crypto(Context context) {
        SecretKey k = null;
        try {
            k = keystoreKey();
        } catch (Exception e) {
            k = null;
        }
        key = k != null ? k : fallbackKey(context);
    }

    private static SecretKey keystoreKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(ALIAS)) {
            return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
        }
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return gen.generateKey();
    }

    private static SecretKey fallbackKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("vault_key", Context.MODE_PRIVATE);
        String stored = prefs.getString("k", null);
        byte[] raw;
        if (stored == null) {
            raw = new byte[32];
            new SecureRandom().nextBytes(raw);
            prefs.edit().putString("k", Base64.encodeToString(raw, Base64.NO_WRAP)).commit();
        } else {
            raw = Base64.decode(stored, Base64.NO_WRAP);
        }
        return new SecretKeySpec(raw, "AES");
    }

    /** Deletes the key so nothing encrypted with it can be read again. */
    public static void destroyKey(Context context) {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            ks.deleteEntry(ALIAS);
        } catch (Exception ignored) {
            // No keystore: nothing to delete there.
        }
        context.getSharedPreferences("vault_key", Context.MODE_PRIVATE).edit().clear().commit();
    }

    public byte[] encrypt(byte[] plain) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = c.getIV();
            byte[] ct = c.doFinal(plain);
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] data) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            return c.doFinal(data, IV_BYTES, data.length - IV_BYTES);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    // ---- PIN hashing (PBKDF2) ----

    public static String newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static String hashPin(String pin, String saltB64) {
        try {
            byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
            KeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 60000, 256);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("PIN hashing failed", e);
        }
    }

    /** Constant-time comparison. */
    public static boolean sameHash(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
