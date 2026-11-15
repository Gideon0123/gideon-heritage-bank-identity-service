package com.gideonheritagebank.IDENTITY_SERVICE.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorage {

    void upload(String objectKey, MultipartFile file);

    String createPresignedReadUrl(String objectKey);

    void delete(String objectKey);
}
