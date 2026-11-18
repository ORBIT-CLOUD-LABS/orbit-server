import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

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
import java.util.List;
import java.util.Optional;

/**
 * docs/spring-java/architecture.md 의 의존 방향과 클래스 위치, 기능 간 순환을 검사한다.
 *
 * <p>설치한 뒤 아래 두 상수를 프로젝트의 패키지로 바꾼다. 바꾸지 않으면 규칙에 걸리는 클래스가 없어 테스트가 실패한다.
 * ArchUnit 기본값은 검사할 클래스가 하나도 없으면 실패한다. 이 실패를 상수를 채웠는지 알리는 신호로 쓰므로,
 * 규칙 1(domain 레이어)과 규칙 5(기능 slice)는 기본값을 그대로 둔다. 컨트롤러·global·external·엔티티·Repository 가
 * 없는 프로젝트도 있으므로, 나머지 규칙은 {@code allowEmptyShould(true)} 로 대상이 없어도 통과하게 둔다.
 *
 * <p>레이어는 {@code FEATURES_PACKAGE.<기능>.<레이어>[.하위...]} 에서 {@code <레이어>} 자리의 이름으로 판단한다.
 * {@code ..domain..} 같은 단순 패턴은 쓰지 않는다. 기능 패키지 자체가 {@code domain} 아래에 있는 프로젝트에서도
 * {@code domain.vehicle.domain} 과 {@code service.stream} 같은 하위 패키지를 맞게 구분하기 위해서다.
 *
 * <p>규칙 1~4 는 {@link FreezingArchRule} 로 감싸서 도입 시점의 기존 위반은 기준선(src/test/resources/archunit_store)에 두고
 * 새 위반만 실패시킨다. 기준선은 손으로 고치거나 지우지 않는다. 절차는 docs/java/lint-and-ci.md 의 "코드 규칙 테스트" 를 따른다.
 * 규칙 5(순환)는 기준선이 아니라 코드의 허용 목록을 쓴다.
 *
 * <p>다른 기능의 repository·엔티티를 쓰는지는 검사하지 않는다.
 */
@AnalyzeClasses(importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeArchives.class})
class LayerArchitectureTest {

    /** 기능 패키지들의 부모 패키지. 이 바로 아래 패키지 하나가 기능 하나다. */
    private static final String FEATURES_PACKAGE = "com.orbit.server.domain";

    /** 기능에 속하지 않는 기술 기반 패키지. FEATURES_PACKAGE 아래에 있어도 기능으로 보지 않는다. */
    private static final String GLOBAL_PACKAGE = "com.orbit.server.global";

    /** global/external 이 쓸 수 있는 기능의 레이어. 외부 연동 구현은 service 가 정한 포트와 그 타입을 쓴다. */
    private static final List<String> LAYERS_USABLE_BY_EXTERNAL = List.of("service", "domain", "exception");

    // 1. domain 은 다른 레이어를 모르고 Spring 에 의존하지 않는다. JPA 매핑 어노테이션(jakarta.persistence)은 Spring 이 아니므로 허용된다.
    @ArchTest
    static final ArchRule domain은_다른_레이어와_Spring에_의존하지_않는다 = FreezingArchRule.freeze(noClasses()
            .that(inLayer("domain"))
            .should()
            .dependOnClassesThat(
                    inLayer("controller", "service", "repository").or(resideInAPackage("org.springframework..")))
            .because("도메인 규칙을 컨테이너 없이 단위 테스트하고, 트랜잭션·외부 호출이 domain 으로 새지 않게 하기 위해서다"
                    + " → docs/spring-java/architecture.md"));

    // 2. controller 는 service 를 거친다. repository 를 직접 쓰지 않는다.
    @ArchTest
    static final ArchRule controller는_repository를_직접_쓰지_않는다 = FreezingArchRule.freeze(noClasses()
            .that(inLayer("controller"))
            .should()
            .dependOnClassesThat(inLayer("repository"))
            .because("controller → service → repository 방향을 지켜야 트랜잭션 경계가 service 에 모인다"
                    + " → docs/spring-java/architecture.md")
            .allowEmptyShould(true));

