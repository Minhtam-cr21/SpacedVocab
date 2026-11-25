# 05 · Đặc tả màn hình

> Mỗi màn: **Mục đích · Role · Thành phần · Trạng thái · API**. Prototype ở `prototype/screens/<file>.html`.
> Trạng thái chung cho mọi màn có gọi API: **loading** → `Skeleton`; **lỗi mạng/500** → `Alert` + nút "Thử lại"; **401** → về D3 kèm thông báo hết phiên; **403** → D22; **404** → D22.

## Guest

### D1 · Landing — `landing.html` — `/`
- **Mục đích:** giới thiệu giá trị, dẫn tới đăng ký hoặc khám phá.
- **Role:** tất cả (đã đăng nhập → chuyển `/app`).
- **Thành phần:** Hero + 2 CTA, 3 bước "Học như thế nào", 3 `DeckCard` phổ biến.
- **API:** `GET /api/public/decks?sort=popular&size=3`.

### D2 · Đăng ký — `register.html` — `/register`
- **Thành phần:** tên hiển thị, email*, mật khẩu*, nhập lại*. Validate phía client trước khi gửi.
- **Trạng thái:** lỗi từng ô; email trùng (`409 Duplicate`) → lỗi tại ô email + link Đăng nhập; nút disabled khi đang gửi.
- **Thành công:** tự đăng nhập → D6.
- **API:** `POST /api/auth/register`. Quy tắc: **R1**.

### D3 · Đăng nhập — `login.html` — `/login`
- **Trạng thái:** sai thông tin → `Alert` chung (không nói rõ sai email hay mật khẩu, tránh dò tài khoản); tài khoản `LOCKED` → `Alert` cảnh báo riêng.
- **Thành công:** LEARNER → D7 (lần đầu → D6); ADMIN → D18.
- **API:** `POST /api/auth/login`. Quy tắc: **R1**.

### D4 · Khám phá — `explore.html` — `/explore`
- **Thành phần:** `SearchInput`, lọc loại (chính thức / cộng đồng), sắp xếp, lưới `DeckCard`, phân trang.
- **Trạng thái:** loading (skeleton 3 thẻ), empty ("Không tìm thấy" + CTA), có dữ liệu.
- **API:** `GET /api/public/decks?q=&type=&sort=&page=`. Deck `hidden` không xuất hiện.

### D5 · Xem trước deck — `public-deck.html` — `/explore/decks/:id`
- **Thành phần:** thông tin deck, khối CTA thay đổi theo người xem, danh sách 10 thẻ đầu.
- **CTA theo người xem:**
  | Người xem | CTA |
  |---|---|
  | Guest | "Đăng ký để học" → D2 |
  | Learner chưa đăng ký | "+ Thêm vào thư viện", "🚩 Báo cáo" |
  | Learner đã đăng ký | "Bắt đầu học", "Bỏ khỏi thư viện" |
  | Chủ deck | "Deck của bạn" — không có nút subscribe (**R4**) |
- **API:** `GET /api/public/decks/{id}`, `POST/DELETE /api/decks/{id}/subscription`.

## Learner

### D6 · Onboarding — `onboarding.html` — `/app/onboarding`
- **Thành phần:** `Stepper` 2 bước; `ChoiceGroup` 10/20/40 thẻ mới; múi giờ (tự đoán từ trình duyệt); chọn deck OFFICIAL.
- **API:** `PUT /api/me/settings`, `POST /api/decks/{id}/subscription` cho mỗi deck chọn.

### D7 · Dashboard — `dashboard.html` — `/app`
- **Mục đích:** trả lời ngay "hôm nay học bao nhiêu" và bắt đầu ôn trong ≤ 2 chạm.
- **Thành phần:** khối "Hôm nay" (số thẻ đến hạn, tách ôn lại/mới, thời gian ước tính, `ProgressRing` thẻ mới), nút **Bắt đầu ôn**, 3 `StatTile`, danh sách deck có thẻ đến hạn.
- **Trạng thái:** có thẻ · đã xong ("Hôm nay xong rồi!") · người dùng mới (thư viện trống).
- **API:** `GET /api/reviews/due?limit=0` (chỉ lấy số đếm; `nextDueAt` cho trạng thái "đã xong"), `GET /api/stats/overview`, `GET /api/decks?scope=all&withDue=true`.

