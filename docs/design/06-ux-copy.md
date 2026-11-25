# 06 · UX copy (tiếng Việt)

## Giọng văn
- **Thân thiện, ngắn, rõ**. Xưng "bạn". Không dùng thuật ngữ kỹ thuật (không "request", "server error 500").
- **Nội dung thẻ giữ tiếng Anh**; mọi chữ còn lại tiếng Việt.
- Nút bắt đầu bằng **động từ**: "Tạo deck", "Gửi báo cáo", không phải "OK".
- Lỗi = **chuyện gì xảy ra + cách sửa**.
- Viết số theo kiểu Việt Nam: `1.240`, `4,6 giờ`; ngày `dd/MM/yyyy`.

## Thuật ngữ thống nhất

| Dùng | Không dùng | Ghi chú |
|---|---|---|
| deck / bộ thẻ | bộ sưu tập, collection | "deck" trong nút và nhãn ngắn |
| thẻ | card, flashcard | |
| ôn / ôn tập | review, học lại | |
| thẻ đến hạn | thẻ due | |
| thêm vào thư viện | subscribe, theo dõi | Hành động subscribe |
| chuỗi ngày học | streak | Có thể kèm 🔥 |
| Quên · Khó · Nhớ · Dễ | Again · Hard · Good · Easy | 4 nút chấm điểm |

## Thông báo lỗi ↔ lỗi backend

Backend trả lỗi dạng `{ code, message, field?, errors? }` từ `@RestControllerAdvice` (`errors` = danh sách `{field, message}`, chỉ có với `VALIDATION_FAILED`). Frontend map theo `code`. Các `code` nằm trong enum `ErrorCode` của backend.

| Exception backend | HTTP | `code` | Câu hiển thị |
|---|---|---|---|
| `DuplicateResourceException` (email) | 409 | `EMAIL_TAKEN` | Email này đã được đăng ký. Bạn muốn **đăng nhập**? |
| `DuplicateResourceException` (deck) | 409 | `DECK_NAME_TAKEN` | Bạn đã có deck tên "{name}". Hãy đặt tên khác. |
| `DuplicateResourceException` (card) | 409 | `CARD_WORD_TAKEN` | "{word}" đã có trong deck này. |
| `DuplicateResourceException` (chấm cùng một thẻ mới hai lần cùng lúc) | 409 | `REVIEW_CONFLICT` | Thẻ này vừa được chấm ở nơi khác. Hãy tải lại phiên ôn. |
| `UnauthorizedException` (sai đăng nhập) | 401 | `BAD_CREDENTIALS` | Email hoặc mật khẩu không đúng. Vui lòng thử lại. |
| Chưa đăng nhập / token không hợp lệ | 401 | `UNAUTHORIZED` | Vui lòng đăng nhập để tiếp tục. |
| Access token hết hạn → gọi `/api/auth/refresh`; refresh token hết hạn / bị thu hồi | 401 | `TOKEN_EXPIRED` | Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại. |
| `ForbiddenOperationException` (tài khoản bị khóa) | 403 | `ACCOUNT_LOCKED` | Tài khoản của bạn đang bị tạm khóa. Liên hệ quản trị viên để được hỗ trợ. |
| `ForbiddenOperationException` (sửa deck công khai của người khác, chọn `OFFICIAL` khi không phải Admin) | 403 | `FORBIDDEN` | Bạn không có quyền thực hiện thao tác này. |
| `ResourceNotFoundException` | 404 | `NOT_FOUND` | Không tìm thấy nội dung này, có thể nó đã bị xóa. |
| `BusinessRuleViolationException` (đổi mật khẩu) | 422 | `WRONG_CURRENT_PASSWORD` | Mật khẩu hiện tại không đúng. (hiện tại ô `currentPassword`) |
| Sai HTTP method | 405 | `METHOD_NOT_ALLOWED` | Thao tác này không được hỗ trợ. |
| Lỗi không lường trước | 500 | `INTERNAL_ERROR` | Đã có lỗi xảy ra. Vui lòng thử lại sau. |
| `BusinessRuleViolationException` (R4) | 422 | `CANNOT_SUBSCRIBE_OWN_DECK` | Đây là deck của bạn nên không cần thêm vào thư viện. |
| `BusinessRuleViolationException` (R4) | 422 | `DECK_NOT_PUBLIC` | Deck này không còn công khai. |
| `BusinessRuleViolationException` (R6) | 422 | `CARD_NOT_IN_LIBRARY` | Thẻ này không thuộc thư viện của bạn. |
| `BusinessRuleViolationException` (R7) | 422 | `DAILY_NEW_LIMIT_REACHED` | Bạn đã học đủ số thẻ mới hôm nay. Hẹn bạn ngày mai nhé! |
| `BusinessRuleViolationException` (tự khóa) | 422 | `CANNOT_LOCK_SELF` | Bạn không thể tự khóa tài khoản của mình. |
| `BusinessRuleViolationException` (R11) | 422 | `STAGE_LOCKED` | Chặng này chưa mở. Hoàn thành chặng trước hoặc làm bài vượt chặng nhé! |
| `BusinessRuleViolationException` (R12) | 422 | `STAGE_TEST_NOT_AVAILABLE` | Chặng này không cần làm bài vượt chặng nữa. |
| `BusinessRuleViolationException` (R12) | 422 | `STAGE_TEST_TOO_FEW_CARDS` | Chặng này chưa đủ từ để làm bài vượt chặng. |
| `BusinessRuleViolationException` (R12) | 422 | `STAGE_TEST_COOLDOWN` | Nghỉ một chút rồi thử lại nhé! Bạn làm lại được lúc {retryAt}. |
| `DuplicateResourceException` (R12) | 409 | `STAGE_TEST_ALREADY_SUBMITTED` | Bài này đã nộp rồi. |
| Validation (`@Valid`) | 400 | `VALIDATION_FAILED` | Hiện lỗi theo từng ô (xem bảng dưới). |
| Lỗi mạng / 5xx | — | — | Không kết nối được tới máy chủ. Kiểm tra mạng rồi thử lại. |

