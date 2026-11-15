package com.gideonheritagebank.IDENTITY_SERVICE.storage;

import com.gideonheritagebank.IDENTITY_SERVICE.cache.CacheNames;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.ProfileImageResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.CurrentUser;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private static final Logger log = LoggerFactory.getLogger(ProfileImageService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private static final String OBJECT_KEY_PREFIX = "profile-images/";

    private final BankUserRepository bankUserRepository;
    private final ProfileImageStorage imageStorage;
    private final MinioStorageProperties storageProperties;
    private final CurrentUserService currentUserService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CUSTOMER_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_DIRECTORY, allEntries = true)
    })
    public ProfileImageResponse uploadMyProfileImage(MultipartFile file) {
        CurrentUser currentUser = currentUserService.getCurrentUser();

        BankUser user = bankUserRepository.findByIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The authenticated identity was not found."
                ));

        validateIdentityCanChangeImage(user);
        String extension = validateAndResolveExtension(file);

        String objectKey = OBJECT_KEY_PREFIX
                + user.getId()
                + "/"
                + UUID.randomUUID()
                + "."
                + extension;

        String previousObjectKey = user.getProfilePhotoUrl();

        imageStorage.upload(objectKey, file);

        user.setProfilePhotoUrl(objectKey);

        try {
            bankUserRepository.saveAndFlush(user);
        } catch (RuntimeException exception) {
            try {
                imageStorage.delete(objectKey);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
                log.error("Could not clean up uploaded profile image after database failure: {}", objectKey,
                        cleanupException);
            }
            throw exception;
        }

        registerStorageCleanup(objectKey, previousObjectKey);

        return toResponse(objectKey);
    }

    @Transactional(readOnly = true)
    public ProfileImageResponse getMyProfileImage() {
        CurrentUser currentUser = currentUserService.getCurrentUser();

        BankUser user = bankUserRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The authenticated identity was not found."
                ));

        String objectKey = user.getProfilePhotoUrl();

        if (!isManagedObjectKey(objectKey)) {
            throw new ResourceNotFoundException(
                    "No profile image has been uploaded for this identity."
            );
        }

        return toResponse(objectKey);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CUSTOMER_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_PROFILE, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.STAFF_DIRECTORY, allEntries = true)
    })
    public void deleteMyProfileImage() {
        CurrentUser currentUser = currentUserService.getCurrentUser();

        BankUser user = bankUserRepository.findByIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "The authenticated identity was not found."
                ));

        validateIdentityCanChangeImage(user);

        String previousObjectKey = user.getProfilePhotoUrl();
        if (previousObjectKey == null || previousObjectKey.isBlank()) {
            return;
        }

        user.setProfilePhotoUrl(null);
        bankUserRepository.saveAndFlush(user);

        registerStorageCleanup(null, previousObjectKey);
    }

    private void validateIdentityCanChangeImage(BankUser user) {
        if (user.getUserType() != UserType.CUSTOMER
                && user.getUserType() != UserType.STAFF) {
            throw new BadRequest("This identity cannot have a profile image.");
        }

        if (user.isIdentityClosed()) {
            throw new BadRequest("A closed identity cannot change its profile image.");
        }

        if (user.isSuspended()) {
            throw new BadRequest("A suspended identity cannot change its profile image.");
        }
    }

    private String validateAndResolveExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequest("A profile image file is required.");
        }

        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new BadRequest("The profile image exceeds the configured file-size limit.");
        }

        String contentType = file.getContentType();
        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequest("Only JPEG, PNG, and WebP profile images are allowed.");
        }

        try {
            byte[] bytes = file.getBytes();
            if (!matchesImageSignature(bytes, contentType.toLowerCase(Locale.ROOT))) {
                throw new BadRequest("The uploaded file content does not match its image type.");
            }
        } catch (IOException exception) {
            throw new BadRequest("The uploaded profile image could not be read.");
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new BadRequest("Unsupported profile image type.");
        };
    }

    private boolean matchesImageSignature(byte[] bytes, String contentType) {
        if (bytes == null) {
            return false;
        }

        return switch (contentType) {
            case "image/jpeg" ->
                    bytes.length >= 3
                            && (bytes[0] & 0xFF) == 0xFF
                            && (bytes[1] & 0xFF) == 0xD8
                            && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" ->
                    bytes.length >= 8
                            && (bytes[0] & 0xFF) == 0x89
                            && bytes[1] == 0x50
                            && bytes[2] == 0x4E
                            && bytes[3] == 0x47
                            && bytes[4] == 0x0D
                            && bytes[5] == 0x0A
                            && bytes[6] == 0x1A
                            && bytes[7] == 0x0A;
            case "image/webp" ->
                    bytes.length >= 12
                            && bytes[0] == 'R'
                            && bytes[1] == 'I'
                            && bytes[2] == 'F'
                            && bytes[3] == 'F'
                            && bytes[8] == 'W'
                            && bytes[9] == 'E'
                            && bytes[10] == 'B'
                            && bytes[11] == 'P';
            default -> false;
        };
    }

    private ProfileImageResponse toResponse(String objectKey) {
        Instant expiresAt = Instant.now().plusSeconds(
                storageProperties.getPresignedUrlExpirySeconds()
        );

        return new ProfileImageResponse(
                objectKey,
                imageStorage.createPresignedReadUrl(objectKey),
                expiresAt
        );
    }

    private boolean isManagedObjectKey(String objectKey) {
        return objectKey != null
                && objectKey.startsWith(OBJECT_KEY_PREFIX);
    }

    private void registerStorageCleanup(
            String newlyUploadedObjectKey,
            String previousObjectKey
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (isManagedObjectKey(previousObjectKey)) {
                imageStorage.delete(previousObjectKey);
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (isManagedObjectKey(previousObjectKey)
                                && !previousObjectKey.equals(newlyUploadedObjectKey)) {
                            try {
                                imageStorage.delete(previousObjectKey);
                            } catch (RuntimeException exception) {
                                // Database commit has already succeeded. A cleanup
                                // failure must not make the API report a false rollback.
                                log.error("Could not delete replaced profile image: {}",
                                        previousObjectKey, exception);
                            }
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED
                                && isManagedObjectKey(newlyUploadedObjectKey)) {
                            try {
                                imageStorage.delete(newlyUploadedObjectKey);
                            } catch (RuntimeException exception) {
                                log.error("Could not clean up profile image after transaction rollback: {}",
                                        newlyUploadedObjectKey, exception);
                            }
                        }
                    }
                }
        );
    }
}
