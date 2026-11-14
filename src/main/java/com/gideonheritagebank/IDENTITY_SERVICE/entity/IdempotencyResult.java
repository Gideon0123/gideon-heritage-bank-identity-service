package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IdempotencyResult {

    private boolean completed;
    private boolean proceed;
    private IdempotencyRecord record;

}