    // 3-1. global 은 기능에 의존하지 않는다. 기술 기반이 비즈니스 규칙을 알면 안 된다.
    @ArchTest
    static final ArchRule global은_기능_패키지에_의존하지_않는다 = FreezingArchRule.freeze(noClasses()
            .that(inGlobalOutsideExternal())
            .should()
            .dependOnClassesThat(featureClasses())
            .because("global 에는 기술 기반만 두므로 기능 → global 방향만 허용한다 → docs/spring-java/architecture.md")
            .allowEmptyShould(true));

    // 3-2. global/external 은 서비스가 정한 포트를 구현하므로 기능의 service·domain·exception 은 쓸 수 있다.
    // 프리셋 문서는 "포트와 포트가 쓰는 타입" 까지만 허용하지만, 이 규칙은 세 레이어 전체를 허용하는 것으로 근사한다.
    @ArchTest
    static final ArchRule external은_기능의_포트_레이어만_쓴다 = FreezingArchRule.freeze(noClasses()
            .that(inExternal())
            .should()
            .dependOnClassesThat(featureClasses().and(not(inLayer(LAYERS_USABLE_BY_EXTERNAL))))
            .because("외부 연동 구현은 service 의 포트를 구현할 뿐 controller·repository 를 거치지 않는다"
                    + " → docs/spring-java/architecture.md")
            .allowEmptyShould(true));

    // 4. 클래스 위치. 어노테이션은 타입 이름이 정확히 같을 때만 본다. @RestControllerAdvice 는 @RestController 가 아니다.
    // 컨트롤러·엔티티·Repository 가 아예 없는 프로젝트도 있으므로 대상이 없어도 실패하지 않게 둔다(allowEmptyShould).
    // 규칙 2·3 도 같은 이유로 allowEmptyShould 를 켠다. 상수를 채우지 않은 것은 규칙 1 과 5 만 대상이 없어 실패하는 것으로 알린다.
    @ArchTest
    static final ArchRule controller는_controller_레이어에_둔다 = FreezingArchRule.freeze(classes()
            .that(featureClasses())
            .and(annotatedWith("org.springframework.web.bind.annotation.RestController")
                    .or(annotatedWith("org.springframework.stereotype.Controller")))
            .should(beInLayer("controller"))
            .because("@RestController·@Controller 는 controller 레이어에 둔다 → docs/spring-java/architecture.md")
            .allowEmptyShould(true));

    @ArchTest
    static final ArchRule 엔티티는_domain_레이어에_둔다 = FreezingArchRule.freeze(classes()
            .that(featureClasses())
            .and(annotatedWith("jakarta.persistence.Entity").or(annotatedWith("jakarta.persistence.Embeddable")))
            .should(beInLayer("domain"))
            .because("@Entity·@Embeddable 은 domain 레이어에 둔다 → docs/spring-java/architecture.md")
            .allowEmptyShould(true));

    @ArchTest
    static final ArchRule Repository는_repository_레이어에_둔다 = FreezingArchRule.freeze(classes()
            .that(featureClasses())
            .and(INTERFACES)
            .and(assignableTo("org.springframework.data.repository.Repository"))
            .should(beInLayer("repository"))
            .because("Spring Data Repository 를 상속한 인터페이스는 repository 레이어에 둔다" + " → docs/spring-java/architecture.md")
            .allowEmptyShould(true));