### D8 · Thư viện — `library.html` — `/app/library`
- **Thành phần:** `Tabs` "Deck của tôi" / "Đã đăng ký"; lưới `DeckCard`; thẻ "+ Tạo deck mới".
- **Trạng thái:** empty từng tab (CTA tạo deck / khám phá).
- **API:** `GET /api/decks?scope=owned|subscribed`.

### D9 · Tạo / sửa deck — `deck-form.html` — `/app/decks/new`, `/:id/edit`
- **Thành phần:** tên* (≤ 100), mô tả (≤ 500), visibility PRIVATE/PUBLIC; **vùng nguy hiểm** (xóa) khi sửa.
- **Trạng thái:** tên trùng (**R2**) → lỗi tại ô; xóa → `ConfirmDialog` nêu số thẻ và số người đang học (**R8**).
- **Admin dùng lại** với visibility cố định `OFFICIAL` (**R3**).
- **API:** `POST /api/decks`, `PUT /api/decks/{id}`, `DELETE /api/decks/{id}`.

### D10 · Chi tiết deck — `deck-detail.html` — `/app/decks/:id`
- **Thành phần:** header + nút ôn deck này; 4 `StatTile` (tổng, NEW, LEARNING, REVIEW); tìm kiếm; `DataTable` thẻ (mobile: `List`); phân trang.
- **Theo người xem:** chủ → nút Sửa, Thêm thẻ, Import, Sửa từng thẻ. Subscriber → `Alert` chỉ đọc, nút 🚩 báo cáo, "Bỏ khỏi thư viện".
- **API:** `GET /api/decks/{id}`, `GET /api/decks/{id}/cards?q=&page=&size=20`. Deck PRIVATE của người khác → **404** (không lộ sự tồn tại). Hiện nút chủ deck theo `editable` trong response. 4 `StatTile` lấy từ `progress` trong response của `GET /api/decks/{id}` (tổng = `cardCount`).

### D11 · Thêm / sửa thẻ — `card-form.html` — `/app/decks/:id/cards/new`
- **Thành phần:** word* (font nội dung, `lang="en"`), từ loại, nghĩa*, IPA, ví dụ, ghi chú, xem trước thẻ; "Lưu & thêm tiếp" (giữ focus ở ô word) và "Lưu".
- **Trạng thái:** trùng từ (**R2**, không phân biệt hoa thường) → lỗi + link tới thẻ cũ.
- **API:** `POST /api/decks/{id}/cards`, `GET/PUT/DELETE /api/decks/{id}/cards/{cardId}` (màn sửa tải thẻ bằng `GET`). Link tới thẻ cũ khi trùng: tìm bằng `GET .../cards?q={word}`.

### D12 · Import CSV — `import.html` — `/app/decks/:id/import`
- **Bước 1:** `FileDropzone` (≤ 2MB, ≤ 1.000 dòng, UTF-8), mô tả định dạng, file mẫu.
- **Bước 2:** gọi `dryRun=true`, hiện 3 `StatTile` (hợp lệ / trùng / lỗi) + bảng dòng có vấn đề. Không ghi gì vào DB.
- **Bước 3:** gọi `dryRun=false`, hiện kết quả + link.
- **API:** `POST /api/decks/{id}/cards/import?dryRun=true|false` (multipart). Quy tắc: **R2**.

### D13 · Phiên ôn tập ⭐ — `review.html` — `/app/review?deckId=`
- **Mục đích:** ôn nhanh, tập trung, dùng một tay hoặc chỉ bàn phím.
- **Layout:** ẩn mọi điều hướng; trên cùng: ✕ thoát, `ProgressBar`, đếm `n/N`.
- **Luồng:** mặt trước (word + badge trạng thái) → lật (chạm / `Space`) → mặt sau (word, IPA, từ loại, nghĩa, ví dụ) → `RatingBar` 4 nút, mỗi nút hiện **khoảng thời gian dự kiến** → thẻ tiếp theo.
- **Quy tắc phiên:** bấm **Quên** → thẻ quay lại cuối hàng đợi trong phiên; hết hàng đợi → D14.
- **Trạng thái:** không có thẻ → empty "Hôm nay xong rồi!"; gửi điểm lỗi mạng → `Toast` "Chưa lưu được, thử lại", **giữ nguyên thẻ**, không mất điểm đã chấm.
- **A11y:** focus sẵn nút "Nhớ" sau khi lật; `aria-live` đọc nghĩa; tắt hiệu ứng lật khi giảm chuyển động.
- **API:** `GET /api/reviews/due?deckId=&limit=50` (kèm `previewIntervals`), `POST /api/reviews {cardId, quality}`. Quy tắc: **R5, R6, R7**. Chấm lại thẻ đã bấm "Quên" trong phiên thì gửi `POST` thêm lần nữa (mỗi lần là 1 `ReviewLog`); thẻ mới thứ `dailyNewLimit + 1` trong ngày → 422 `DAILY_NEW_LIMIT_REACHED`.

