# Spring Boot 程式碼範本

> 本文件提供各層程式碼的標準範本，供 `springboot-codegen` Skill 在 Phase 2 產出實作程式碼時參照。  
> 所有 `{PLACEHOLDER}` 須依實際專案替換。

---

## §1 BaseEntity（共用審計欄位）

```java
// src/main/java/com/{company}/{project}/domain/common/BaseEntity.java
package com.{company}.{project}.domain.common;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.UUID;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() { return deletedAt != null; }

    public void softDelete() { this.deletedAt = Instant.now(); }
}
```

---

## §2 Entity 範本

```java
// src/main/java/com/{company}/{project}/domain/{Entity}.java
package com.{company}.{project}.domain;

import com.{company}.{project}.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "{table_name}",
    indexes = {
        @Index(name = "idx_{table}_{field}", columnList = "{field}"),
        @Index(name = "uq_{table}_{field}", columnList = "{field}", unique = true)
    }
)
@SQLDelete(sql = "UPDATE {table_name} SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class {Entity} extends BaseEntity {

    @Column(name = "{field_1}", nullable = false, length = 255)
    private String {field1};

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private {Entity}Status status;

    // 多對一
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "{parent_id}", nullable = false)
    private {ParentEntity} {parent};

    // 一對多
    @OneToMany(mappedBy = "{entity}", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<{ChildEntity}> {children} = new ArrayList<>();

    // 靜態工廠方法
    public static {Entity} create(String {field1}) {
        {Entity} entity = new {Entity}();
        entity.{field1} = {field1};
        entity.status = {Entity}Status.ACTIVE;
        return entity;
    }

    public void update(String {field1}) {
        this.{field1} = {field1};
    }

    public enum {Entity}Status {
        ACTIVE, INACTIVE
    }
}
```

---

## §3 Repository 範本

```java
// src/main/java/com/{company}/{project}/repository/{Entity}Repository.java
package com.{company}.{project}.repository;

import com.{company}.{project}.domain.{Entity};
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface {Entity}Repository extends JpaRepository<{Entity}, UUID> {

    Optional<{Entity}> findBy{UniqueField}(String {uniqueField});
    boolean existsBy{UniqueField}(String {uniqueField});
    Page<{Entity}> findBy{Field}(String {field}, Pageable pageable);

    @Query("""
        SELECT e FROM {Entity} e
        WHERE (:keyword IS NULL
            OR LOWER(e.{searchField}) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND e.status = :status
        ORDER BY e.createdAt DESC
        """)
    Page<{Entity}> searchByKeywordAndStatus(
        @Param("keyword") String keyword,
        @Param("status") {Entity}.{Entity}Status status,
        Pageable pageable
    );

    // Fetch Join 解決 N+1
    @Query("""
        SELECT DISTINCT e FROM {Entity} e
        LEFT JOIN FETCH e.{children}
        WHERE e.id = :id
        """)
    Optional<{Entity}> findByIdWithChildren(@Param("id") UUID id);
}
```

---

## §4 DTO 範本

```java
// {Resource}CreateRequest.java — Java Record + Bean Validation
public record {Resource}CreateRequest(

    @NotBlank(message = "{validation.{field1}.required}")
    @Size(max = 255)
    String {field1},

    @NotNull
    @Positive
    Integer {field2},

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$",
             message = "{validation.email.invalid}")
    String email

) {}

// {Resource}UpdateRequest.java
public record {Resource}UpdateRequest(
    @NotBlank @Size(max = 255) String {field1},
    @NotNull @Positive Integer {field2}
) {}

// {Resource}Response.java
public record {Resource}Response(
    UUID id,
    String {field1},
    Integer {field2},
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
```

### 統一回應包裝

```java
// ApiResponse.java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String code, String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", null, data, Instant.now());
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now());
    }
}

// PageResponse.java
public record PageResponse<T>(
    List<T> content, int page, int size,
    long totalElements, int totalPages, boolean last
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
```

---

## §5 MapStruct Mapper 範本

```java
// src/main/java/com/{company}/{project}/mapper/{Resource}Mapper.java
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface {Resource}Mapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    {Entity} toEntity({Resource}CreateRequest request);

    {Resource}Response toResponse({Entity} entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest({Resource}UpdateRequest request,
                                 @MappingTarget {Entity} entity);
}
```

---

## §6 Service 範本

### 介面

```java
public interface {Business}Service {
    {Resource}Response create({Resource}CreateRequest request, UUID currentUserId);
    {Resource}Response findById(UUID id);
    PageResponse<{Resource}Response> findAll(String keyword, Pageable pageable);
    {Resource}Response update(UUID id, {Resource}UpdateRequest request, UUID currentUserId);
    void delete(UUID id, UUID currentUserId);
}
```

