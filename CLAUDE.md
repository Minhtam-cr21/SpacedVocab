# SpacedVocab — hướng dẫn cho Claude

REST API học từ vựng tiếng Anh bằng flashcard, lên lịch ôn bằng thuật toán **SM-2**. Dự án luyện tập để đưa lên GitHub làm portfolio.

- **Nghiệp vụ (nguồn sự thật):** [docs/domain/business-spec.md](docs/domain/business-spec.md) — actors, use case, entity, ràng buộc **R1–R9**, công thức SM-2.
- **Thiết kế UX/UI:** [docs/design/](docs/design/README.md) — màn **D1–D22**, ma trận quyền, ánh xạ màn hình ↔ API, UX copy.
- Đổi nghiệp vụ → sửa `business-spec.md` trước, rồi mới sửa code và tài liệu design.

## Stack
- Java 21, **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Actuator), Maven Wrapper.
- PostgreSQL 17 + **Flyway**. Test: JUnit 5, Testcontainers.
- Chạy bằng **Docker Compose**. CI: GitHub Actions (`.github/workflows/ci.yml`).
- Frontend (sau này): **React + Vite**, giao diện **tiếng Việt**, mobile-first, dùng lại `docs/design/prototype/assets/tokens.css`.

## Lệnh thường dùng
```bash
docker compose up -d --build          # Postgres + API (API ở http://localhost:8090)
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

## Cấu trúc code
Package gốc `com.spacedvocab`, chia theo layer:
```
controller  service  repository  entity  dto  config  exception  algorithm
```
- `dto`: Java `record`, **tách khỏi entity**; validate bằng `@Valid` + Bean Validation.
- `exception`: `AppException` (abstract) → `ResourceNotFoundException`, `DuplicateResourceException`, `ForbiddenOperationException`, `BusinessRuleViolationException`. Một `@RestControllerAdvice` trả `{ code, message, field? }`; danh sách `code` ở [docs/design/06-ux-copy.md](docs/design/06-ux-copy.md).
- `algorithm`: `SpacedRepetitionAlgorithm` (interface, Strategy) → `Sm2Algorithm`.

## Quy tắc OOP / thiết kế
- **Constructor injection**, không dùng `@Autowired` trên field. Log bằng **SLF4J**.
- **Rich domain model:** `CardProgress` không có setter cho EF/interval/repetitions; chỉ thay đổi qua `applyReview(quality, algorithm, clock)`.
- **Value object** bằng `record` tự kiểm tra trong constructor: `Quality` (0..5), `EasinessFactor` (≥ 1.3).
- **Tách `Card` (nội dung dùng chung) khỏi `CardProgress` (tiến độ từng user)** — không gộp lại.
- `ReviewLog` bất biến, không bao giờ xóa (R8).
- Inject `java.time.Clock` để test được logic ngày giờ; "ngày học" tính theo `timezone` của user (R7, R9).
- Kiểm tra quyền sở hữu qua interface `Ownable#isOwnedBy(userId)`. Deck PRIVATE của người khác trả **404**, không trả 403.

## Database
- Schema do **Flyway** quản lý, Hibernate chỉ `ddl-auto: validate`.
- **Không bao giờ sửa migration đã chạy** — luôn thêm file mới `V{n}__mo_ta.sql` trong `src/main/resources/db/migration`.
- Thời gian lưu **UTC** (`TIMESTAMPTZ`).

## Test
- Unit test SM-2 bằng `@ParameterizedTest` với các bộ (trạng thái, quality) → kết quả mong đợi theo `business-spec.md` §5.
- Integration test dùng `@Import(TestcontainersConfiguration.class)` (Postgres thật), không dùng H2.
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
| M1 | User, đăng ký/đăng nhập JWT, phân quyền | D2, D3, D6, D16 |
| M2 | CRUD deck & card, quyền sở hữu | D8–D11 |
| M3 | **SM-2**, thẻ đến hạn, chấm điểm, ReviewLog | D7, D13, D14 |
| M4 | Deck public/official, subscribe, import CSV, admin | D4, D5, D12, D17–D21 |
| M5 | Thống kê, streak, Swagger | D15 |

- **M3 là chế độ gia sư:** người dùng **tự code** `Sm2Algorithm` và test của nó. Claude chỉ hỏi gợi mở và gợi ý **từng bước**, không đưa lời giải trừ khi được yêu cầu rõ. Các phần khác Claude có thể code thẳng.
- Endpoint mới phải khớp bảng "Ánh xạ màn hình ↔ API" trong [03-information-architecture.md](docs/design/03-information-architecture.md); lệch thì cập nhật tài liệu cùng commit.
