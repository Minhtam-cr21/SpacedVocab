# SpacedVocab — hướng dẫn cho Claude

REST API học từ vựng tiếng Anh bằng flashcard, lên lịch ôn bằng thuật toán **SM-2**. Dự án luyện tập để đưa lên GitHub làm portfolio.

- **Tiếp nhận dự án / setup Docker & DB:** [docs/HANDBOOK.md](docs/HANDBOOK.md) — đọc đầu tiên nếu mới vào.
- **Nghiệp vụ (nguồn sự thật):** [docs/domain/business-spec.md](docs/domain/business-spec.md) — actors, use case, entity, ràng buộc **R1–R9**, công thức SM-2.
- **Thiết kế UX/UI:** [docs/design/](docs/design/README.md) — màn **D1–D26**, ma trận quyền, ánh xạ màn hình ↔ API, UX copy.
- Đổi nghiệp vụ → sửa `business-spec.md` trước, rồi mới sửa code và tài liệu design.

## Stack
- Java 21, **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Actuator), Maven Wrapper.
- PostgreSQL 17 + **Flyway**. Test: JUnit 5, Testcontainers.
- Chạy bằng **Docker Compose**. CI: GitHub Actions (`.github/workflows/ci.yml`).
- Frontend ở `web/`: **React 19 + Vite** (JavaScript), giao diện **tiếng Việt**, mobile-first. Docker build ra nginx ở **cổng 3000**, nginx proxy `/api` → service `app` (cùng origin, không cần CORS).
  - Phong cách **"trà sữa vẽ tay"**: màu phẳng, **không gradient**, không nền trắng/đen thuần, viền mực + bóng khối. Token ở `web/src/styles/tokens.css` (thay cho `tokens.css` của prototype). Đối tượng: **mọi lứa tuổi từ 5+** → chữ gốc 18px, nút ≥ 52px.
  - Linh vật **robot "Bit"** (nhân vật tự vẽ, `web/src/mascot/`) **chỉ nói tiếng Anh**, câu chào theo giờ ở `greeting.js`. Không dùng linh vật/thương hiệu của bên khác.

## Lệnh thường dùng
```bash
docker compose up -d --build          # Postgres + API (http://localhost:8090) + web (http://localhost:3000)
cd web && npm run dev                 # frontend dev server http://localhost:5173 (proxy /api → 8090)
docker compose logs -f app            # xem log API
docker compose down                   # dừng (giữ dữ liệu); thêm -v để xóa DB
docker compose --profile tools up -d  # thêm Adminer ở http://localhost:8081
./mvnw verify                         # build + toàn bộ test (cần Docker cho Testcontainers)
python -m http.server 5500 --directory docs/design/prototype   # xem prototype UI
```
- **Cổng API là 8090**, không phải 8080: cổng 8080 trên máy đã có dự án khác dùng.
- Máy không có `java`/`mvn` trong PATH → build/test qua Docker hoặc IntelliJ. Chạy test trong container trên Windows:
  `docker run --rm -v "<repo>:/w" -v spacedvocab-m2:/root/.m2 -v //var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -w /w maven:3.9-eclipse-temurin-21 mvn -B verify`
- Secret (mật khẩu DB, JWT secret…) chỉ để trong `.env` / biến môi trường. `.env` **không commit**; thêm biến mới thì cập nhật `.env.example`.
  - `JWT_SECRET` bắt buộc (≥ 32 ký tự), thiếu thì app không khởi động. `ADMIN_EMAIL` / `ADMIN_PASSWORD` tạo admin đầu tiên khi khởi động — **không seed admin bằng migration**.
  - Giá trị cho test nằm ở `src/test/resources/application-test.yml` (giá trị giả, không phải secret thật).

## Cấu trúc code
Package gốc `com.spacedvocab`, chia theo layer:
```
controller  service  repository  entity  dto  config  exception  algorithm
```
- `dto`: Java `record`, **tách khỏi entity**; validate bằng `@Valid` + Bean Validation.
- `exception`: `AppException` (abstract, mang `ErrorCode`) → `ResourceNotFoundException` 404, `DuplicateResourceException` 409, `ForbiddenOperationException` 403, `BusinessRuleViolationException` 422, `UnauthorizedException` 401. `GlobalExceptionHandler` (`@RestControllerAdvice`) trả `{ code, message, field?, errors? }` (`errors` chỉ có khi `VALIDATION_FAILED`). Thêm `code` mới → thêm vào enum `ErrorCode` **và** bảng trong [docs/design/06-ux-copy.md](docs/design/06-ux-copy.md).
- `config/security`: JWT HS256 qua OAuth2 Resource Server. `UserJwtAuthenticationConverter` đọc lại user mỗi request (khóa tài khoản có hiệu lực ngay). Lỗi 401/403 của filter được chuyển về `GlobalExceptionHandler` qua `SecurityErrorHandler`. Controller lấy user bằng `@AuthenticationPrincipal AuthenticatedUser me`.
- Token: access JWT 15 phút; refresh token ngẫu nhiên 14 ngày, **chỉ lưu SHA-256**, xoay vòng mỗi lần refresh, dùng lại token cũ → thu hồi mọi phiên của user.
- `algorithm`: `SpacedRepetitionAlgorithm#next(SchedulingState, Quality)` (interface, Strategy, hàm thuần) → `Sm2Algorithm`. Value object `Quality`, `EasinessFactor`, `SchedulingState` nằm cùng package.
- Ôn tập (M3): `ReviewService` + `CardProgressRepository`. Thẻ **mới** = chưa có `CardProgress` (tạo ở lần chấm đầu). "Thư viện" (R6) = deck mình sở hữu **hoặc** deck PUBLIC/OFFICIAL đã subscribe (`DeckSubscription`). Đổi quy tắc thì sửa cùng lúc `DeckAccessService#isInLibrary` **và** các query `findDue`/`countDue`/`findNextReviewAt`/`findNewForLearner`/`countNewForLearner`. M4 chỉ còn thêm endpoint subscribe/hủy.
- Lộ trình (§6): `PathService` (learner) + `PathAdminService` (admin). Chặng mở do xong bài được ghi **khi đọc** lộ trình (`loadProgress`), không móc vào `ReviewService`. Bài vượt chặng là `StageTestAttempt` (rich model: `submit`, `isFailed`, `retryAt`). `PathControllerTests` tự tạo path riêng bằng `JdbcTemplate` cho mỗi test.
  - Nội dung bài: `src/main/resources/seed/cefr/<LEVEL>.txt` (nội dung **tự soạn**, không lấy từ site khác), `PathContentSeeder` nạp lúc khởi động thành deck OFFICIAL của admin (`ADMIN_EMAIL`); bài trùng tên deck thì bỏ qua. Hiện mới có **A1** (7 bài × 12 từ).