### D14 · Tổng kết phiên — `review-summary.html` — `/app/review/summary`
- **Thành phần:** lời khen, streak mới, `ProgressRing` tỷ lệ nhớ, 3 `StatTile` (đã ôn, thẻ mới, thời gian), danh sách từ bấm "Quên".
- **API:** dữ liệu từ phiên ở client + `GET /api/stats/overview` để lấy streak mới.

### D15 · Thống kê — `stats.html` — `/app/stats`
- **Thành phần:** chọn khoảng thời gian; 4 `StatTile`; `Heatmap` 20 tuần; phân bố NEW/LEARNING/REVIEW; dự báo 7 ngày.
- **Trạng thái:** người mới → empty "Ôn vài thẻ để xem thống kê".
- **API:** `GET /api/stats/overview?range=`, `GET /api/stats/heatmap?from=&to=`, `GET /api/stats/forecast?days=7`.

### D16 · Hồ sơ & cài đặt — `settings.html` — `/app/settings`
- **Thành phần:** 3 form riêng: hồ sơ, học tập (`dailyNewLimit` 0–200, múi giờ), đổi mật khẩu; chọn giao diện; đăng xuất.
- **API:** `GET/PUT /api/me`, `PUT /api/me/settings`, `PUT /api/me/password`, `POST /api/auth/logout`.

### D17 · Modal báo cáo deck — trong `public-deck.html`, `deck-detail.html`
- **Thành phần:** `Modal` với lý do* (4 lựa chọn) + mô tả (≤ 500).
- **Ẩn khi:** người xem là chủ deck hoặc Guest.
- **API:** `POST /api/decks/{id}/reports`.

### D23 · Lộ trình CEFR — `/app/path`
- **Mục đích:** cho người học thấy mình đang ở chặng nào và việc cần làm tiếp theo.
- **Thành phần:** tiêu đề "Chọn chặng học"; danh sách 6 `StageCard` A1 → C2. Mỗi thẻ có ô nhãn CEFR màu riêng, tên chặng, "n/N bài đã hoàn thành", `ProgressBar`, nút bên phải.
- **Trạng thái `StageCard`:** **đang học** (nổi bật, nút →) · **đã xong** (dấu ✓, ghi "Vượt bằng bài kiểm tra" nếu `unlockMethod = TEST` của chặng sau) · **khóa** (mờ, icon ổ khóa, chạm vào → D24 ở chế độ chỉ xem).
- **Bit:** đứng cạnh chặng đang học, nói tiếng Anh ("Let's go, A1!"). Vừa mở chặng mới → "Level up! B1 unlocked!".
- **A11y:** chặng khóa vẫn focus được; `aria-label` "Chặng B1, đang khóa, 0 trên 21 bài".
- **API:** `GET /api/paths/{id}`. Quy tắc: **R10, R11**.

### D24 · Chi tiết chặng — `/app/path/stages/:stageId`
- **Thành phần:** đầu trang có nhãn CEFR + tiến độ chặng; danh sách `LessonRow` (tên bài, "đã học x/y từ", dấu ✓ khi xong, nút "Bắt đầu" / "Học tiếp" / "Ôn lại").
- **Khối "Nhảy chặng"** (chỉ hiện khi `testAvailable`): giải thích "20 câu, gõ từ tiếng Anh theo nghĩa, 10 phút, đúng 18 câu là qua" + nút **"Làm bài vượt chặng"** → `ConfirmDialog` → D25. Đang chờ 24 giờ thì nút disabled và ghi "Làm lại sau {giờ}".
- **Chặng khóa:** danh sách bài chỉ xem, nút "Bắt đầu" bị ẩn; hiện dòng "Hoàn thành chặng {trước} hoặc làm bài vượt chặng để mở".
- **API:** `GET /api/stages/{id}`, `POST /api/lessons/{id}/start` → chuyển D13 `?deckId=`. Quy tắc: **R10, R11, R12**.

