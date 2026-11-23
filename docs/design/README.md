# Thiết kế UX/UI — SpacedVocab

Bộ tài liệu thiết kế làm "hợp đồng" cho các milestone M1–M5 và cho frontend **React + Vite** sau này.
Giao diện **tiếng Việt**, **responsive, mobile-first**. Nghiệp vụ gốc: [`../domain/business-spec.md`](../domain/business-spec.md).

## Mục lục

| File | Nội dung |
|---|---|
| [01-ux-research.md](01-ux-research.md) | 3 persona, hành trình người học, 7 nguyên tắc UX |
| [02-user-flows.md](02-user-flows.md) | Luồng theo role (Guest, Learner, Admin) — Mermaid |
| [03-information-architecture.md](03-information-architecture.md) | Sitemap, route React, ma trận quyền, ánh xạ màn hình ↔ API |
| [04-design-system.md](04-design-system.md) | Màu, chữ, spacing, component, accessibility |
| [05-screen-specs.md](05-screen-specs.md) | Đặc tả 22 màn hình D1–D22 |
| [06-ux-copy.md](06-ux-copy.md) | Thuật ngữ, thông báo lỗi ↔ exception backend, empty state, toast |
| [prototype/](prototype/index.html) | Prototype HTML/CSS bấm thử được |

## Mở prototype

Cần Python 3 (hoặc bất kỳ static server nào):

```bash
python -m http.server 5500 --directory docs/design/prototype
```

Rồi mở <http://localhost:5500>. Cũng có thể mở trực tiếp file `prototype/index.html` bằng trình duyệt.

- Góc dưới phải mỗi màn: **☰ Mục lục** và **◐ Giao diện** (đổi sáng/tối).
- Nhiều màn có ô **"Trạng thái demo"** để xem các trạng thái empty / loading / lỗi / vai trò khác nhau.
- Màn **D13 Phiên ôn tập**: `Space` lật thẻ, `1–4` chấm điểm; khoảng thời gian dưới mỗi nút được tính bằng SM-2 thật (`assets/app.js`).

## Quy ước

- Mã màn **D1–D22** dùng thống nhất trong mọi tài liệu, issue và commit liên quan đến UI.
- Đổi nghiệp vụ → sửa `business-spec.md` trước, rồi cập nhật tài liệu design và prototype tương ứng.
- Prototype chỉ là tham chiếu hành vi và giao diện; app React viết lại component theo [04-design-system.md](04-design-system.md), **dùng lại nguyên `tokens.css`**.