### 實作

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class {Business}ServiceImpl implements {Business}Service {

    private final {Entity}Repository {entity}Repository;
    private final {Resource}Mapper {resource}Mapper;

    @Override
    @Transactional
    public {Resource}Response create({Resource}CreateRequest request, UUID currentUserId) {
        log.info("Creating {entity}: userId={}", currentUserId);

        if ({entity}Repository.existsBy{UniqueField}(request.{uniqueField}())) {
            throw new {Resource}AlreadyExistsException(request.{uniqueField}());
        }

        {Entity} entity = {resource}Mapper.toEntity(request);
        {Entity} saved = {entity}Repository.save(entity);

        log.info("Created {entity}: id={}", saved.getId());
        return {resource}Mapper.toResponse(saved);
    }

    @Override
    @Cacheable(value = "{entity}s", key = "#id")
    public {Resource}Response findById(UUID id) {
        return {entity}Repository.findById(id)
            .map({resource}Mapper::toResponse)
            .orElseThrow(() -> new {Resource}NotFoundException(id));
    }

    @Override
    public PageResponse<{Resource}Response> findAll(String keyword, Pageable pageable) {
        return PageResponse.of(
            {entity}Repository
                .searchByKeywordAndStatus(keyword, {Entity}.{Entity}Status.ACTIVE, pageable)
                .map({resource}Mapper::toResponse)
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = "{entity}s", key = "#id")
    public {Resource}Response update(UUID id, {Resource}UpdateRequest request, UUID currentUserId) {
        {Entity} entity = {entity}Repository.findById(id)
            .orElseThrow(() -> new {Resource}NotFoundException(id));
        {resource}Mapper.updateEntityFromRequest(request, entity);
        log.info("Updated {entity}: id={}, userId={}", id, currentUserId);
        return {resource}Mapper.toResponse(entity);
    }

    @Override
    @Transactional
    @CacheEvict(value = "{entity}s", key = "#id")
    public void delete(UUID id, UUID currentUserId) {
        {Entity} entity = {entity}Repository.findById(id)
            .orElseThrow(() -> new {Resource}NotFoundException(id));
        entity.softDelete();
        log.info("Soft-deleted {entity}: id={}, userId={}", id, currentUserId);
    }
}
```

---

## §7 Controller 範本

```java
@RestController
@RequestMapping("/api/v1/{resources}")
@RequiredArgsConstructor
@Tag(name = "{Resource} API", description = "{功能說明}")
public class {Resource}Controller {

    private final {Business}Service {business}Service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ApiResponse<PageResponse<{Resource}Response>>> findAll(
        @RequestParam(required = false) String keyword,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(
            ApiResponse.success({business}Service.findAll(keyword, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<ApiResponse<{Resource}Response>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success({business}Service.findById(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<{Resource}Response>> create(
        @Valid @RequestBody {Resource}CreateRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success({business}Service.create(request, principal.getId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<{Resource}Response>> update(
        @PathVariable UUID id,
        @Valid @RequestBody {Resource}UpdateRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
            ApiResponse.success({business}Service.update(id, request, principal.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        {business}Service.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
```

---

## §8 Exception 範本

```java
// BusinessException.java — 基底
@Getter
public abstract class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

// {Resource}NotFoundException.java
public class {Resource}NotFoundException extends BusinessException {
    public {Resource}NotFoundException(UUID id) {
        super(ErrorCode.{RESOURCE}_NOT_FOUND,
              String.format("{Resource} not found: id=%s", id));
    }
}

// {Resource}AlreadyExistsException.java
public class {Resource}AlreadyExistsException extends BusinessException {
    public {Resource}AlreadyExistsException(String field) {
        super(ErrorCode.{RESOURCE}_ALREADY_EXISTS,
              String.format("{Resource} already exists: %s", field));
    }
}

// ErrorCode.java
@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED"),
    BUSINESS_VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_VALIDATION_FAILED"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN"),
    {RESOURCE}_NOT_FOUND(HttpStatus.NOT_FOUND, "{RESOURCE}_NOT_FOUND"),
    {RESOURCE}_ALREADY_EXISTS(HttpStatus.CONFLICT, "{RESOURCE}_ALREADY_EXISTS");

    private final HttpStatus httpStatus;
    private final String code;

    ErrorCode(HttpStatus httpStatus, String code) {
        this.httpStatus = httpStatus;
        this.code = code;
    }
}

// GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("Business exception: code={}, message={}",
            ex.getErrorCode().getCode(), ex.getMessage());
        return ResponseEntity
            .status(ex.getErrorCode().getHttpStatus())
            .body(ApiResponse.error(ex.getErrorCode().getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> handleValidation(
        MethodArgumentNotValidException ex
    ) {
        Map<String, List<String>> errors = ex.getBindingResult().getFieldErrors()
            .stream()
            .collect(Collectors.groupingBy(
                FieldError::getField,
                Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
            ));
        return ResponseEntity.badRequest()
            .body(new ApiResponse<>(ErrorCode.VALIDATION_FAILED.getCode(),
                "Validation failed", errors, Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                "An unexpected error occurred"));
    }
}
```
