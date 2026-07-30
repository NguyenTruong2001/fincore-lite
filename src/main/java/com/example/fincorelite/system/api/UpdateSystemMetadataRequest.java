package com.example.fincorelite.system.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSystemMetadataRequest(

        @NotBlank(
                message =
                        "metadataValue must not be blank"
        )
        @Size(
                max = 500,
                message =
                        "metadataValue must not exceed 500 characters"
        )
        String metadataValue
) {
}