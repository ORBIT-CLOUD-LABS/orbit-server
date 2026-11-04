package com.orbit.server;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.orbit.server.domain.update.service.VehicleStateService;
import com.orbit.server.domain.vehicle.service.VehicleRegistrationService;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.util.Set;

/**
 * docs/architecture.md 의 의존 규칙을 검사한다. 문서로만 두면 리뷰에서 놓친 위반이 쌓이므로 빌드에서 막는다.
 *
 * <p>규칙을 바꾸려면 문서와 이 테스트를 함께 고친다.
 */
@AnalyzeClasses(packages = "com.orbit.server", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN_PACKAGE = "com.orbit.server.domain.";
    private static final Set<String> CONTEXT_INTERNAL_LAYERS = Set.of("entity", "repository");

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

    // 포트(service 의 interface)와 포트가 주고받는 타입(service 의 record, entity, exception)만 허용한다.
    // 금지 목록이 아니라 허용 목록으로 둬야 유스케이스 서비스나 도메인 서비스를 직접 부르는 경로까지 막힌다
    private static final DescribedPredicate<JavaClass> PORT_OR_PORT_TYPE = resideInAPackage("..domain..service..")
            .and(INTERFACES.or(assignableTo(Record.class)))
            .or(resideInAnyPackage("..domain..entity..", "..domain..exception.."))
            .as("ports or types used by ports");

    @ArchTest
    static final ArchRule externalDependsOnlyOnPorts = noClasses()
            .that()
            .resideInAPackage("..global.external..")
            .should()
            .dependOnClassesThat(resideInAPackage("..domain..").and(not(PORT_OR_PORT_TYPE)))
            .because("외부 연동 구현은 service 의 포트를 구현할 뿐 다른 계층이나 비즈니스 규칙을 거치지 않는다");

    // 기존 위반은 archunit_store 에 기록해 두고 새 위반만 막는다. 위반을 고치면 기록에서 자동으로 빠진다
    @ArchTest
    static final ArchRule contextsDoNotReferenceOtherContextsInternals = FreezingArchRule.freeze(classes()
            .that()
            .resideInAPackage(DOMAIN_PACKAGE + "*..")
            .should(notReferenceOtherContextsInternals())
            .because("다른 컨텍스트는 service 나 이벤트로만 접근해야 컨텍스트 안의 모델을 바꿔도 밖이 깨지지 않는다"));

    // 순환은 FreezingArchRule 로 기록하지 않는다. 순환 위반 문장에 경로상의 모든 의존(생성자 시그니처 등)이 들어가서,
    // 순환에 걸친 클래스를 고칠 때마다 같은 순환이 새 위반으로 잡히기 때문이다.
    // 대신 알려진 역방향 의존만 클래스 단위로 허용한다.
    // - vehicle → update: 차량 등록이 VehicleStateService 로 초기 상태를 만든다. 도메인 이벤트로 끊으면 이 허용을 지운다
    @ArchTest
    static final ArchRule contextsAreFreeOfCycles = slices().matching(DOMAIN_PACKAGE + "(*)..")
            .should()
            .beFreeOfCycles()
            .ignoreDependency(VehicleRegistrationService.class, VehicleStateService.class)
            .because("컨텍스트끼리 서로를 알면 한쪽 변경이 다른 쪽으로 번진다. 역방향이 필요하면 도메인 이벤트로 끊는다");

    private static ArchCondition<JavaClass> notReferenceOtherContextsInternals() {
        return new ArchCondition<>("not reference entity or repository of other contexts") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String context = contextOf(javaClass.getPackageName());
                javaClass.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> isOtherContextsInternal(context, dependency.getTargetClass()))
                        .forEach(dependency ->
                                events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription())));
            }
        };
    }

    private static boolean isOtherContextsInternal(String context, JavaClass target) {
        String packageName = target.getPackageName();
        if (!packageName.startsWith(DOMAIN_PACKAGE)) {
            return false;
        }
        String[] segments = packageName.substring(DOMAIN_PACKAGE.length()).split("\\.");
        return segments.length > 1 && !segments[0].equals(context) && CONTEXT_INTERNAL_LAYERS.contains(segments[1]);
    }

    private static String contextOf(String packageName) {
        return packageName.substring(DOMAIN_PACKAGE.length()).split("\\.")[0];
    }
}
