package com.example.fincorelite.system.api;

import com.example.fincorelite.system.application.SystemMetadataApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Objects;

/**
 * Endpoint kỹ thuật cho bảng {@code system_metadata}.
 *
 * <p>Tồn tại chủ yếu để chứng minh toàn bộ chuỗi
 * validation → service → transaction → exception mapping chạy đúng qua HTTP
 * thật, thay vì chỉ được test bằng cách gọi thẳng vào
 * {@code GlobalExceptionHandler}.
 *
 * <p>Chưa có authorization. Sprint 1 phải gắn permission
 * {@code system-metadata:write} cho POST/PUT trước khi endpoint này ra khỏi
 * môi trường local.
 *
 * <p>Cố tình KHÔNG dùng {@code @Validated} ở class level. Từ Spring Framework
 * 6.1, constraint đặt trực tiếp trên {@code @PathVariable}/{@code @RequestParam}
 * đã được validate sẵn và ném {@code HandlerMethodValidationException}. Thêm
 * {@code @Validated} sẽ tạo CGLIB proxy cho controller và đổi exception thành
 * {@code ConstraintViolationException} — hai đường đi khác nhau cho cùng một
 * loại lỗi, không có lợi ích gì.
 */
@RestController
@RequestMapping("/api/v1/system-metadata")
public class SystemMetadataController {

    private final SystemMetadataApplicationService applicationService;

    public SystemMetadataController(
            SystemMetadataApplicationService applicationService
    ) {
        this.applicationService = Objects.requireNonNull(
                applicationService,
                "applicationService must not be null"
        );
    }

    @PostMapping
    public ResponseEntity<SystemMetadataResponse> create(
            @Valid @RequestBody CreateSystemMetadataRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        applicationService.create(
                request.metadataKey(),
                request.metadataValue()
        );

        URI location = uriBuilder
                .path("/api/v1/system-metadata/{metadataKey}")
                .buildAndExpand(request.metadataKey())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(new SystemMetadataResponse(
                        request.metadataKey(),
                        request.metadataValue()
                ));
    }

    @GetMapping("/{metadataKey}")
    public SystemMetadataResponse get(
            @PathVariable
            @NotBlank
            @Size(max = 100)
            String metadataKey
    ) {
        return new SystemMetadataResponse(
                metadataKey,
                applicationService.getValue(metadataKey)
        );
    }

    @PutMapping("/{metadataKey}")
    public ResponseEntity<Void> update(
            @PathVariable
            @NotBlank
            @Size(max = 100)
            String metadataKey,

            @Valid @RequestBody UpdateSystemMetadataRequest request
    ) {
        applicationService.changeValue(
                metadataKey,
                request.metadataValue()
        );

        return ResponseEntity.noContent().build();
    }
}
