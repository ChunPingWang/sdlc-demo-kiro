package com.example.lifepremium.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SliceRuleDefinition.slices;

/**
 * 架構規範確定性測試 — 對照 .kiro/steering/java-coding-standards.md
 *
 * 這些結構性規則由 build 自動驗證，Code Review 時 code-review skill
 * 不需 LLM 逐檔讀取原始碼判斷分層與命名，大幅降低 token 消耗。
 */
@AnalyzeClasses(
    packages = "com.example.lifepremium",
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
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
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
        slices().matching("com.example.lifepremium.(*)..")
            .should().beFreeOfCycles()
            .because("模組之間不得有循環依賴");

    // ══════════════════════════════════════════════════════
    // 5. 一般禁止事項（對照 §5 建構子注入）
    // ══════════════════════════════════════════════════════

    @ArchTest
    static final ArchRule 不得使用field注入 =
        noClasses().should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
            .because("禁止 field injection，須用 constructor injection（@RequiredArgsConstructor）");
}
