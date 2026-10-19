# mopl 백엔드 코드 컨벤션

본 문서는 mopl 백엔드 코드의 일관성과 품질을 유지하기 위한 컨벤션 가이드입니다.
CodeRabbit 자동 리뷰가 본 문서를 참고하므로, 본 문서를 따르지 않는 코드는 PR 단계에서 지적될 수 있습니다.

---

## 1. 적용 범위

- mopl 백엔드(`src/main/java/**`, `src/test/java/**`, `src/main/resources/**`).
- 본 컨벤션과 충돌하는 외부 라이브러리 권장 사항이 있을 경우, **공식 가이드를 우선**한다.
- 본 컨벤션은 살아 있는 문서이며, 변경은 PR로 진행한다.

---

## 2. 패키지 구조

메인 애플리케이션 클래스를 최상위 패키지에 두고 도메인 기반으로 패키지를 구성한다.

```
src/main/java/com/codeit/team5/mopl/
├── MoplApplication.java
├── global/                  # 공통 모듈 (여러 도메인이 공유)
│   ├── config/
│   ├── exception/
│   └── util/
├── user/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── exception/
└── playlist/
    ├── controller/
    ├── service/
    ├── repository/
    ├── entity/
    ├── dto/
    └── exception/
```

### 규칙

- 도메인 단위로 묶고, 도메인 내부에 계층별 패키지를 둔다.
- 한 도메인에서만 쓰이는 DTO/예외는 해당 도메인 내부에 둔다.
- 여러 도메인이 공유하는 모듈은 `global/`에 둔다.
- 메인 애플리케이션 클래스(`@SpringBootApplication`)는 최상위 패키지에 둔다.

---

## 3. 네이밍 컨벤션

| 대상 | 규칙 | 예시 |
|---|---|---|
| 패키지 | 소문자, 점 구분 | `com.codeit.team5.mopl.user` |
| 클래스/인터페이스 | UpperCamelCase | `UserService`, `Playlist` |
| 메서드 | lowerCamelCase, 동사 시작 | `findById`, `createPlaylist` |
| 상수 | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 일반 변수 | lowerCamelCase | `userId` |

### 계층별 접미사

| 접미사 | 용도 |
|---|---|
| `Controller` | REST 컨트롤러 구현체 |
| `Api` | REST 컨트롤러 인터페이스 (Swagger 문서화 전용) |
| `Service` | 비즈니스 로직 클래스 |
| `Repository` | 데이터 영속성 인터페이스 |
| `Request` | 요청 DTO |
| `Response` | 응답 DTO |
| `Mapper` | MapStruct 매퍼 인터페이스 |
| `Exception` | 커스텀 예외 |

---

## 4. 코드 스타일

- 들여쓰기: **4 spaces** (tab 금지).
- continuation indent: **8 spaces**.
- 한 줄 최대 길이: **120 chars**.
- 파일 끝 개행: **필수**.
- IntelliJ 포맷터 적용 후 push (`option + command + L`).
- 유틸/상수 클래스는 인스턴스화를 막는다 (`private` 생성자 또는 `@NoArgsConstructor(access = AccessLevel.PRIVATE)`).

---

## 5. 빈(Bean) 주입

**생성자 주입**을 사용한다.

### 규칙

- **생성자 주입만 사용한다.** 필드 주입(`@Autowired`)과 setter 주입은 금지.
- Lombok `@RequiredArgsConstructor`로 보일러플레이트 제거.
- 의존성은 `private final`로 선언.

