package com.orbit.server;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * docs/architecture.md 의 의존 규칙을 검사한다. 문서로만 두면 리뷰에서 놓친 위반이 쌓이므로 빌드에서 막는다.
 *
 * <p>규칙을 바꾸려면 문서와 이 테스트를 함께 고친다.
 */
@AnalyzeClasses(packages = "com.orbit.server", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule entityDoesNotDependOnSpring = noClasses()
            .that()
            .resideInAPackage("..domain..entity..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .because("도메인 규칙을 컨테이너 없이 단위 테스트하고, 트랜잭션·외부 호출이 entity 로 새지 않게 하기 위해서다");

    @ArchTest
    static final ArchRule globalDoesNotDependOnDomain = noClasses()
            .that()
            .resideInAPackage("..global..")
            .and()
            .resideOutsideOfPackage("..global.external..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..domain..")
            .because("global 에는 기술 기반만 두고 비즈니스 규칙을 두지 않으므로 domain → global 방향만 허용한다");

    // global/external 은 service 의 포트를 구현하므로 포트와 포트가 주고받는 entity·exception 까지만 안다
    @ArchTest
    static final ArchRule externalDependsOnlyOnPorts = noClasses()
            .that()
            .resideInAPackage("..global.external..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..domain..controller..", "..domain..dto..", "..domain..repository..")
            .because("외부 연동 구현은 service 의 포트를 구현할 뿐 다른 계층을 거치지 않는다");
}
