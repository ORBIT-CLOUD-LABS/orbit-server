package com.orbit.server;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
 * 프리셋 템플릿 {@code LayerArchitectureTest} 에 없는 orbit 의 컨텍스트 경계 규칙을 검사한다.
 *
 * <p>레이어 의존 방향, 클래스 위치, 컨텍스트 간 순환은 {@code LayerArchitectureTest} 가 검사하므로 여기 두지 않는다. 두 테스트에 같은 규칙을 두면 같은 위반이 두 번
 * 보고된다. 근거 문서는 docs/architecture.md 의 컨텍스트 간 통신과 docs/spring-java/architecture.md 의 패키지 절이다.
 *
 * <p>규칙을 바꾸려면 문서와 이 테스트를 함께 고친다.
 */
@AnalyzeClasses(packages = "com.orbit.server", importOptions = ImportOption.DoNotIncludeTests.class)
class ContextBoundaryTest {

    private static final String DOMAIN_PACKAGE = "com.orbit.server.domain.";
    // 컨텍스트 바로 아래 domain 계층. "..domain..domain.." 처럼 쓰면 최상위 domain 과 겹쳐 뜻이 흐려지므로 위치를 고정한다
    private static final String CONTEXT_DOMAIN_LAYER = DOMAIN_PACKAGE + "*.domain..";
    private static final Set<String> CONTEXT_INTERNAL_LAYERS = Set.of("domain", "repository");

    // 포트(service 의 interface)와 포트가 주고받는 타입(service 의 record, domain, exception)만 허용한다.
    // 금지 목록이 아니라 허용 목록으로 둬야 유스케이스 서비스나 도메인 서비스를 직접 부르는 경로까지 막힌다.
    // LayerArchitectureTest 의 external 규칙은 service 레이어 전체를 허용하는 근사라 이 규칙을 따로 둔다
    private static final DescribedPredicate<JavaClass> PORT_OR_PORT_TYPE = resideInAPackage("..domain..service..")
            .and(INTERFACES.or(assignableTo(Record.class)))
            .or(resideInAnyPackage(CONTEXT_DOMAIN_LAYER, "..domain..exception.."))
            .as("ports or types used by ports");

    @ArchTest
    static final ArchRule externalDependsOnlyOnPorts = noClasses()
            .that()
            .resideInAPackage("..global.external..")
            .should()
            .dependOnClassesThat(resideInAPackage("..domain..").and(not(PORT_OR_PORT_TYPE)))
            .because("외부 연동 구현은 service 의 포트를 구현할 뿐 다른 계층이나 비즈니스 규칙을 거치지 않는다");

    // 기존 위반은 archunit_store 에 기록해 두고 새 위반만 막는다. 위반을 고치면 기록에서 자동으로 빠진다.
    // 규칙 설명(조건 이름 포함)이 기준선의 키라서 바꾸지 않는다. 바꾸면 새 규칙으로 보고 그 시점의 위반을 조용히 기록한다
    @ArchTest
    static final ArchRule contextsDoNotReferenceOtherContextsInternals = FreezingArchRule.freeze(classes()
            .that()
            .resideInAPackage(DOMAIN_PACKAGE + "*..")
            .should(notReferenceOtherContextsInternals())
            .because("다른 컨텍스트는 service 나 이벤트로만 접근해야 컨텍스트 안의 모델을 바꿔도 밖이 깨지지 않는다"));

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
