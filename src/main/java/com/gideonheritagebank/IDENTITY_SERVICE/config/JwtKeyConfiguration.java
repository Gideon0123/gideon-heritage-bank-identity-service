package com.gideonheritagebank.IDENTITY_SERVICE.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtKeyConfiguration {

    @Value("${identity.jwt.keystore-path}")
    private String keystorePath;

    @Value("${identity.jwt.keystore-password}")
    private String keystorePassword;

    @Value("${identity.jwt.keystore-alias:identity-jwt}")
    private String keystoreAlias;

    @Value("${identity.jwt.issuer}")
    private String issuer;

    @Value("${identity.jwt.audience}")
    private String audience;

    @Bean
    public RSAKey rsaSigningKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");

            try (InputStream inputStream =
                         Files.newInputStream(Path.of(keystorePath))) {

                keyStore.load(
                        inputStream,
                        keystorePassword.toCharArray()
                );
            }

            Key key = keyStore.getKey(
                    keystoreAlias,
                    keystorePassword.toCharArray()
            );

            Certificate certificate =
                    keyStore.getCertificate(keystoreAlias);

            if (!(key instanceof RSAPrivateKey privateKey)) {
                throw new IllegalStateException(
                        "The configured keystore entry is not an RSA private key"
                );
            }

            if (certificate == null
                    || !(certificate.getPublicKey() instanceof RSAPublicKey publicKey)) {
                throw new IllegalStateException(
                        "The configured keystore entry has no RSA public key"
                );
            }

            /*
             * Derive a stable key ID from the public key.
             * The same key retains the same ID across application restarts.
             */
            RSAKey preliminaryKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(KeyUse.SIGNATURE)
                    .build();

            String keyId = preliminaryKey.computeThumbprint().toString();

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyId)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(KeyUse.SIGNATURE)
                    .build();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to load the JWT signing key. "
                            + "Check the keystore path, alias, and password.",
                    exception
            );
        }
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaSigningKey) {
        return new ImmutableJWKSet<>(
                new JWKSet(rsaSigningKey)
        );
    }

    @Bean
    public JwtEncoder jwtEncoder(
            JWKSource<SecurityContext> jwkSource
    ) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaSigningKey) throws JOSEException {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(rsaSigningKey.toRSAPublicKey()).build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            if (jwt.getAudience() != null
                    && jwt.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            "invalid_token",
                            "Required token audience is missing",
                            null
                    )
            );
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator
                )
        );

        return decoder;
    }
}