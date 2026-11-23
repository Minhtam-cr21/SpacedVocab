# 04 · Design system

> Code tham chiếu: [`prototype/assets/tokens.css`](prototype/assets/tokens.css), [`components.css`](prototype/assets/components.css).
> Xem trực quan: [`prototype/screens/style-guide.html`](prototype/screens/style-guide.html).
> Khi làm React + Vite: copy `tokens.css` sang `src/styles/tokens.css`, mỗi nhóm class dưới đây thành 1 component.

## 1. Màu sắc

| Token | Light | Dark | Dùng cho |
|---|---|---|---|
| `--bg` | `#f6f6fb` | `#0e0f1d` | Nền trang |
| `--surface` / `--surface-2` | `#ffffff` / `#f0f0f7` | `#17182b` / `#202239` | Card, bảng / nền phụ |
| `--border` | `#e0e0ea` | `#2f3150` | Viền |
| `--text` / `--text-muted` | `#1b1b29` / `#5b5c72` | `#ececf5` / `#a5a6c0` | Chữ chính / phụ |
| `--primary` | `#4f46e5` | `#8f88ff` | Hành động chính, link, trạng thái đang chọn |
| `--success` · `--warning` · `--danger` · `--info` | `#15803d` · `#b45309` · `#dc2626` · `#0369a1` | sáng hơn tương ứng | Trạng thái |

- **Độ tương phản (WCAG AA ≥ 4.5:1 cho chữ thường)**: `--text` 16:1, `--text-muted` 6.6:1, `--primary` 6.3:1 trên nền trắng. Màu trạng thái đều ≥ 4.5:1 trên trắng.
- **Dark mode**: tự theo `prefers-color-scheme`, có thể ép bằng `data-theme="light|dark"` trên `<html>`.

### Màu nút chấm điểm (RatingBar)

| Nút | `quality` | Phím | Token | Ý nghĩa màu |
|---|---|---|---|---|
| **Quên** | 1 | `1` | `--rate-again` (đỏ) | Cần ôn lại ngay |
| **Khó** | 3 | `2` | `--rate-hard` (cam) | Nhớ nhưng chật vật |
| **Nhớ** | 4 | `3` | `--rate-good` (xanh lá) | Lựa chọn mặc định, được focus sẵn |
| **Dễ** | 5 | `4` | `--rate-easy` (xanh dương) | Nhớ ngay |

Mỗi nút hiển thị **khoảng thời gian tới lần ôn sau** (ví dụ "6 ngày"), do backend tính và trả về trong `previewIntervals`. Không dùng **chỉ màu** để phân biệt: luôn có chữ + số phím.

### Trạng thái thẻ
`NEW` xanh dương · `LEARNING` cam · `REVIEW` xanh lá — dùng `Badge` (`.badge-new`, `.badge-learning`, `.badge-review`).

## 2. Typography

| Token | Giá trị | Dùng cho |
|---|---|---|
| `--font-ui` | **Be Vietnam Pro** | Toàn bộ giao diện tiếng Việt (hiển thị dấu tốt) |
| `--font-content` | **Source Serif 4** | Nội dung tiếng Anh trên thẻ: từ, IPA, câu ví dụ |
| `--text-xs … --text-3xl` | 12 · 14 · 16 · 18 · 22 · 28 · 36 px | Thang cỡ chữ; gốc 16px |
| `--text-word` | `clamp(2rem, 8vw, 3rem)` | Từ vựng trên flashcard |

Tách font UI và font nội dung giúp người học **nhận ra ngay đâu là tiếng Anh cần học** (nguyên tắc UX số 7).

## 3. Spacing, bo góc, đổ bóng
- **Spacing** theo thang 4px: `--space-1` (4) … `--space-16` (64).
- **Radius**: `sm 6` · `md 10` (nút, input) · `lg 16` (card) · `xl 24` (flashcard) · `full`.
- **Shadow**: `sm` (card tĩnh) · `md` (hover, flashcard) · `lg` (modal, toast).
- **Breakpoint**: mobile mặc định · `640px` (lưới 2 cột) · `768px` (header đầy đủ) · `1024px` (sidebar thay bottom nav).

