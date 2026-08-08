package com.example.fincorelite.system.domain;

import com.example.fincorelite.shared.error.BusinessException;
import com.example.fincorelite.shared.error.ErrorCode;

public final class SystemMetadataNotFoundException extends BusinessException {

    public SystemMetadataNotFoundException(String metadataKey) {
        super(
                ErrorCode.SYSTEM_METADATA_NOT_FOUND,
                "System metadata was not found: " + metadataKey
        );
    }
}