    // 5. 기능끼리 순환 의존이 없다. 기능 하나가 slice 하나다.
    // 기준선에 두지 않는다. 순환 위반 문장에 경로상의 모든 의존이 들어가서, 순환에 걸친 클래스를 고칠 때마다
    // 같은 순환이 새 위반으로 잡히기 때문이다. 대신 알려진 역방향 의존만 클래스 단위로 허용한다.
    //
    // 허용 목록을 늘리지 않는다. 새 순환은 도메인 이벤트(ApplicationEventPublisher, @TransactionalEventListener)로 끊고,
    // 이벤트로 끊은 의존은 목록에서 지운다. 허용하는 이유를 한 줄씩 적는다. 예:
    //   // vehicle → update: 차량 등록이 초기 상태를 만들려고 VehicleStateService 를 부른다. 이벤트로 끊으면 이 줄을 지운다
    //   .ignoreDependency(
    //           "com.example.app.vehicle.service.VehicleRegistrationService",
    //           "com.example.app.update.service.VehicleStateService")
    @ArchTest
    static final ArchRule 기능끼리_순환_의존이_없다 = slices().matching(FEATURES_PACKAGE + ".(*)..")
            .should()
            .beFreeOfCycles()
            // global 로 들어가고 나가는 의존은 순환으로 보지 않는다. global 이 기능을 알면 규칙 3 이 잡는다.
            .ignoreDependency(resideInAPackage(GLOBAL_PACKAGE + ".."), DescribedPredicate.alwaysTrue())
            .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage(GLOBAL_PACKAGE + ".."))
            // vehicle → update: 차량 등록이 초기 상태를 만들려고 VehicleStateService 를 부른다. 이벤트로 끊으면 이 줄을 지운다
            .ignoreDependency(
                    "com.orbit.server.domain.vehicle.service.VehicleRegistrationService",
                    "com.orbit.server.domain.update.service.VehicleStateService")
            .because("기능끼리 서로를 알면 한쪽 변경이 다른 쪽으로 번진다. 역방향이 필요하면 도메인 이벤트로 끊는다" + " → docs/spring-java/architecture.md");

    /** 기능 패키지의 클래스. FEATURES_PACKAGE 바로 아래 패키지의 클래스는 포함하고, GLOBAL_PACKAGE 는 뺀다. */
    private static DescribedPredicate<JavaClass> featureClasses() {
        return DescribedPredicate.describe("기능 패키지의 클래스", javaClass -> isFeaturePackage(javaClass.getPackageName()));
    }

    /** 기능의 레이어(FEATURES_PACKAGE.<기능>.<레이어>) 가 주어진 이름 가운데 하나인 클래스. 하위 패키지도 그 레이어에 속한다. */
    private static DescribedPredicate<JavaClass> inLayer(String... layers) {
        return inLayer(List.of(layers));
    }

    // FreezingArchRule 은 규칙 설명을 기준선의 키로 쓴다. 설명이 실행마다 달라지면 매번 새 규칙으로 보고 위반을 조용히 기록하므로,
    // 순서가 없는 Set 이 아니라 List 로 받아 설명을 항상 같게 만든다.
    private static DescribedPredicate<JavaClass> inLayer(List<String> layers) {
        return DescribedPredicate.describe(
                "레이어 " + layers + " 의 클래스",
                javaClass -> layerOf(javaClass).filter(layers::contains).isPresent());
    }

    private static DescribedPredicate<JavaClass> inGlobalOutsideExternal() {
        return resideInAPackage(GLOBAL_PACKAGE + "..").and(not(inExternal()));
    }

    private static DescribedPredicate<JavaClass> inExternal() {
        return resideInAPackage(GLOBAL_PACKAGE + ".external..");
    }

    private static ArchCondition<JavaClass> beInLayer(String layer) {
        return new ArchCondition<>(layer + " 레이어에 있다") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String actual = layerOf(javaClass).orElse("(레이어 없음)");
                if (!actual.equals(layer)) {
                    events.add(SimpleConditionEvent.violated(
                            javaClass,
                            javaClass.getDescription() + " 이(가) " + actual + " 레이어에 있다. " + layer + " 레이어로 옮긴다"));
                }
            }
        };
    }

    private static Optional<String> layerOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!isFeaturePackage(packageName)) {
            return Optional.empty();
        }
        String[] segments = packageName.substring(FEATURES_PACKAGE.length() + 1).split("\\.");
        return segments.length > 1 ? Optional.of(segments[1]) : Optional.empty();
    }

    private static boolean isFeaturePackage(String packageName) {
        boolean underFeatures = packageName.startsWith(FEATURES_PACKAGE + ".");
        boolean inGlobal = packageName.equals(GLOBAL_PACKAGE) || packageName.startsWith(GLOBAL_PACKAGE + ".");
        return underFeatures && !inGlobal;
    }
}
