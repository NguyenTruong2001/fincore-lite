package com.example.fincorelite.shared.persistence.metadata;

import com.example.fincorelite.shared.error.BusinessException;
import com.example.fincorelite.shared.error.ErrorCode;

public final class SystemMetadataAlreadyExistsException extends BusinessException {
    public SystemMetadataAlreadyExistsException(String metadataKey, Throwable cause) {
        super(ErrorCode.SYSTEM_METADATA_ALREADY_EXISTS, "System metadata already exists: " + metadataKey, cause);
    }
}
