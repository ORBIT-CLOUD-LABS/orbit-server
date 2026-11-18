import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import java.util.Optional;
import java.util.Set;

/**
 * 바이트코드로 판단할 수 있는 Java 코드 작성 규칙을 검사한다. 근거는 docs/java/ 의 각 문서다.
 *
 * <p>PackageSizeTest 와 같이 기본 패키지에 두고, 테스트 클래스와 jar·jrt 를 뺀 프로젝트의 main 클래스만 읽는다.
 *
 * <p>모든 규칙은 {@link FreezingArchRule} 로 감싸서, 도입 시점에 이미 있던 위반은 기준선(src/test/resources/archunit_store)에
 * 두고 새 위반만 실패시킨다. 기준선은 손으로 고치거나 지우지 않는다. 위반을 고치면 기준선에서 줄어든다.
 * 절차는 docs/java/lint-and-ci.md 의 "코드 규칙 테스트" 를 따른다.
 */
@AnalyzeClasses(importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeArchives.class})
class CodingRulesTest {

    /** 인자 없는 now() 가 시스템 시계를 직접 읽는 java.time 타입들. */
    private static final Set<String> TIME_TYPES_WITH_NOW = Set.of(
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.LocalTime",
            "java.time.Instant",
            "java.time.ZonedDateTime",
            "java.time.OffsetDateTime");

    @ArchTest
    static final ArchRule 표준_출력과_printStackTrace를_쓰지_않는다 =
            FreezingArchRule.freeze(GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.because(
                    "로그 레벨과 출력 위치를 설정으로 바꿀 수 없다 → docs/java/logging-and-security.md"));

    @ArchTest
    static final ArchRule java_util_logging을_쓰지_않는다 =
            FreezingArchRule.freeze(GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.because(
                    "로깅은 프로젝트의 로깅 라이브러리(SLF4J)로 통일한다 → docs/java/logging-and-security.md"));

    @ArchTest
    static final ArchRule 일반_예외를_그대로_던지지_않는다 =
            FreezingArchRule.freeze(GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.because(
                    "Exception·RuntimeException·Throwable 대신 의미 있는 커스텀 예외를 던진다 → docs/java/clean-code.md"));

    @ArchTest
    static final ArchRule 시계를_받지_않는_now를_부르지_않는다 = FreezingArchRule.freeze(noClasses()
            .should()
            .callMethodWhere(zeroArgumentNowOfJavaTime())
            .because("현재 시각은 Clock 을 주입받아 now(Clock) 으로 얻는다. 테스트에서 시각을 고정할 수 없다 → docs/java/testing.md"));

    @ArchTest
    static final ArchRule Optional을_필드와_파라미터에_쓰지_않는다 = FreezingArchRule.freeze(classes()
            .should(notUseOptionalAsFieldOrParameter())
            .because("Optional 은 반환 타입에만 쓴다 → docs/java/clean-code.md"));

    @ArchTest
    static final ArchRule Java_직렬화로_읽지_않는다 = FreezingArchRule.freeze(noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("java.io.ObjectInputStream")
            .because("신뢰할 수 없는 데이터를 Java 직렬화로 읽으면 원격 코드 실행으로 이어질 수 있다 → docs/java/logging-and-security.md"));

    /** java.time 타입의 인자 없는 now() 호출만 고른다. now(Clock)·now(ZoneId) 는 걸리지 않는다. */
    private static DescribedPredicate<JavaMethodCall> zeroArgumentNowOfJavaTime() {
        return new DescribedPredicate<>("java.time 의 인자 없는 now() 를 호출한다") {
            @Override
            public boolean test(JavaMethodCall call) {
                return call.getName().equals("now")
                        && call.getTarget().getRawParameterTypes().isEmpty()
                        && TIME_TYPES_WITH_NOW.contains(call.getTargetOwner().getName());
            }
        };
    }

    /** 필드 타입이나 메서드·생성자 파라미터 타입이 Optional 이면 위반이다. 컴파일러가 만든 합성 멤버(람다 등)는 뺀다. */
    private static ArchCondition<JavaClass> notUseOptionalAsFieldOrParameter() {
        return new ArchCondition<>("Optional 을 필드 타입이나 파라미터 타입으로 쓰지 않는다") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaField field : javaClass.getFields()) {
                    if (!field.getModifiers().contains(JavaModifier.SYNTHETIC)
                            && field.getRawType().isEquivalentTo(Optional.class)) {
                        events.add(SimpleConditionEvent.violated(field, field.getDescription() + " 의 타입이 Optional 이다"));
                    }
                }
                for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
                    if (codeUnit.getModifiers().contains(JavaModifier.SYNTHETIC)) {
                        continue;
                    }
                    for (JavaClass parameterType : codeUnit.getRawParameterTypes()) {
                        if (parameterType.isEquivalentTo(Optional.class)) {
                            events.add(SimpleConditionEvent.violated(
                                    codeUnit, codeUnit.getDescription() + " 의 파라미터 타입이 Optional 이다"));
                        }
                    }
                }
            }
        };
    }
}
