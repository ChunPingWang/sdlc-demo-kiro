# ArchUnit 規則範本

> 本檔提供對照 `.kiro/steering/java-coding-standards.md` 的 ArchUnit 規則範本。  
> 產生專案的 `ArchitectureTest.java` 時，依實際 Package Root 替換 `{基礎套件}`（如 `com.example.lifepremium`）。

---

## 依賴與版本

```xml
<!-- pom.xml：test scope -->
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

Gradle：

```groovy
testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
```

---

## 完整 ArchitectureTest 範本

放置於 `src/test/java/{基礎套件}/architecture/ArchitectureTest.java`。

```java
package {基礎套件}.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SliceRuleDefinition.slices;

/**
 * 架構規範確定性測試 — 對照 java-coding-standards.md
 * 這些結構性規則由 build 自動驗證，Code Review 時不需 LLM 讀取原始碼判斷。
 */
@AnalyzeClasses(
    packages = "{基礎套件}",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    // ══════════════════════════════════════════════════════
    // 1. 分層架構（對照 §4 分層職責與禁止事項）
    // ══════════════════════════════════════════════════════

    @ArchTest
    static final ArchRule 分層架構依賴方向 = layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Repository").definedBy("..repository..")
        .layer("Domain").definedBy("..domain..")
        .layer("Dto").definedBy("..dto..")
        .layer("Exception").definedBy("..exception..")

        // Controller 只能被外部呼叫，不能被其他層依賴
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        // Service 只能被 Controller 存取
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
        // Repository 只能被 Service 存取
        .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule controller不得直接依賴repository =
        noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because("Controller 不得跳過 Service 直接存取 Repository（§4 Controller 層）");

    @ArchTest
    static final ArchRule domain不得依賴spring框架 =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.stereotype..")
            .because("Domain 層不得引用 Spring Bean（§4 Domain 層）");

    @ArchTest
    static final ArchRule dto不得依賴jpa =
        noClasses().that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
            .because("DTO 不得引用 JPA Entity 型態（§1.3 禁止事項）");

    @ArchTest
    static final ArchRule service不得依賴controller =
        noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("Service 不得反向依賴 Controller（§4 Service 層）");

    // ══════════════════════════════════════════════════════
    // 2. 類別命名慣例（對照 §2 類別命名規範）
    // ══════════════════════════════════════════════════════

    @ArchTest
    static final ArchRule controller命名 =
        classes().that().resideInAPackage("..controller..")
            .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().haveSimpleNameEndingWith("Controller")
            .because("Controller 類別須以 Controller 結尾（§2）");

    @ArchTest
    static final ArchRule repository命名 =
        classes().that().resideInAPackage("..repository..")
            .should().haveSimpleNameEndingWith("Repository")
            .because("Repository 類別須以 Repository 結尾（§2）");

    @ArchTest
    static final ArchRule serviceImpl命名 =
        classes().that().resideInAPackage("..service.impl..")
            .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
            .should().haveSimpleNameEndingWith("ServiceImpl")
            .because("Service 實作類別須以 ServiceImpl 結尾（§2）");

    @ArchTest
    static final ArchRule exception命名 =
        classes().that().resideInAPackage("..exception..")
            .and().areAssignableTo(RuntimeException.class)
            .should().haveSimpleNameEndingWith("Exception")
            .because("例外類別須以 Exception 結尾（§2）");

    @ArchTest
    static final ArchRule config命名 =
        classes().that().resideInAPackage("..config..")
            .and().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
            .should().haveSimpleNameEndingWith("Config")
            .because("設定類別須以 Config 結尾（§2）");

    // ══════════════════════════════════════════════════════
    // 3. 注解規範（對照 §5 注解使用規範）
    // ══════════════════════════════════════════════════════

    @ArchTest
    static final ArchRule controller須標注RestController =
        classes().that().resideInAPackage("..controller..")
            .and().haveSimpleNameEndingWith("Controller")
            .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .because("Controller 須標注 @RestController（§5）");

    @ArchTest
    static final ArchRule serviceImpl須標注Service =
        classes().that().resideInAPackage("..service.impl..")
            .and().haveSimpleNameEndingWith("ServiceImpl")
            .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
            .because("Service 實作須標注 @Service（§5）");

    @ArchTest
    static final ArchRule repository須為介面 =
        classes().that().resideInAPackage("..repository..")
            .and().haveSimpleNameEndingWith("Repository")
            .should().beInterfaces()
            .because("Repository 應為 Spring Data 介面（§4 Repository 層）");

    // ══════════════════════════════════════════════════════
    // 4. 循環依賴（對照 §4 分層職責）
    // ══════════════════════════════════════════════════════

    @ArchTest
    static final ArchRule 套件之間無循環依賴 =
        slices().matching("{基礎套件}.(*)..")
            .should().beFreeOfCycles()
            .because("模組之間不得有循環依賴");

    // ══════════════════════════════════════════════════════
    // 5. 一般禁止事項（對照 §1.3）
    // ══════════════════════════════════════════════════════

    @ArchTest
    static final ArchRule 不得使用field注入 =
        noClasses().should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
            .because("禁止 field injection，須用 constructor injection（@RequiredArgsConstructor）");

    @ArchTest
    static final ArchRule 不得直接使用javaUtilLogging =
        noClasses().should().dependOnClassesThat().resideInAPackage("java.util.logging..")
            .because("統一使用 SLF4J（@Slf4j），不得使用 java.util.logging");
}
```

---

## 規則對照表

| ArchUnit 規則 | 對照 java-coding-standards | 取代的 LLM 工作 |
|--------------|---------------------------|----------------|
| `分層架構依賴方向` | §4 分層職責 | 讀全部類別判斷層級依賴 |
| `controller不得直接依賴repository` | §1.3、§4 Controller | 逐一檢查 Controller import |
| `domain不得依賴spring框架` | §1.3、§4 Domain | 逐一檢查 domain import |
| `dto不得依賴jpa` | §1.3 | 逐一檢查 dto import |
| `*命名` 系列 | §2 類別命名規範 | 逐檔比對命名 |
| `*須標注*` 系列 | §5 注解規範 | 逐檔檢查注解 |
| `套件之間無循環依賴` | §4 | 人工難以偵測 |
| `不得使用field注入` | §5 建構子注入 | 逐檔檢查 @Autowired |

---

## 客製化指引

新增專案特定規則時，於 `ArchitectureTest` 追加 `@ArchTest static final ArchRule` 欄位。  
每條規則務必加上 `.because("...")` 說明並標注對照的規範章節，  
如此 ArchUnit 失敗訊息即可直接指出違反哪條規範，Code Review 時無需再查規範文件。