- Integration test review dùng **algorithm giả** (`@TestConfiguration` + `@Primary` trong `ReviewControllerTests`), không phụ thuộc SM-2. Muốn dời thời gian trong test thì sửa `card_progress.next_review_at` bằng `JdbcTemplate` (không thay `Clock` vì JWT dùng chung Clock).

## Quy tắc OOP / thiết kế
- **Constructor injection**, không dùng `@Autowired` trên field. Log bằng **SLF4J**.
- **Rich domain model:** `CardProgress` không có setter cho EF/interval/repetitions; chỉ thay đổi qua `applyReview(quality, algorithm, clock)`.
- **Value object** bằng `record` tự kiểm tra trong constructor: `Quality` (0..5), `EasinessFactor` (≥ 1.3).
- **Tách `Card` (nội dung dùng chung) khỏi `CardProgress` (tiến độ từng user)** — không gộp lại.
- `ReviewLog` bất biến, không bao giờ xóa (R8).
- Inject `java.time.Clock` để test được logic ngày giờ; "ngày học" tính theo `timezone` của user (R7, R9).
- Kiểm tra quyền sở hữu qua interface `Ownable#isOwnedBy(userId)`. Deck PRIVATE của người khác trả **404**, không trả 403.
  - Mọi truy cập deck đi qua `DeckAccessService`: `requireVisible` (404) và `requireEditable` (403). Quy tắc nằm trong `Deck#isVisibleTo` / `Deck#isEditableBy` (OFFICIAL chỉ Admin sửa).
- Deck và Card **xóa mềm** (`deletedAt`); mọi query phải lọc `deletedAt is null`. Unique R2 là partial index `lower(...) WHERE deleted_at IS NULL`.
- Nội dung thẻ là value object `CardContent` (`@Embeddable record`).
- Danh sách phân trang trả `PageResponse` (`page` tính từ 0), không trả thẳng `Page` của Spring.

## Database
- Schema do **Flyway** quản lý, Hibernate chỉ `ddl-auto: validate`.
- **Không bao giờ sửa migration đã chạy** — luôn thêm file mới `V{n}__mo_ta.sql` trong `src/main/resources/db/migration`.
- Thời gian lưu **UTC** (`TIMESTAMPTZ`).

## Test
- Unit test SM-2 bằng `@ParameterizedTest` với các bộ (trạng thái, quality) → kết quả mong đợi theo `business-spec.md` §5.
- Integration test gắn meta-annotation `@IntegrationTest` (SpringBootTest + MockMvc + profile `test` + `TestcontainersConfiguration`, Postgres thật), không dùng H2. Gọi API bằng `MockMvcTester`; helper chung ở `controller/ApiTestSupport`; mỗi test dùng email ngẫu nhiên nên không cần dọn DB.
- Mỗi ràng buộc R1–R9 nên có ít nhất 1 test.

## Git
- Mỗi milestone / tính năng một branch (`feat/auth-jwt`, `feat/deck-card-crud`…), commit theo **Conventional Commits** (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`).
- Commit **local**; **hỏi trước khi push** hoặc tạo PR.
- Trước khi sửa: `git status`; có thay đổi chưa commit thì commit snapshot hoặc tạo branch trước.

## Roadmap và cách làm việc
| Milestone | Nội dung | Màn hình liên quan |
|---|---|---|
| M0 ✅ | Setup Maven, Docker, Flyway, CI | — |
| Design ✅ | Tài liệu UX/UI + prototype | D1–D22 |
| M1 ✅ | User, đăng ký/đăng nhập JWT, phân quyền | D2, D3, D6, D16 |
| M2 ✅ | CRUD deck & card, quyền sở hữu | D8–D11 |
| M3 | **SM-2**, thẻ đến hạn, chấm điểm, ReviewLog | D7, D13, D14 |
| Lộ trình ✅ | Lộ trình CEFR A1–C2: chặng, bài (deck OFFICIAL), mở khóa 100% hoặc bài vượt chặng (§6 business-spec) | D23–D26 |
| M4 | Deck public/official, subscribe, import CSV, admin | D4, D5, D12, D17–D21 |
| M5 | Thống kê, streak, Swagger | D15 |

- **M3 là chế độ gia sư:** người dùng **tự code** `Sm2Algorithm` và test của nó. Claude chỉ hỏi gợi mở và gợi ý **từng bước**, không đưa lời giải trừ khi được yêu cầu rõ. Các phần khác Claude có thể code thẳng.
- Endpoint mới phải khớp bảng "Ánh xạ màn hình ↔ API" trong [03-information-architecture.md](docs/design/03-information-architecture.md); lệch thì cập nhật tài liệu cùng commit.
