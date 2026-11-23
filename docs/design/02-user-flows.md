# 02 · Luồng người dùng (User Flows)

> Sơ đồ viết bằng Mermaid — GitHub tự render. Mã màn hình (D1, D7…) khớp với [05-screen-specs.md](05-screen-specs.md).

## Guest

### F-G1 · Khám phá → đăng ký / đăng nhập
```mermaid
flowchart TD
    A[D1 Landing] --> B[D4 Khám phá deck]
    B --> C[D5 Xem trước deck]
    C -->|Bấm 'Đăng ký để học'| D[D2 Đăng ký]
    A -->|Bắt đầu miễn phí| D
    A -->|Đăng nhập| E[D3 Đăng nhập]
    D --> D1{Hợp lệ?}
    D1 -->|Email đã tồn tại - R1| D2[Báo lỗi tại ô email + link 'Đăng nhập']
    D2 --> E
    D1 -->|Mật khẩu dưới 8 ký tự| D
    D1 -->|OK| F[D6 Onboarding]
    E --> E1{Kết quả}
    E1 -->|Sai email/mật khẩu| E
    E1 -->|Tài khoản LOCKED - R1| E2[Thông báo bị khóa, không cho vào]
    E1 -->|LEARNER| G[D7 Dashboard]
    E1 -->|ADMIN| H[D18 Admin dashboard]
```

## Learner

### F-L1 · Onboarding lần đầu
```mermaid
flowchart LR
    A[Đăng ký xong] --> B[D6 Bước 1: chọn số thẻ mới/ngày]
    B --> C[D6 Bước 2: xác nhận múi giờ]
    C --> D[Gợi ý 3 deck OFFICIAL]
    D -->|Đăng ký 1 deck| E[D7 Dashboard có thẻ để học]
    D -->|Bỏ qua| F[D7 Dashboard trống + CTA tạo deck]
```

### F-L2 · Phiên ôn tập hằng ngày (luồng quan trọng nhất)
```mermaid
flowchart TD
    A[D7 Dashboard: 'Còn 32 thẻ'] -->|Bắt đầu ôn| B[GET /api/reviews/due]
    B --> C{Có thẻ?}
    C -->|Không| C1[Empty: 'Hôm nay xong rồi!' + giờ thẻ tiếp theo đến hạn]
    C -->|Có| D[D13 Mặt trước: word]
    D -->|Space / chạm thẻ| E[Mặt sau: nghĩa, IPA, ví dụ + 4 nút]
    E -->|1 Quên · 2 Khó · 3 Nhớ · 4 Dễ| F[POST /api/reviews]
    F --> G[SM-2 tính interval, EF, nextReviewAt]
    G --> H{Q = Quên?}
    H -->|Có| I[Đưa thẻ về cuối hàng đợi phiên]
    H -->|Không| J[Bỏ khỏi hàng đợi]
    I --> K{Còn thẻ?}
    J --> K
    K -->|Có| D
    K -->|Hết| L[D14 Tổng kết phiên + streak]
    F -.->|Lỗi mạng| M[Toast 'Chưa lưu được, thử lại' - giữ nguyên thẻ]
```

### F-L3 · Tạo deck và thêm card
```mermaid
flowchart LR
    A[D8 Thư viện] -->|+ Tạo deck| B[D9 Form deck]
    B -->|Tên trùng - R2| B
    B -->|Lưu| C[D10 Chi tiết deck - trống]
    C -->|+ Thêm thẻ| D[D11 Form card]
    D -->|Từ đã có trong deck - R2| D
    D -->|Lưu & thêm tiếp| D
    D -->|Lưu| C
    C -->|Import CSV| E[F-L4]
```

### F-L4 · Import CSV
```mermaid
flowchart LR
    A[D12 Bước 1: chọn file .csv] --> B[POST import?dryRun=true]
    B --> C[D12 Bước 2: xem trước — dòng hợp lệ / dòng lỗi]
    C -->|Sửa file, tải lại| A
    C -->|Nhập N dòng hợp lệ| D[POST import?dryRun=false]
    D --> E[D12 Bước 3: kết quả + link về deck]
```

### F-L5 · Đăng ký (subscribe) deck công khai
```mermaid
flowchart LR
    A[D4 Khám phá] --> B[D5 Xem trước deck]
    B --> C{Deck của chính mình? - R4}
    C -->|Có| C1[Ẩn nút, hiện 'Deck của bạn']
    C -->|Không| D[Bấm 'Thêm vào thư viện']
    D --> E[POST /subscription]
    E --> F[Toast + nút đổi thành 'Bắt đầu học']
```

### F-L6 · Report deck
```mermaid
flowchart LR
    A[D10 Deck đã đăng ký / D5] -->|⋯ Báo cáo| B[D17 Modal: chọn lý do + mô tả]
    B -->|Gửi| C[POST /reports]
    C --> D[Toast 'Cảm ơn, admin sẽ xem xét']
```

## Admin

### F-A1 · Khóa / mở khóa user
```mermaid
flowchart LR
    A[D19 Quản lý user] -->|Lọc, tìm email| B[Chọn user]
    B -->|Khóa| C[ConfirmDialog: nêu hậu quả]
    C -->|Hủy| A
    C -->|Xác nhận| D[PATCH /admin/users/:id/status]
    D --> E[Badge đổi sang LOCKED + toast]
    B -.->|Chính mình| F[Nút bị vô hiệu: không tự khóa mình]
```

### F-A2 · Tạo deck chính thức
```mermaid
flowchart LR
    A[D20 Deck chính thức] -->|+ Tạo| B[D9 Form deck - visibility cố định OFFICIAL]
    B --> C[D10 Chi tiết deck - thêm card / import CSV]
```

### F-A3 · Xử lý report
```mermaid
flowchart TD
    A[D21 Hàng đợi report - lọc OPEN] --> B[Xem chi tiết: lý do + xem trước deck]
    B -->|Ẩn deck| C[ConfirmDialog]
    C --> D[Report RESOLVED, deck hidden = true]
    B -->|Bỏ qua| E[Report DISMISSED]
    D --> A
    E --> A
```

## System (không có màn hình riêng)
- **UC-S1 SM-2**: kết quả thể hiện ở D13 (khoảng thời gian trên mỗi nút), D10 (trạng thái thẻ).
- **UC-S2 Job hằng đêm**: kết quả thể hiện ở D7 (streak), D15 (heatmap, thống kê).