## 4. Component (đặt tên theo React)

| Component | Class CSS | Ghi chú |
|---|---|---|
| `AppShell` | `.app-shell.has-sidebar` | Bọc sidebar + topbar + nội dung |
| `Sidebar` | `.sidebar`, `.nav-item` | Desktop ≥ 1024px; `aria-current="page"` cho mục đang chọn |
| `BottomNav` | `.bottom-nav`, `.nav-primary` | Mobile; nút giữa "Ôn tập" nổi bật + badge số thẻ |
| `Topbar` | `.topbar`, `.brand` | Mobile hiện streak 🔥 |
| `Button` | `.btn` + `-primary/-secondary/-ghost/-danger/-danger-outline`, `-sm/-lg/-block/-icon` | Cao tối thiểu 44px |
| `Input`, `Select`, `Textarea` | `.field > .label + .input + .hint/.error-text` | Lỗi: `.field.has-error` |
| `ChoiceGroup` | `.choice-group > .choice` | Radio dạng thẻ (onboarding, visibility) |
| `SearchInput` | `.search > .input` | |
| `Badge` | `.badge` + biến thể màu, `.badge-count` | |
| `Alert` | `.alert-danger/-warning/-info/-success` | Lỗi cấp form/trang |
| `Tabs` | `.tabs > .tab[aria-selected]` | ARIA `role="tablist"` |
| `DeckCard` | `.card.deck-card` | Tên, chủ sở hữu, số thẻ, số thẻ đến hạn |
| `Flashcard` | `.flashcard > .face.front/.back` | Lật 3D, tắt khi `prefers-reduced-motion` |
| `RatingBar` | `.rating-bar > .rate-btn.rate-*` | 4 nút |
| `ProgressBar`, `ProgressRing` | `.progress`, `.ring` | |
| `StatTile` | `.card.stat-tile` | |
| `Heatmap` | `.heatmap > span[data-l=0..4]` | Mỗi ô 1 ngày |
| `DataTable` | `.table-wrap > .table`, `.pagination` | Desktop; mobile dùng `List` |
| `List` | `.list > .list-item` | Danh sách thẻ, user trên mobile |
| `Modal`, `ConfirmDialog` | `dialog.modal` | Dùng `<dialog>` gốc: có sẵn focus trap + `Esc` |
| `Toast` | `.toast-region > .toast` | `aria-live="polite"`, tự tắt sau 4 giây |
| `EmptyState` | `.empty-state` | Luôn có 1 hành động gợi ý |
| `Skeleton` | `.skeleton` | Trạng thái loading |
| `FileDropzone` | `.dropzone` | Kéo thả hoặc chọn file |
| `Stepper` | `.stepper > li[aria-current="step"]` | Import CSV, onboarding |

**Icon:** prototype dùng emoji cho nhanh; app React dùng **lucide-react**, kèm `aria-hidden` khi đi cùng chữ.

## 5. Accessibility

- **Focus ring** 3px `--focus` cho mọi phần tử tương tác (`:focus-visible`).
- **Vùng chạm ≥ 44 × 44px** (`--tap-min`).
- **Phím tắt màn ôn tập**: `Space`/`Enter` lật thẻ · `1–4` chấm điểm · `Esc` thoát. Gợi ý phím chỉ hiện trên thiết bị có chuột (`hover: hover`).
- **Screen reader**: vùng `aria-live` đọc nghĩa của thẻ sau khi lật; nút icon luôn có `aria-label`.
- **Giảm chuyển động**: `prefers-reduced-motion` → tắt hiệu ứng lật, shimmer, transition.
- **Ngôn ngữ**: `<html lang="vi">`; nội dung tiếng Anh trên thẻ nên gắn `lang="en"` để screen reader đọc đúng.
- **Form**: label luôn hiển thị (không chỉ dùng placeholder), lỗi gắn với ô bằng `aria-describedby`.
