# Spring 테스트

Spring Boot 4 기준이다. 테스트 원칙(given/when/then, AssertJ, 재현 테스트)은 Java 프리셋 문서를 따른다.
이 문서는 Spring 이 필요한 테스트를 어떻게 띄우는지 정한다.

## 테스트 종류

Spring 컨텍스트를 띄우는 테스트는 느리다. 가능한 한 아래 표의 위쪽 방법을 쓴다.

| 대상 | 방법 |
| --- | --- |
| 도메인 객체, 서비스 | Spring 없이 단위 테스트. 서비스의 Repository·외부 연동은 Mockito 로 대체 |
| 컨트롤러 (요청·검증·응답 형식) | Spring 없이 `MockMvcBuilders.standaloneSetup` 과 `MockMvc`. 서비스는 Mockito `mock()`, 오류 응답은 `setControllerAdvice(new GlobalExceptionHandler())` |
| Repository, 쿼리 | `@SpringBootTest` 와 `TestcontainersConfiguration` 으로 띄운 운영과 같은 MySQL. 테스트마다 `@Transactional` 로 되돌린다 |
| 여러 레이어를 잇는 흐름 (인증·필터 포함) | `@SpringBootTest` 와 `@AutoConfigureMockMvc`. 핵심 시나리오에만 쓴다 |

orbit 은 `@WebMvcTest`·`MockMvcTester`·`@DataJpaTest` 를 쓰지 않는다. 기존 테스트와 같은 방식으로 맞추고, 바꾸려면 별도 이슈로 다룬다.

## 규칙

- `@MockBean`·`@SpyBean` 은 Spring Boot 4 에서 제거됐다. `@MockitoBean`·`@MockitoSpyBean` 을 쓴다.
- 기술별 테스트 starter(`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test` 등)를 `testImplementation` 으로 둔다.
- `@SpringBootTest` 에서 MockMvc 를 쓰려면 `@AutoConfigureMockMvc` 를 붙인다.
- DB 는 H2 같은 다른 DB 로 대신하지 않는다. 방언·제약·함수가 달라 운영에서만 실패하는 쿼리를 놓친다.
- Testcontainers 컨테이너는 `@ServiceConnection` 을 붙인 `@TestConfiguration` 빈으로 두고 테스트에서 `@Import` 한다.
  같은 설정의 테스트끼리 컨텍스트 캐시를 함께 써서 컨테이너를 한 번만 띄운다.
- 테스트마다 `@MockitoBean` 조합이나 설정이 다르면 컨텍스트를 새로 띄운다. 공통 조합은 기반 클래스나 설정으로 모은다.
- 의존 방향·클래스 위치·기능 간 순환은 설치된 `src/test/java/LayerArchitectureTest.java` 가 ArchUnit 으로 검사한다. 일반 `./gradlew test` 에 포함된다. → [architecture.md](architecture.md)
- 기존 위반은 ArchUnit 기준선(`src/test/resources/archunit_store`)에 두고 새 위반만 막는다. 기준선을 손으로 고치거나 지우지 않는다.

## Spring 코드 규칙 테스트

설치된 `src/test/java/SpringCodingRulesTest.java` 가 바이트코드로 판단할 수 있는 Spring·JPA 코드 규칙을 ArchUnit 으로 검사한다.
컴파일된 main 클래스만 읽고, 루트 패키지를 적을 필요가 없다. 기준선 방식과 만드는 절차는 `LayerArchitectureTest` 와 같다. Java 프리셋의 `docs/java/lint-and-ci.md` 에 적혀 있다.

| 규칙 | 근거 문서 |
| --- | --- |
| 필드 주입(`@Autowired` 필드)을 쓰지 않는다 | [architecture.md](architecture.md) |
| `@Transactional`(Spring·Jakarta)을 `..controller..`·`..repository..` 패키지의 클래스와 메서드에 두지 않는다 | [data-jpa.md](data-jpa.md) |
| `@Enumerated` 는 `EnumType.STRING` 만 쓴다. 값을 적지 않으면 기본값이 `ORDINAL` 이라 위반이다 | [data-jpa.md](data-jpa.md) |
| `@ManyToOne`·`@OneToOne` 은 `fetch = FetchType.LAZY` 를 적는다. 적지 않으면 기본값이 즉시 로딩이라 위반이다 | [data-jpa.md](data-jpa.md) |
| `@Entity` 클래스에 `setXxx` public 메서드를 두지 않는다. Lombok `@Setter`·`@Data` 는 만들어진 메서드로 판단한다 | [data-jpa.md](data-jpa.md) |
| 이름이 `ServiceImpl` 로 끝나는 클래스를 두지 않는다 | [architecture.md](architecture.md) |

`@ToString`·`@EqualsAndHashCode`, 로그 문자열 연결, 트랜잭션 안의 외부 호출처럼 바이트코드로 판단하기 어려운 규칙은 문서와 리뷰에 맡긴다.

## 예시

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
    }
}

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class CampaignRepositoryTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Test
    @DisplayName("같은 대상 조건과 목표 버전의 활성 캠페인이 있으면 찾는다")
    void findsActiveCampaign() { ... }
}

class CampaignAdminControllerTest {

    private MockMvc mockMvc;
    private CampaignRegistrationService campaignRegistrationService;

    @BeforeEach
    void setUp() {
        campaignRegistrationService = mock(CampaignRegistrationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CampaignAdminController(campaignRegistrationService, ...))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("이름이 비어 있으면 400 을 돌려준다")
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/admin/campaigns").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "", "artifactId": 1}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }
}
```
