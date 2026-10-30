package com.example.asm_final.backend;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Per-local-account remote sessions, encrypted with a non-exportable Android Keystore key. */
public final class BackendSession {
  private static final String ALIAS = "aimentor_backend_session_v1";
  private final Context context;
  private final int owner;
  private final String endpoint;
  private final String preferenceName;

  public BackendSession(Context context, int owner, String endpoint) {
    this(context, owner, endpoint, "backend_sessions");
  }

  public BackendSession(Context context, int owner, String endpoint, String preferenceName) {
    this.preferenceName = preferenceName;
    this.context = context.getApplicationContext();
    this.owner = owner;
    this.endpoint = endpoint;
  }

  private static synchronized SecretKey key() throws Exception {
    KeyStore store = KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    if (!store.containsAlias(ALIAS)) {
      KeyGenerator generator =
          KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
      generator.init(
          new KeyGenParameterSpec.Builder(
                  ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .build());
      generator.generateKey();
    }
    return (SecretKey) store.getKey(ALIAS, null);
  }

  public JSONObject load() throws Exception {
    String stored =
        context.getSharedPreferences(preferenceName, 0).getString("owner_" + owner, "");
    JSONObject decoded = decode(stored);
    if (decoded == null
        && !stored.isEmpty()
        && stored.equals(
            context.getSharedPreferences(preferenceName, 0).getString("owner_" + owner, "")))
      clear();
    return decoded;
  }

  public JSONObject decode(String stored) throws Exception {
    if (stored.isEmpty()) return null;
    try {
      JSONObject wrapped = new JSONObject(stored);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          key(),
          new GCMParameterSpec(128, Base64.decode(wrapped.getString("iv"), Base64.NO_WRAP)));
      cipher.updateAAD((owner + ":" + endpoint).getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return new JSONObject(
          new String(
              cipher.doFinal(Base64.decode(wrapped.getString("data"), Base64.NO_WRAP)),
              java.nio.charset.StandardCharsets.UTF_8));
    } catch (java.security.GeneralSecurityException
        | org.json.JSONException
        | IllegalArgumentException invalid) {
      return null;
    }
  }

  public void save(JSONObject session) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key());
    cipher.updateAAD((owner + ":" + endpoint).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    JSONObject wrapped =
        new JSONObject()
            .put("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
            .put(
                "data",
                Base64.encodeToString(
                    cipher.doFinal(
                        session.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    Base64.NO_WRAP));
    if (!context
        .getSharedPreferences(preferenceName, 0)
        .edit()
        .putString("owner_" + owner, wrapped.toString())
        .commit()) throw new java.io.IOException("SESSION_STORAGE_FAILED");
  }

  public void clear() {
    if (!context.getSharedPreferences(preferenceName, 0).edit().remove("owner_" + owner).commit())
      throw new IllegalStateException("SESSION_STORAGE_FAILED");
  }

  public String take() {
    String stored =
        context.getSharedPreferences(preferenceName, 0).getString("owner_" + owner, "");
    clear();
    return stored;
  }
}
