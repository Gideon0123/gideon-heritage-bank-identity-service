package com.gideonheritagebank.IDENTITY_SERVICE.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class JwkController {

    private final RSAKey rsaSigningKey;

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> getJwks() {

        JWKSet publicJwkSet = new JWKSet(
                rsaSigningKey.toPublicJWK()
        );

        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl.maxAge(5, TimeUnit.MINUTES)
                                .cachePublic()
                )
                .body(publicJwkSet.toJSONObject());
    }
}
