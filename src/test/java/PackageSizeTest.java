import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 패키지 하나에 클래스가 너무 많이 쌓이지 않았는지 검사한다.
 *
 * 루트 패키지를 몰라도 되도록 이 클래스는 기본 패키지에 두고, 패키지를 지정하지 않는다.
 * 대신 테스트 클래스와 jar·jrt 를 빼서 프로젝트의 main 클래스만 읽는다.
 * 바이트코드 기준이라 컴파일된 main 클래스만 센다. 최상위 클래스만 세고 중첩·익명·합성 클래스와 package-info 는 세지 않는다.
 *
 * 크기 초과로 실패하면 docs/java/package-structure.md 의 순서대로 패키지를 나눈다.
 */
@AnalyzeClasses(importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeArchives.class})
class PackageSizeTest {

    /** 패키지 하나에 둘 수 있는 클래스 수의 기본 상한. 프로젝트에 맞게 바꿀 수 있다. */
    private static final int MAX_CLASSES_PER_PACKAGE = 15;

    /**
     * 이 테스트를 도입할 때 이미 상한을 넘어 있던 패키지와 그 시점의 클래스 수. 키는 패키지 전체 이름이다. 예: "com.x.monitoring.service" → 28
     *
     * <ul>
     *   <li>도입 시점에만 채운다. 나중에 패키지가 커졌다고 여기에 더하지 않는다.
     *   <li>적힌 수를 올리지 않는다. 넘으면 패키지를 나눈다.
     *   <li>패키지가 {@link #MAX_CLASSES_PER_PACKAGE} 이하로 줄거나 사라지면 테스트가 실패하니 항목을 지운다.
     * </ul>
     */
    private static final Map<String, Integer> KNOWN_LARGE_PACKAGES = Map.of();

    private enum Role {
        CONFIG("config"),
        CONTROLLER("controller"),
        SERVICE("service"),
        REPOSITORY("repository"),
        ENTITY("entity"),
        RECORD("record"),
        OTHER("기타");

        private final String label;

        Role(String label) {
            this.label = label;
        }
    }

    @ArchTest
    static void 패키지의_클래스_수가_상한을_넘지_않는다(JavaClasses classes) {
        Map<String, List<JavaClass>> byPackage = classes.stream()
                .filter(PackageSizeTest::isCountable)
                .collect(Collectors.groupingBy(JavaClass::getPackageName, TreeMap::new, Collectors.toList()));

        List<String> violations = new ArrayList<>();
        byPackage.forEach((packageName, members) -> {
            int limit = KNOWN_LARGE_PACKAGES.getOrDefault(packageName, MAX_CLASSES_PER_PACKAGE);
            if (members.size() > limit) {
                violations.add(oversizeMessage(packageName, members, limit));
            }
        });
        KNOWN_LARGE_PACKAGES.keySet().forEach(packageName -> {
            int size = byPackage.getOrDefault(packageName, List.of()).size();
            if (size <= MAX_CLASSES_PER_PACKAGE) {
                violations.add(staleEntryMessage(packageName, size));
            }
        });

        if (!violations.isEmpty()) {
            throw new AssertionError("\n" + String.join("\n\n", violations));
        }
    }

    /** 최상위 클래스만 센다. 중첩·익명·지역 클래스와 package-info, module-info 는 파일 수와 어긋나므로 뺀다. */
    private static boolean isCountable(JavaClass javaClass) {
        String simpleName = javaClass.getSimpleName();
        return javaClass.isTopLevelClass() && !simpleName.equals("package-info") && !simpleName.equals("module-info");
    }

    private static String oversizeMessage(String packageName, List<JavaClass> members, int limit) {
        return "패키지 " + packageName + " 의 클래스가 " + members.size() + "개로 상한 " + limit + "개를 넘었습니다"
                + " (기본 상한 " + MAX_CLASSES_PER_PACKAGE + ").\n"
                + "역할 분포: " + roleDistribution(members) + "\n"
                + "나누는 순서: 1) 역할이 섞여 있으면 역할 단위 하위 패키지로"
                + " 2) 한 역할 안에서도 크면 기능 단위 하위 패키지로"
                + " 3) 컨텍스트 경계가 의심되면 사람에게 묻는다\n"
                + "→ docs/java/package-structure.md";
    }

    private static String staleEntryMessage(String packageName, int size) {
        String state = size == 0 ? "클래스가 없습니다(이름이 바뀌었거나 사라졌습니다)" : "클래스가 " + size + "개로 기본 상한 이하입니다";
        return "KNOWN_LARGE_PACKAGES 의 패키지 " + packageName + " 는 " + state + ".\n"
                + "KNOWN_LARGE_PACKAGES 에서 지우세요.\n"
                + "→ docs/java/package-structure.md";
    }

    /** 역할별 클래스 수를 "config 3, service 7" 꼴로 만든다. 0개인 역할은 적지 않는다. */
    private static String roleDistribution(List<JavaClass> members) {
        Map<Role, Integer> counts = new EnumMap<>(Role.class);
        for (JavaClass member : members) {
            counts.merge(classify(member), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .map(entry -> entry.getKey().label + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    /** 먼저 맞는 역할 하나로 분류한다. 어노테이션은 패키지를 빼고 단순 이름이 정확히 같은지로 비교한다. */
    private static Role classify(JavaClass javaClass) {
        Set<String> annotations = javaClass.getAnnotations().stream()
                .map(JavaAnnotation::getRawType)
                .map(JavaClass::getSimpleName)
                .collect(Collectors.toSet());

        if (annotations.contains("Configuration") || annotations.contains("ConfigurationProperties")) {
            return Role.CONFIG;
        }
        if (annotations.contains("RestController") || annotations.contains("Controller")) {
            return Role.CONTROLLER;
        }
        if (annotations.contains("Service") || annotations.contains("Component")) {
            return Role.SERVICE;
        }
        if (annotations.contains("Repository")
                || javaClass.isAssignableTo("org.springframework.data.repository.Repository")) {
            return Role.REPOSITORY;
        }
        if (annotations.contains("Entity") || annotations.contains("Embeddable")) {
            return Role.ENTITY;
        }
        if (javaClass.isRecord()) {
            return Role.RECORD;
        }
        return Role.OTHER;
    }
}
