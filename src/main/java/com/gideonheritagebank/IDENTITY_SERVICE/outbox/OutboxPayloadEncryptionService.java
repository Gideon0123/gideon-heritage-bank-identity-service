package com.gideonheritagebank.IDENTITY_SERVICE.outbox;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class OutboxPayloadEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BYTES = 32;
    private final String encodedKey;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey secretKey;

    public OutboxPayloadEncryptionService(
            @Value("${identity.outbox.encryption-key}")
            String encodedKey
    ) {
        this.encodedKey = encodedKey;
    }

    @PostConstruct
    void initialize() {

        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);

            if (keyBytes.length != KEY_LENGTH_BYTES) {
                throw new IllegalStateException(
                        "identity.outbox.encryption-key must decode to exactly 32 bytes"
                );
            }

            this.secretKey = new SecretKeySpec(keyBytes, "AES");

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "identity.outbox.encryption-key must be a valid Base64-encoded AES-256 key",
                    exception
            );
        }
    }

    public String encrypt(byte[] plaintext) {

        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];

            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce)
            );

            byte[] ciphertext = cipher.doFinal(plaintext);

            ByteBuffer combined = ByteBuffer.allocate(
                    nonce.length + ciphertext.length
            );

            combined.put(nonce);
            combined.put(ciphertext);

            return Base64.getEncoder().encodeToString(combined.array());

        } catch (GeneralSecurityException exception) {

            throw new IllegalStateException(
                    "Could not encrypt outbox payload",
                    exception
            );
        }
    }

    public byte[] decrypt(String encryptedPayload) {

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedPayload);

            if (combined.length <= NONCE_LENGTH_BYTES) {
                throw new IllegalStateException("Encrypted outbox payload is malformed");
            }

            ByteBuffer buffer = ByteBuffer.wrap(combined);

            byte[] nonce = new byte[NONCE_LENGTH_BYTES];

            buffer.get(nonce);

            byte[] ciphertext = new byte[buffer.remaining()];

            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, nonce)
            );

            return cipher.doFinal(ciphertext);

        } catch (GeneralSecurityException | IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "Could not decrypt outbox payload",
                    exception
            );
        }
    }
}