### Lỗi validate theo ô

| Ô | Điều kiện | Câu |
|---|---|---|
| Email | sai định dạng | Email chưa đúng định dạng. |
| Mật khẩu | < 8 ký tự | Mật khẩu cần ít nhất 8 ký tự. |
| Nhập lại mật khẩu | không khớp | Mật khẩu nhập lại chưa khớp. |
| Tên deck | trống / > 100 | Hãy nhập tên deck. / Tên deck tối đa 100 ký tự. |
| Word, Nghĩa | trống | Hãy nhập từ. / Hãy nhập nghĩa. |
| Số thẻ mới/ngày | ngoài 0–200 | Hãy nhập số từ 0 đến 200. |
| File CSV | sai loại / quá lớn | Chỉ hỗ trợ file .csv. / File tối đa 2MB. |

## Empty state

| Nơi | Tiêu đề | Mô tả | Hành động |
|---|---|---|---|
| D7 xong bài | Hôm nay xong rồi! 🎉 | Thẻ tiếp theo đến hạn vào {time}. | Khám phá deck mới |
| D7 người mới | Thư viện còn trống | Thêm một bộ thẻ có sẵn hoặc tự tạo để bắt đầu học. | Khám phá bộ thẻ · Tạo deck |
| D8 tab Deck của tôi | Bạn chưa tạo deck nào | Tự tạo hoặc import từ CSV. | + Tạo deck |
| D8 tab Đã đăng ký | Chưa có deck nào trong thư viện | Thêm deck chính thức để học ngay. | Khám phá bộ thẻ |
| D10 deck trống | Deck chưa có thẻ | Thêm thẻ đầu tiên hoặc import cả danh sách. | + Thêm thẻ · Import CSV |
| D4 không kết quả | Không tìm thấy bộ thẻ nào | Thử từ khóa khác. | Xóa bộ lọc |
| D15 người mới | Chưa có dữ liệu | Ôn vài thẻ để xem thống kê của bạn. | Bắt đầu ôn |
| D21 hết report | Không còn report nào 👏 | Mọi report đã được xử lý. | — |

## Toast

| Sự kiện | Nội dung | Hành động |
|---|---|---|
| Lưu thẻ | Đã lưu thẻ "{word}". | Hoàn tác |
| Tạo deck | Đã tạo deck. | — |
| Xóa deck | Đã xóa deck. | — |
| Thêm vào thư viện | Đã thêm vào thư viện. {n} thẻ mới sẵn sàng hôm nay. | Học ngay |
| Bỏ khỏi thư viện | Đã bỏ deck khỏi thư viện. Tiến độ vẫn được giữ. | Hoàn tác |
| Gửi report | Cảm ơn bạn! Admin sẽ xem xét báo cáo. | — |
| Chấm điểm lỗi mạng | Chưa lưu được, thử lại. | Thử lại |
| Khóa / mở khóa user | Đã khóa tài khoản. / Đã mở khóa tài khoản. | — |
| Ẩn deck | Đã ẩn deck và đóng {n} report. | — |

## Xác nhận hành động nguy hiểm

| Hành động | Tiêu đề | Nội dung | Nút |
|---|---|---|---|
| Xóa deck | Xóa deck "{name}"? | {n} thẻ sẽ bị xóa. {m} người đang học deck này — họ sẽ không thấy deck nữa, nhưng lịch sử ôn vẫn được giữ. | Hủy · **Xóa deck** |
| Khóa user | Khóa tài khoản này? | Người dùng sẽ bị đăng xuất và không thể đăng nhập cho tới khi được mở khóa. Dữ liệu học tập vẫn được giữ nguyên. | Hủy · **Khóa tài khoản** |
| Ẩn deck | Ẩn deck này? | Deck sẽ biến mất khỏi Khám phá và thư viện của người đã đăng ký. Có thể bỏ ẩn sau. | Hủy · **Ẩn deck** |
