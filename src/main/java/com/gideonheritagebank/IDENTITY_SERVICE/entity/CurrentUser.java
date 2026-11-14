package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser {

    private UUID id;
    private String email;
    private String role;
}