### D25 · Bài vượt chặng — `/app/path/stages/:stageId/test`
- **Layout:** giống D13, ẩn điều hướng; trên cùng: ✕ thoát (`ConfirmDialog` "Đồng hồ vẫn chạy khi bạn thoát. Hết giờ mà chưa nộp thì tính là trượt."), đồng hồ đếm ngược, đếm `n/20`.
- **Mỗi câu:** nghĩa tiếng Việt cỡ lớn + từ loại; một ô nhập (tắt tự sửa chính tả, tự viết hoa); nút "Tiếp" và "Bỏ qua". Được quay lại sửa câu trước khi nộp.
- **Nộp:** nút "Nộp bài" ở câu 20, hoặc tự nộp khi hết giờ.
- **Kết quả:** `ProgressRing` x/20. **Qua** → Bit ăn mừng, nút "Tới chặng {sau}". **Trượt** → lời động viên, giờ được làm lại, danh sách câu sai (nghĩa, bạn gõ, đáp án đúng), nút "Học tiếp chặng này".
- **Trạng thái:** mất mạng khi nộp → giữ câu trả lời ở client, `Toast` "Chưa nộp được, thử lại".
- **API:** `POST /api/stages/{id}/test-attempts`, `POST /api/stage-test-attempts/{id}/submit`. Quy tắc: **R12**.

## Admin

### D18 · Tổng quan — `admin-dashboard.html` — `/admin`
- **Thành phần:** 4 `StatTile` hệ thống; "Cần xử lý" (report đang chờ, deck chính thức); top deck nhiều người học.
- **API:** `GET /api/admin/stats`.

### D19 · Người dùng — `admin-users.html` — `/admin/users`
- **Thành phần:** tìm kiếm, lọc trạng thái/vai trò, `DataTable`, phân trang; nút Khóa / Mở khóa mỗi dòng.
- **Quy tắc:** luôn qua `ConfirmDialog` nêu hậu quả; **không tự khóa chính mình** (nút disabled + tooltip).
- **API:** `GET /api/admin/users?q=&status=&role=&page=`, `PATCH /api/admin/users/{id}/status {status}`.

### D20 · Deck chính thức — `admin-decks.html` — `/admin/decks`
- **Thành phần:** bảng deck OFFICIAL (số thẻ, người học, cập nhật, trạng thái); thao tác Thẻ / Import / Sửa dùng lại D9–D12.
- **API:** `GET /api/admin/decks`, `POST/PUT/DELETE /api/admin/decks/{id}`.

### D21 · Hàng đợi report — `admin-reports.html` — `/admin/reports`
- **Thành phần:** `Tabs` Đang chờ / Đã xử lý; desktop chia 2 cột (danh sách + chi tiết), mobile xếp chồng.
- **Hành động:** **Bỏ qua** (report → DISMISSED) · **Ẩn deck** (`ConfirmDialog`; deck `hidden = true`, mọi report của deck → RESOLVED).
- **API:** `GET /api/admin/reports?status=OPEN`, `PATCH /api/admin/reports/{id} {action}`.

### D26 · Lộ trình — `/admin/path`
- **Thành phần:** 6 chặng cố định A1–C2 (chỉ sửa tên); trong mỗi chặng là bảng bài (deck, số thẻ, thứ tự) với nút Thêm bài (chọn deck `OFFICIAL` chưa thuộc bài nào), Lên/Xuống, Gỡ (`ConfirmDialog`).
- **Cảnh báo:** chặng có < 20 thẻ thì hiện "Chưa đủ thẻ cho bài vượt chặng".
- **API:** xem bảng ánh xạ D26 ở [03](03-information-architecture.md). Quy tắc: **R10**.

## Chung

### D22 · Lỗi — `error.html` — `/403`, `*`
- **Biến thể:** 404 không tìm thấy · 403 không có quyền · lỗi mạng/máy chủ (nút Thử lại) · hết phiên đăng nhập (→ D3).
- Mọi biến thể đều có **một hành động** để đi tiếp.
