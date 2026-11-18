import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
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
import java.util.List;
import java.util.Optional;

/**
 * 바이트코드로 판단할 수 있는 Spring·JPA 코드 작성 규칙을 검사한다. 근거는 docs/spring-java/ 의 각 문서다.
 *
 * <p>PackageSizeTest 와 같이 기본 패키지에 두고, 테스트 클래스와 jar·jrt 를 뺀 프로젝트의 main 클래스만 읽는다.
 * 루트 패키지를 적을 필요가 없다.
 *
 * <p>모든 규칙은 {@link FreezingArchRule} 로 감싸서, 도입 시점에 이미 있던 위반은 기준선(src/test/resources/archunit_store)에
 * 두고 새 위반만 실패시킨다. 기준선은 손으로 고치거나 지우지 않는다. 절차는 docs/java/lint-and-ci.md 의 "코드 규칙 테스트" 를 따른다.
 *
 * <p>@Enumerated·@ManyToOne·@Entity 처럼 프로젝트에 해당 요소가 아예 없을 수 있는 규칙은 {@code allowEmptyShould(true)} 로 둔다.
 * ArchUnit 기본값은 검사할 클래스가 하나도 없으면 실패하는데, 요소가 없는 것은 위반이 아니기 때문이다.
 *
 * <p>JPA 어노테이션은 {@code jakarta.persistence.*} 의 정확한 타입 이름으로 비교한다.
 * Lombok 의 {@code @Setter}·{@code @Data} 는 바이트코드에 남지 않지만 만들어진 메서드는 남으므로, 메서드로 판단한다.
 */
@AnalyzeClasses(importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeArchives.class})
class SpringCodingRulesTest {

    private static final String ENTITY = "jakarta.persistence.Entity";
    private static final String ENUMERATED = "jakarta.persistence.Enumerated";
    private static final String MANY_TO_ONE = "jakarta.persistence.ManyToOne";
    private static final String ONE_TO_ONE = "jakarta.persistence.OneToOne";

    /** Spring 과 Jakarta 의 @Transactional. 둘 다 같은 규칙으로 본다. */
    private static final List<String> TRANSACTIONAL_ANNOTATIONS =
            List.of("org.springframework.transaction.annotation.Transactional", "jakarta.transaction.Transactional");

    @ArchTest
    static final ArchRule 필드_주입을_쓰지_않는다 =
            FreezingArchRule.freeze(GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION.because(
                    "생성자로 주입해야 의존을 숨기지 않고 테스트에서 직접 만들 수 있다 → docs/spring-java/architecture.md"));

    @ArchTest
    static final ArchRule controller와_repository에_Transactional을_두지_않는다 = FreezingArchRule.freeze(classes()
            .that()
            .resideInAnyPackage("..controller..", "..repository..")
            .should(notBeTransactionalNorHaveTransactionalMethods())
            .because("트랜잭션 경계는 service 에 둔다. controller 에 두면 응답 직렬화까지 트랜잭션이 길어진다" + " → docs/spring-java/data-jpa.md")
            .allowEmptyShould(true));

    // @Enumerated 의 기본값은 ORDINAL 이라 값을 적지 않은 것도 위반이다. enum 순서를 바꾸면 저장된 데이터의 뜻이 달라진다.
    @ArchTest
    static final ArchRule Enumerated는_STRING만_쓴다 = FreezingArchRule.freeze(fields().that()
            .areAnnotatedWith(ENUMERATED)
            .should(haveEnumValue(ENUMERATED, "value", "STRING"))
            .because("ORDINAL 은 enum 순서를 바꾸면 저장된 데이터의 뜻이 달라진다 → docs/spring-java/data-jpa.md")
            .allowEmptyShould(true));

    // @ManyToOne·@OneToOne 의 기본값은 EAGER 라 값을 적지 않은 것도 위반이다.
    @ArchTest
    static final ArchRule ToOne_연관관계는_지연_로딩이다 = FreezingArchRule.freeze(fields().that()
            .areAnnotatedWith(MANY_TO_ONE)
            .or()
            .areAnnotatedWith(ONE_TO_ONE)
            .should(haveFetchLazy())
            .because("@ManyToOne·@OneToOne 은 기본값이 즉시 로딩이라 fetch = FetchType.LAZY 를 직접 적는다"
                    + " → docs/spring-java/data-jpa.md")
            .allowEmptyShould(true));

    @ArchTest
    static final ArchRule 엔티티에_공개_setter를_두지_않는다 = FreezingArchRule.freeze(classes()
            .that()
            .areAnnotatedWith(ENTITY)
            .should(notHavePublicSetters())
            .because("setter 로 상태를 바꾸면 불변식을 지킬 수 없다. 의도가 드러나는 메서드로 바꾼다 → docs/spring-java/data-jpa.md")
            .allowEmptyShould(true));

