package com.gideonheritagebank.IDENTITY_SERVICE.entity;

import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "bank_users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_bank_users_email",
                        columnNames = "email"
                ),
                @UniqueConstraint(
                        name = "uq_bank_users_phone",
                        columnNames = "phone_number"
                )
        },
        indexes = {
                @Index(name = "idx_bank_users_last_name", columnList = "last_name"),
                @Index(name = "idx_bank_users_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankUser {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "profile_photo_url", length = 1000)
    private String profilePhotoUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<UserAddress> addresses = new ArrayList<>();

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private StaffProfile staffProfile;

    public BankUser(
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            String passwordHash,
            UserType userType
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.userType = userType;
    }

    public void addRole(Role role) {
        roles.add(role);
        role.getUsers().add(this);
    }

    public void removeRole(Role role) {
        roles.remove(role);
        role.getUsers().remove(this);
    }

    public void addAddress(UserAddress address) {
        addresses.add(address);
        address.setUser(this);
    }

    public void removeAddress(UserAddress address) {
        addresses.remove(address);
        address.setUser(null);
    }

    public void assignStaffProfile(StaffProfile staffProfile) {
        this.staffProfile = staffProfile;

        if (staffProfile != null && staffProfile.getUser() != this) {
            staffProfile.setUser(this);
        }
    }

    public static BankUser createCustomer(
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            String passwordHash
    ) {
        BankUser user = new BankUser();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(passwordHash);
        user.setUserType(UserType.CUSTOMER);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setAccountNonLocked(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        return user;
    }

    public static BankUser createStaff(
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            String passwordHash
    ) {
        BankUser user = new BankUser();

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPasswordHash(passwordHash);

        user.setUserType(UserType.STAFF);

        // Privileged identities must not be enabled automatically.
        user.setEnabled(false);
        user.setEmailVerified(false);
        user.setAccountNonLocked(true);

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        return user;
    }
}