### 예시

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
}
```

### 이유

- 불변 객체 보장 (`final`).
- 순환 의존성을 컴파일 시점에 발견.
- 테스트 시 mock 주입 용이.
- Spring 컨테이너 없이도 객체 생성 가능.

---

## 6. 트랜잭션

### 규칙

- 서비스 클래스에 `@Transactional(readOnly = true)`를 **기본**으로 적용.
- CUD(Create/Update/Delete) 메서드에는 메서드 레벨로 `@Transactional`을 추가하여 오버라이드.
- `@Transactional`은 **public 메서드**에만 적용. (Spring AOP 프록시 한계)

### 예시

```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;

    public Playlist findById(Long id) {
        return playlistRepository.findById(id)
            .orElseThrow(() -> new PlaylistNotFoundException(id));
    }

    @Transactional
    public Playlist create(PlaylistCreateRequest request) {
        // ...
    }
}
```

### 이유

- 읽기 전용 트랜잭션은 영속성 컨텍스트의 더티 체킹을 생략 → 성능 향상.
- 메서드 레벨 변경만 보면 어떤 메서드가 쓰기 동작인지 즉시 파악 가능.

---

## 7. JPA / 엔티티

### 엔티티 작성 규칙

```java
@Entity
@Table(name = "playlists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "title"})
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

### 규칙

- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 사용 (JPA reflection 호출, 외부 직접 생성 차단).
- `@EqualsAndHashCode(of = "id")`로 ID 기반 동등성 비교.
- `@ToString`에 양방향 연관관계 필드 포함 금지(무한 순환 방지).
- `@Data` 사용 금지 — Setter 자동 생성으로 불변성 깨짐.
- `@Column`의 `nullable`, `length`, `unique` 옵션 명시.
- 양방향 연관관계는 정말 필요할 때만. 단방향 우선.
- `FetchType.EAGER` 사용 금지. **기본 LAZY**.
- 비즈니스 로직은 엔티티의 도메인 메서드로 캡슐화 (Setter 직접 사용 지양).

### 엔티티 생성 — 정적 팩토리 메서드

- 엔티티 인스턴스 생성은 **정적 팩토리 메서드**로 한다. 관용적으로 `create`를 쓰며 `of`도 허용한다.
- 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 둔다.
  (Jakarta Persistence 표준상 엔티티 no-arg 생성자는 public/protected만 허용 — `private` 불가)
  외부에서 `new`로 직접 만들지 못하게 막고, 생성은 정적 팩토리로만 한다.
- 빌더(`@Builder`)보다 정적 팩토리를 우선한다 — 생성 의도를 메서드명으로 드러낸다.

```java
public static User create(String email, String name, String profileImageUrl) {
    User user = new User();
    user.email = email;
    user.name = name;
    user.profileImageUrl = profileImageUrl;
    return user;
}
```

### N+1 방지

- 컬렉션 조회 시 `@EntityGraph` 또는 `JOIN FETCH` 사용.
- `OneToMany` 컬렉션 조회에 페이지네이션 결합 시 주의 (Hibernate가 메모리 페이징으로 전환되며 경고 로그 출력).
- 조회 패턴에 맞는 인덱스를 둔다.

---

## 8. DTO

### 위치 및 분류

- 도메인 패키지 내부 `dto/` 하위.
- 요청 DTO: `Request` 접미사.
- 응답 DTO: `Response` 접미사.

### 작성 규칙

- DTO는 `record`로 작성. (Java 16+ 표준, 불변성 자동 보장)
- DTO에 비즈니스 로직 포함 금지.
- DTO에 정적 팩토리 메서드(`from`, `of`) 작성 금지 — Entity ↔ DTO 변환은 MapStruct가 전담한다.
- 필드 검증 어노테이션(`@NotBlank`, `@NotNull` 등) 적극 사용.

### 예시

```java
public record PlaylistCreateRequest(
    @NotBlank @Size(max = 100) String title,
    @Size(max = 500) String description
) {
}

public record PlaylistResponse(
    Long id,
    String title,
    String description,
    LocalDateTime createdAt
) {
}
```

---

## 9. Entity ↔ DTO 변환 (MapStruct)

### 규칙

- Entity ↔ DTO 변환은 **MapStruct 매퍼**가 전담.
- 매퍼는 도메인 패키지 내부 `mapper/`에 둔다.
- `componentModel = "spring"`로 Spring Bean 자동 등록.

### 매퍼 작성

```java
@Mapper(componentModel = "spring")
public interface PlaylistMapper {

    PlaylistResponse toResponse(Playlist playlist);

    List<PlaylistResponse> toResponseList(List<Playlist> playlists);

    Playlist toEntity(PlaylistCreateRequest request);
}
```

- 단순 이름 매핑이면 어노테이션 추가 없이도 자동 동작.
- 필드명이 다르면 `@Mapping(source = "x", target = "y")`로 명시.
- Lombok과 함께 사용할 경우 `annotationProcessor` 순서가 `lombok → lombok-mapstruct-binding → mapstruct-processor`여야 한다. (현재 `build.gradle`에 설정됨)

---

## 10. 검증 (Validation)

### 3계층 검증 매트릭스

| 검증 종류 | 위치 | 방법 | 예시 |
|---|---|---|---|
| 형식 검증 | DTO | Bean Validation 어노테이션 | `@NotBlank`, `@Email`, `@Size` |
| 비즈니스 규칙 검증 | 도메인(Entity, VO) | 메서드 내부 검증 | 상태 전이, 권한 검사 |
| DB 제약 검증 | Entity | `@Column` 옵션 | `nullable = false`, `unique = true` |

### Controller에서 검증 활성화

```java
@PostMapping("/playlists")
public ResponseEntity<PlaylistResponse> create(
    @Valid @RequestBody PlaylistCreateRequest request
) {
    // ...
}
```

- `@RequestBody`에는 반드시 `@Valid`를 붙인다.
- `@PathVariable`, `@RequestParam`에는 클래스 레벨 `@Validated`와 함께 제약 어노테이션 직접 부착.

### 비즈니스 규칙 예시

```java
public class Playlist {

    public void rename(String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            throw new IllegalArgumentException("제목은 비어 있을 수 없습니다.");
        }
        if (newTitle.length() > 100) {
            throw new IllegalArgumentException("제목은 100자를 초과할 수 없습니다.");
        }
        this.title = newTitle;
    }
}
```

---

## 11. 예외 처리

### 규칙

- 도메인별 커스텀 예외는 `domain/{name}/exception/` 패키지에 정의.
- 공통 추상 예외(`BusinessException`)를 정의하고 도메인 예외가 상속.
- 예외는 상황별로 **세분화**하여 정의한다. 포괄적 예외 하나로 뭉치지 않는다.
  (운영 환경에서 원인을 정확히 식별하기 위함)
- **비즈니스 규칙 위반 예외만** `BusinessException`을 상속한다 (주로 4xx).
  인프라/외부 연동 장애(Redis, S3, Kafka 등)는 비즈니스 예외가 아니므로
  `BusinessException`을 상속하지 않고 5xx로 별도 처리한다.
  (단, 잘못된 입력으로 인한 실패 — 파일 형식/크기 위반 등 — 는 클라이언트 책임이므로 BusinessException(4xx))
- 전역 예외 처리는 `global/exception/GlobalExceptionHandler` (`@RestControllerAdvice`).
- 커스텀 예외 메시지에는 식별자(ID 등) 포함.

### 예시

```java
public abstract class BusinessException extends RuntimeException {
    protected BusinessException(String message) {
        super(message);
    }
}

public class PlaylistNotFoundException extends BusinessException {
    public PlaylistNotFoundException(Long id) {
        super("Playlist not found: id=" + id);
    }
}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PlaylistNotFoundException.class)
    public ResponseEntity<?> handle(PlaylistNotFoundException e) {
        // ...
    }
}
```

### 금지

- 컨트롤러에서 `try-catch`로 예외 처리 금지 (전역 핸들러에 위임).
- `RuntimeException`을 그대로 throw 금지 (의미 있는 커스텀 예외로).
- 예외를 catch만 하고 무시 금지 (`catch (Exception ignored)`).

---

## 12. REST API 설계

### URI 설계

- 명사 사용 (동사 금지). `/users` ✅, `/getUsers` ❌
- 복수형 사용. `/users` ✅, `/user` ❌
- kebab-case 사용. `/order-items` ✅, `/orderItems` ❌
- API 버전을 URI 경로에 명시. `/api/v1/users`
- 리소스 계층은 2단계 이하 권장. `/users/{id}/playlists` ✅, `/users/{id}/playlists/{pid}/songs/{sid}/...` ❌

### HTTP 메서드 매핑

| 메서드 | 용도 | 멱등성 |
|---|---|---|
| GET | 조회 | ✅ |
| POST | 생성 (또는 비멱등 액션) | ❌ |
| PUT | 전체 수정 | ✅ |
| PATCH | 부분 수정 | ❌ |
| DELETE | 삭제 | ✅ |

### 상태 코드

| 코드 | 의미 |
|---|---|
| 200 OK | 정상 처리 (조회/수정) |
| 201 Created | 정상 생성 (POST 후) |
| 204 No Content | 정상 처리 후 응답 본문 없음 (DELETE) |
| 400 Bad Request | 클라이언트 요청 오류 (검증 실패 등) |
| 401 Unauthorized | 인증 필요 |
| 403 Forbidden | 권한 부족 |
| 404 Not Found | 리소스 없음 |
| 409 Conflict | 리소스 충돌 (중복 등) |
| 500 Internal Server Error | 서버 오류 |

### Controller 작성 규칙

```java
@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
@Slf4j
public class PlaylistController implements PlaylistApi {

    private final PlaylistService playlistService;

    @Override
    @PostMapping
    public ResponseEntity<PlaylistResponse> create(
        @Valid @RequestBody PlaylistCreateRequest request
    ) {
        log.info("Request API: POST /api/v1/playlists, title={}", request.title());
        PlaylistResponse response = playlistService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### 규칙

- 컨트롤러 구현체는 인터페이스(`*Api`)를 구현. 인터페이스에 Swagger 어노테이션 작성.
- 인터페이스(`*Api`)는 `@Tag`, `@Operation`, `@ApiResponses`로 문서화한다.
  `@ApiResponse`는 성공·실패(200/400/404 등)를 모두 정의하고, `@Parameter`로 파라미터를 설명한다.
- `ResponseEntity` 사용으로 상태 코드와 헤더 명시.
- 요청 진입 시점 로깅 (`log.info("Request API: ...")`).
- 비즈니스 로직은 서비스 계층에. 컨트롤러는 진입점 역할만.

---

## 13. 로깅

### 규칙

- SLF4J 사용 (Spring Boot 기본). Lombok `@Slf4j` 사용 권장.
- 로그 메시지에 `String.format` 또는 문자열 연결 사용 금지. **placeholder `{}`** 사용.

```java
log.info("Playlist created: id={}, title={}", playlist.getId(), playlist.getTitle());  // ✅
log.info("Playlist created: id=" + playlist.getId());                                    // ❌
```

### 로그 레벨

| 레벨 | 용도 |
|---|---|
| `ERROR` | 시스템 오류, 즉시 조치 필요 |
| `WARN` | 잠재적 문제, 비정상 흐름 감지 |
| `INFO` | 비즈니스 이벤트, API 진입/완료 |
| `DEBUG` | 개발자용 상세 정보 |
| `TRACE` | 매우 상세한 내부 추적 |

### 보안

- 비밀번호, 토큰, 주민번호 등 민감 정보 **로깅 금지**.
- 사용자 입력 직접 로깅 시 로그 인젝션(개행 문자 등) 주의.

### 데이터 영속화 후 로깅

```java
@Transactional
public Playlist create(PlaylistCreateRequest request) {
    Playlist saved = playlistRepository.save(playlistMapper.toEntity(request));
    log.info("Playlist saved: id={}", saved.getId());
    return saved;
}
```

---

## 14. 테스트

### 테스트 전략

| 테스트 유형 | 대상 | 작성 기준 |
|---|---|---|
| 슬라이스 테스트 | Repository(`@DataJpaTest`), Controller(`@WebMvcTest`) | 계층별 단위 검증 |
| 단위 테스트 | Service / 도메인 로직 | 복잡한 비즈니스 로직 |
| 통합 테스트 | 전체 흐름 (Controller~DB) | 모든 API 엔드포인트 |

> Controller는 **슬라이스 테스트(`@WebMvcTest`)와 통합 테스트(Testcontainers + MockMvc)를 모두** 작성한다.
> 슬라이스로 컨트롤러 계층을 단위 검증하고, 통합으로 전체 흐름을 별도 검증한다.

### 통합 테스트 (Testcontainers)

- `@Import(TestcontainersConfiguration.class)` 적용.
- `@ActiveProfiles("test")` 적용.
- 상태 코드, 응답 본문, DB 상태 검증.

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class PlaylistApiTest {
    // ...
}
```

> 반복 작성을 줄이려면 추상 베이스 클래스(`IntegrationTestBase`)나 메타 어노테이션을 도입한다.

### 단위 테스트

- Mockito로 의존성 모킹.
- Given-When-Then 패턴.
- 한국어 `@DisplayName` 사용.

```java
@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @InjectMocks
    private PlaylistService playlistService;

    @DisplayName("플레이리스트 생성 성공")
    @Test
    void createPlaylist_성공() {
        // given
        PlaylistCreateRequest request = new PlaylistCreateRequest("My List", null);
        Playlist saved = new Playlist(1L, "My List", null);
        given(playlistRepository.save(any())).willReturn(saved);

        // when
        PlaylistResponse response = playlistService.create(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("My List");
    }
}
```

### 규칙

- 테스트 패키지 구조 = 메인 패키지 구조.
- 한국어 `@DisplayName` 사용 권장.
- `@DisplayName` 규칙: **테스트 메서드**는 `~성공`/`~실패`로 끝낸다.
  `@Nested`·클래스 레벨은 테스트 그룹/대상을 나타내는 이름이므로 성공/실패 suffix를 붙이지 않는다.
- 메서드 이름: `<동작>_<결과>` 패턴 (예: `createPlaylist_성공`, `findById_NotFound`).
- 성공 케이스만 작성 금지 — 실패/예외 케이스도 작성.
- AssertJ 사용 권장 (가독성 좋음).

### 금지

- `@MockBean` 남용 금지 — 컨텍스트 재로딩 비용 큼. 단위 테스트에선 Mockito로.
- `Thread.sleep` 기반 비동기 테스트 금지 — Awaitility 등 폴링 라이브러리 사용.

---

## 15. Flyway 마이그레이션

### 위치 및 네이밍

- `src/main/resources/db/migration/`
- 형식: `V{버전}__{스네이크_케이스_설명}.sql`
- 예: `V1__init_schema.sql`, `V2__add_playlists_table.sql`

### 규칙

- 이미 적용된 마이그레이션 파일은 **절대 수정하지 않는다**. 새 마이그레이션을 추가.
- 한 마이그레이션 = 한 논리적 변경.
- `ddl-auto: validate` 모드 — 엔티티 변경 시 반드시 마이그레이션 파일 추가.
- 운영에 영향 가는 변경(컬럼 삭제, 타입 변경)은 PR 본문에 명시.

### 예시

```sql
-- V2__add_playlists_table.sql
CREATE TABLE playlists (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_playlists_created_at ON playlists (created_at DESC);
```

---

## 16. 환경 설정 (application.yml)

### 규칙

- 환경별 분리 (`application-dev.yml`, `application-prod.yml`, `application-test.yml`).
- 민감 정보(비밀번호, API 키, DB URL 등) **하드코딩 금지**.
- 운영 환경 값은 `${ENV_VAR}` 형태로 환경변수 주입.
- `@ConfigurationProperties`로 타입 안전한 설정 객체 사용 권장.