    @ArchTest
    static final ArchRule ServiceImpl_클래스를_두지_않는다 = FreezingArchRule.freeze(noClasses()
            .should()
            .haveSimpleNameEndingWith("ServiceImpl")
            .because("구현이 하나뿐인 서비스에 인터페이스와 Impl 을 나누지 않는다 → docs/spring-java/architecture.md"));

    /** 클래스 자체나 그 클래스가 선언한 메서드에 @Transactional 이 있으면 위반이다. */
    private static ArchCondition<JavaClass> notBeTransactionalNorHaveTransactionalMethods() {
        return new ArchCondition<>("@Transactional 을 클래스나 메서드에 두지 않는다") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (String annotation : TRANSACTIONAL_ANNOTATIONS) {
                    if (javaClass.isAnnotatedWith(annotation)) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass, javaClass.getDescription() + " 에 @Transactional 이 있다"));
                    }
                    for (JavaMethod method : javaClass.getMethods()) {
                        if (method.isAnnotatedWith(annotation)) {
                            events.add(SimpleConditionEvent.violated(
                                    method, method.getDescription() + " 에 @Transactional 이 있다"));
                        }
                    }
                }
            }
        };
    }

    /** 어노테이션 속성이 주어진 enum 상수가 아니면 위반이다. 속성을 적지 않았으면(기본값이 쓰이면) 위반이다. */
    private static ArchCondition<JavaField> haveEnumValue(String annotationType, String property, String expected) {
        return new ArchCondition<>("@" + simpleName(annotationType) + " 의 " + property + " 가 " + expected + " 이다") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                String actual =
                        explicitEnumValue(field, annotationType, property).orElse("(미지정)");
                if (!actual.equals(expected)) {
                    events.add(SimpleConditionEvent.violated(
                            field,
                            field.getDescription() + " 의 @" + simpleName(annotationType) + " " + property + " 가 "
                                    + actual + " 이다 (기대값 " + expected + ")"));
                }
            }
        };
    }

    private static ArchCondition<JavaField> haveFetchLazy() {
        return new ArchCondition<>("fetch 가 LAZY 이다") {
            @Override
            public void check(JavaField field, ConditionEvents events) {
                for (String annotationType : List.of(MANY_TO_ONE, ONE_TO_ONE)) {
                    if (!field.isAnnotatedWith(annotationType)) {
                        continue;
                    }
                    String actual =
                            explicitEnumValue(field, annotationType, "fetch").orElse("(미지정, 기본값 EAGER)");
                    if (!actual.equals("LAZY")) {
                        events.add(SimpleConditionEvent.violated(
                                field,
                                field.getDescription() + " 의 @" + simpleName(annotationType) + " fetch 가 " + actual
                                        + " 이다. fetch = FetchType.LAZY 로 적는다"));
                    }
                }
            }
        };
    }

    /** public 인 인스턴스 메서드 가운데 set + 대문자로 시작하고 파라미터가 하나인 것이 있으면 위반이다. */
    private static ArchCondition<JavaClass> notHavePublicSetters() {
        return new ArchCondition<>("public setter 를 갖지 않는다") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaMethod method : javaClass.getMethods()) {
                    if (isPublicSetter(method)) {
                        events.add(SimpleConditionEvent.violated(
                                method, method.getDescription() + " 는 엔티티의 public setter 이다"));
                    }
                }
            }
        };
    }

    private static boolean isPublicSetter(JavaMethod method) {
        String name = method.getName();
        return method.getModifiers().contains(JavaModifier.PUBLIC)
                && !method.getModifiers().contains(JavaModifier.STATIC)
                && !method.getModifiers().contains(JavaModifier.SYNTHETIC)
                && method.getRawParameterTypes().size() == 1
                && name.length() > 3
                && name.startsWith("set")
                && Character.isUpperCase(name.charAt(3));
    }

    /** 소스에 적은 어노테이션 속성의 enum 상수 이름. 적지 않았으면 비어 있다. */
    private static Optional<String> explicitEnumValue(JavaField field, String annotationType, String property) {
        JavaAnnotation<JavaField> annotation = field.getAnnotationOfType(annotationType);
        return annotation
                .tryGetExplicitlyDeclaredProperty(property)
                .flatMap(value ->
                        value instanceof JavaEnumConstant constant ? Optional.of(constant.name()) : Optional.empty());
    }

    private static String simpleName(String typeName) {
        return typeName.substring(typeName.lastIndexOf('.') + 1);
    }
}
