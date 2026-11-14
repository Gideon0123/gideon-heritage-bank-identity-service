package com.gideonheritagebank.IDENTITY_SERVICE.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {

    private Long userId;
    private String email;
    private String role;

}