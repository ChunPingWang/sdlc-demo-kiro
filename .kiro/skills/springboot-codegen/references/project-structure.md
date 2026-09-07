# Spring Boot 標準專案目錄結構規範

> 本規範適用於以本 Skill 產生的所有 Java/Spring Boot 專案。  
> 基礎：Spring Boot 3.x、Java 17+、Maven 或 Gradle。

---

## 完整目錄結構

```
{project-root}/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/{company}/{project}/
│   │   │       ├── {ProjectName}Application.java        # Spring Boot 啟動入口
│   │   │       │
│   │   │       ├── config/                              # 設定類別
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── JpaConfig.java                   # @EnableJpaAuditing
│   │   │       │   ├── CacheConfig.java                 # Redis Cache
│   │   │       │   ├── OpenApiConfig.java               # Springdoc
│   │   │       │   └── AsyncConfig.java                 # @EnableAsync（選用）
│   │   │       │
│   │   │       ├── domain/                              # JPA Entity（業務領域物件）
│   │   │       │   ├── {EntityA}.java
│   │   │       │   ├── {EntityB}.java
│   │   │       │   └── common/
│   │   │       │       └── BaseEntity.java              # 共用審計欄位
│   │   │       │
│   │   │       ├── repository/                          # Spring Data JPA Repository
│   │   │       │   ├── {EntityA}Repository.java
│   │   │       │   └── {EntityB}Repository.java
│   │   │       │
│   │   │       ├── service/                             # 業務邏輯層
│   │   │       │   ├── {Business}Service.java           # 介面
│   │   │       │   └── impl/
│   │   │       │       └── {Business}ServiceImpl.java   # 實作
│   │   │       │
│   │   │       ├── controller/                          # REST Controller
│   │   │       │   └── {Resource}Controller.java
│   │   │       │
│   │   │       ├── dto/                                 # Data Transfer Objects
│   │   │       │   ├── request/
│   │   │       │   │   ├── {Resource}CreateRequest.java
│   │   │       │   │   └── {Resource}UpdateRequest.java
│   │   │       │   └── response/
│   │   │       │       ├── {Resource}Response.java
│   │   │       │       ├── PageResponse.java            # 通用分頁回應
│   │   │       │       └── ApiResponse.java             # 統一回應包裝
│   │   │       │
│   │   │       ├── exception/                           # 例外處理
│   │   │       │   ├── {Resource}NotFoundException.java
│   │   │       │   ├── {Resource}AlreadyExistsException.java
│   │   │       │   ├── BusinessValidationException.java
│   │   │       │   ├── ErrorCode.java                   # 錯誤碼枚舉
│   │   │       │   └── GlobalExceptionHandler.java      # @RestControllerAdvice
│   │   │       │
│   │   │       ├── mapper/                              # Entity ↔ DTO 轉換
│   │   │       │   └── {Resource}Mapper.java            # MapStruct 或手動
│   │   │       │
│   │   │       ├── security/                            # 安全相關（選用）
│   │   │       │   ├── JwtTokenProvider.java
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   └── UserPrincipal.java
│   │   │       │
│   │   │       └── event/                               # 領域事件（選用）
│   │   │           ├── {Domain}Event.java
│   │   │           └── {Domain}EventPublisher.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml                          # 主設定檔
│   │       ├── application-local.yml                   # 本機開發設定
│   │       ├── application-staging.yml                 # Staging 設定
│   │       ├── application-prod.yml                    # 正式環境設定
│   │       ├── db/migration/                            # Flyway 資料庫版本控制
│   │       │   ├── V1__create_initial_schema.sql
│   │       │   └── V2__{description}.sql
│   │       └── messages/
│   │           ├── messages.properties                  # 預設訊息（英文）
│   │           └── messages_zh_TW.properties            # 繁體中文訊息
│   │
│   └── test/
│       ├── java/
│       │   └── com/{company}/{project}/
│       │       ├── bdd/
│       │       │   ├── CucumberTestRunner.java          # Cucumber 執行入口
│       │       │   ├── CucumberSpringConfiguration.java # Spring 整合設定
│       │       │   └── steps/
│       │       │       └── {Feature}Steps.java          # Step Definitions
│       │       │
│       │       ├── controller/
│       │       │   └── {Resource}ControllerTest.java    # @SpringBootTest + MockMvc
│       │       │
│       │       ├── service/
│       │       │   └── {Business}ServiceTest.java       # JUnit5 + Mockito
│       │       │
│       │       ├── repository/
│       │       │   └── {Resource}RepositoryTest.java    # @DataJpaTest
│       │       │
│       │       └── fixture/
│       │           └── {Resource}Fixture.java           # 測試資料工廠
│       │
│       └── resources/
│           ├── application-test.yml                     # 測試環境設定（H2）
│           └── features/                                # Gherkin Feature 檔
│               └── {module}/
│                   └── {feature-name}.feature           # 從 sdlc/fsd/output/features/ 複製
│
├── pom.xml 或 build.gradle                              # 建置設定
├── .env.example                                         # 環境變數範本
├── docker-compose.yml                                   # 本機開發服務
├── Dockerfile                                           # 容器映像建置
├── REFACTOR-NOTES.md                                    # Phase 3 重構建議（由 Skill 產生）
└── README.md
```

---

## 核心依賴（pom.xml / build.gradle）

### Maven 必要依賴

```xml
<!-- Spring Boot Parent -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.x</version>
</parent>

<dependencies>
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Cache (Redis) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.6.x</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- OpenAPI (Springdoc) -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.x.x</version>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.x</version>
    </dependency>

    <!-- ── 測試依賴 ── -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
        <!-- 包含 JUnit5, Mockito, AssertJ, MockMvc -->
    </dependency>

    <!-- Cucumber -->
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-spring</artifactId>
        <version>7.x.x</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-junit-platform-engine</artifactId>
        <version>7.x.x</version>
        <scope>test</scope>
    </dependency>

    <!-- H2（測試用記憶體 DB）-->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers（整合測試真實 DB）-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## application.yml 基礎範本

```yaml
spring:
  application:
    name: {project-name}
  profiles:
    active: local

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/{db_name}}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate          # 正式環境使用 validate，由 Flyway 管理 schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 100

  flyway:
    enabled: true
    locations: classpath:db/migration

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

  cache:
    type: redis
    redis:
      time-to-live: 3600000       # 1 小時（ms）

server:
  port: 8080
  servlet:
    context-path: /

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

app:
  jwt:
    secret: ${JWT_SECRET:change-me-in-production}
    access-token-expiry: 900      # 15 分鐘（秒）
    refresh-token-expiry: 604800  # 7 天（秒）
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000}
```

---

## 分層職責摘要

| 層次 | 職責 | 禁止事項 |
|------|------|---------|
| Controller | HTTP 進出、`@Valid` 驗證、回應格式化 | 業務邏輯、直接存取 Repository |
| Service | 業務規則、交易管理、Entity ↔ DTO 轉換 | 直接回傳 Entity、HTTP 相關操作 |
| Repository | 資料存取、JPQL 查詢 | 業務邏輯、DTO 轉換 |
| Entity | 業務狀態封裝、JPA Mapping | 直接引用 DTO、Spring Bean |
| DTO | 資料傳輸結構、輸入驗證注解 | 業務邏輯、JPA 注解 |
| Exception | 例外定義與統一處理 | 業務邏輯 |
