# 01 · Nghiên cứu UX

> Persona dưới đây là **giả định** dựa trên đối tượng mục tiêu, chưa qua phỏng vấn người dùng thật. Cần kiểm chứng khi có người dùng thử.

## Persona

### P1 · Minh Anh — sinh viên ôn IELTS (Learner)
| | |
|---|---|
| **Bối cảnh** | 20 tuổi, năm 3, mục tiêu IELTS 6.5 trong 6 tháng. Học trên **điện thoại** lúc đi xe buýt, trước khi ngủ. |
| **Mục tiêu** | Nhớ khoảng 1.500 từ học thuật. Thấy mình tiến bộ mỗi ngày. |
| **Nỗi đau** | Học xong quên nhanh. Không biết hôm nay nên ôn từ nào. Sổ từ vựng giấy dày lên mà không ôn lại. |
| **Kịch bản** | Mở app → thấy "32 thẻ cần ôn" → bấm **Bắt đầu ôn** → ôn 10 phút bằng một tay → xem streak tăng lên. |
| **Cần từ app** | Ôn nhanh, phím/nút to, deck IELTS có sẵn, streak tạo động lực. |

### P2 · Anh Tuấn — người đi làm, 10 phút mỗi ngày (Learner)
| | |
|---|---|
| **Bối cảnh** | 28 tuổi, kỹ sư phần mềm, đọc tài liệu tiếng Anh. Học trên **laptop** giờ nghỉ trưa. |
| **Mục tiêu** | Ghi lại từ mới gặp khi đọc tài liệu và ôn đều đặn. |
| **Nỗi đau** | Nhập từng từ rất lâu. Ít thời gian nên cần biết chính xác khi nào học xong. |
| **Kịch bản** | Dán danh sách từ vào CSV → **import** 50 từ → ôn bằng **bàn phím** (Space, 1–4) → xem thống kê tuần. |
| **Cần từ app** | Import CSV, phím tắt, giới hạn số thẻ mới mỗi ngày, thống kê gọn. |

### P3 · Chị Hà — quản trị nội dung (Admin)
| | |
|---|---|
| **Bối cảnh** | Giáo viên tiếng Anh, phụ trách bộ thẻ chính thức và kiểm duyệt nội dung. Dùng **desktop**. |
| **Mục tiêu** | Duy trì deck chính thức chất lượng, xử lý report nhanh, chặn tài khoản spam. |
| **Nỗi đau** | Report bị bỏ sót. Khóa nhầm user mà không có bước xác nhận. |
| **Kịch bản** | Mở **Hàng đợi report** → xem deck bị báo cáo → **Ẩn deck** → chuyển sang **Quản lý user** → khóa tài khoản spam (có hộp thoại xác nhận). |
| **Cần từ app** | Bảng dữ liệu có lọc, thao tác hàng loạt rõ ràng, hành động nguy hiểm luôn cần xác nhận. |

## Hành trình chính của Learner (P1)

| Giai đoạn | Hành động | Cảm xúc | Cơ hội cho thiết kế |
|---|---|---|---|
| Khám phá | Xem deck công khai khi chưa đăng nhập | Tò mò | Cho xem trước 10 thẻ, nút "Đăng ký để học" |
| Bắt đầu | Đăng ký → chọn mục tiêu mỗi ngày | Hào hứng | Onboarding ≤ 2 bước, gợi ý deck chính thức |
| Học hằng ngày | Ôn thẻ đến hạn | Tập trung / chán nếu quá nhiều | Thanh tiến độ, giới hạn thẻ mới |
| Duy trì | Xem streak, thống kê | Tự hào / áp lực khi mất streak | Ăn mừng nhẹ, không phạt nặng |

## Nguyên tắc UX của SpacedVocab

1. **Từ lúc mở app đến lúc bắt đầu ôn: ≤ 2 chạm.** Dashboard luôn có nút "Bắt đầu ôn" ở vị trí nổi bật nhất.
2. **Luôn nói rõ "hôm nay còn bao nhiêu thẻ"** — ở dashboard, trên thanh điều hướng, trong phiên ôn.
3. **Ôn bằng một tay trên mobile** (các nút chấm điểm nằm ở nửa dưới màn hình) và **chỉ bằng bàn phím trên desktop** (`Space`, `1–4`).
4. **Hành động nguy hiểm phải xác nhận** (xóa deck, khóa user, ẩn deck) và nêu rõ hậu quả.
5. **Lỗi nói bằng ngôn ngữ người dùng**, có hướng khắc phục — không hiện mã lỗi kỹ thuật.
6. **Động lực nhẹ nhàng**: khen khi xong phiên, không làm người học xấu hổ khi mất streak.
7. **Tiếng Việt cho giao diện, tiếng Anh cho nội dung thẻ** — phân biệt bằng font/kiểu chữ.
