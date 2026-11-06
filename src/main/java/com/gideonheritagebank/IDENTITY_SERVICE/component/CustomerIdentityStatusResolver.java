package com.gideonheritagebank.IDENTITY_SERVICE.component;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerIdentityStatus;
import org.springframework.stereotype.Component;

@Component
public class CustomerIdentityStatusResolver {

    public CustomerIdentityStatus resolve(BankUser user) {

        if (user.isIdentityClosed()) {
            return CustomerIdentityStatus.CLOSED;
        }

        if (user.isSuspended()) {
            return CustomerIdentityStatus.SUSPENDED;
        }

        if (!user.isAccountNonLocked()) {
            return CustomerIdentityStatus.LOCKED;
        }

        if (!user.isEnabled()) {
            return CustomerIdentityStatus.DISABLED;
        }

        if (!user.isEmailVerified()) {
            return CustomerIdentityStatus.PENDING_VERIFICATION;
        }

        return CustomerIdentityStatus.ACTIVE;
    }
}