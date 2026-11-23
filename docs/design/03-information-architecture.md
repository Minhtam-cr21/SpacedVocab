# 03 · Kiến trúc thông tin

## Sitemap

```mermaid
flowchart TD
    subgraph Guest["Công khai (Guest)"]
        L["/ Landing · D1"]
        REG["/register · D2"]
        LOG["/login · D3"]
        EXP["/explore · D4"]
        PUB["/explore/decks/:id · D5"]
    end
    subgraph App["/app (LEARNER)"]
        ONB["/app/onboarding · D6"]
        DASH["/app · D7"]
        LIB["/app/library · D8"]
        DF["/app/decks/new, /:id/edit · D9"]
        DD["/app/decks/:id · D10"]
        CF["/app/decks/:id/cards/new · D11"]
        IMP["/app/decks/:id/import · D12"]
        REV["/app/review · D13"]
        SUM["/app/review/summary · D14"]
        ST["/app/stats · D15"]
        SET["/app/settings · D16"]
    end
    subgraph Admin["/admin (ADMIN)"]
        AD["/admin · D18"]
        AU["/admin/users · D19"]
        ADK["/admin/decks · D20"]
        AR["/admin/reports · D21"]
    end
    L --> EXP --> PUB
    L --> REG --> ONB --> DASH
    L --> LOG
    DASH --> REV --> SUM
    DASH --> LIB --> DD --> CF
    DD --> IMP
    LIB --> DF
    AD --> AU
    AD --> ADK
    AD --> AR
```

## Điều hướng

| Shell | Mobile (< 768px) | Desktop (≥ 1024px) |
|---|---|---|
| **Guest** | Header: logo + "Đăng nhập" | Header: logo, Khám phá, Đăng nhập, Đăng ký |
| **Learner** | **Bottom nav** 5 mục: Trang chủ · Thư viện · **Ôn tập** (nút giữa, có badge số thẻ) · Thống kê · Tôi | **Sidebar** trái với cùng 5 mục + Khám phá |
| **Admin** | Header + menu dạng drawer | Sidebar: Tổng quan · Người dùng · Deck chính thức · Report (badge số OPEN) |

Phiên ôn tập (D13) **ẩn mọi thanh điều hướng** để tập trung, chỉ có nút ✕ thoát.

## Danh sách route (React Router)

| Route | Màn | Role | Ghi chú |
|---|---|---|---|
| `/` | D1 Landing | Tất cả | Đã đăng nhập → chuyển `/app` |
| `/register`, `/login` | D2, D3 | Guest | Đã đăng nhập → chuyển `/app` |
| `/explore` | D4 | Tất cả | Query `?q=&sort=` |
| `/explore/decks/:deckId` | D5 | Tất cả | Nút thay đổi theo role |
| `/app/onboarding` | D6 | LEARNER | Chỉ hiện lần đầu |
| `/app` | D7 Dashboard | LEARNER | |
| `/app/library` | D8 | LEARNER | Tab `?tab=owned|subscribed` |
| `/app/decks/new`, `/app/decks/:id/edit` | D9 | LEARNER (chủ) | |
| `/app/decks/:id` | D10 | LEARNER (chủ hoặc subscriber) | Subscriber: chỉ đọc |
| `/app/decks/:id/cards/new`, `.../cards/:cardId/edit` | D11 | LEARNER (chủ) | |
| `/app/decks/:id/import` | D12 | LEARNER (chủ) | |
| `/app/review` | D13 | LEARNER | `?deckId=` để ôn 1 deck |
| `/app/review/summary` | D14 | LEARNER | |
| `/app/stats` | D15 | LEARNER | |
| `/app/settings` | D16 | LEARNER, ADMIN | |
| `/admin` | D18 | ADMIN | |
| `/admin/users` | D19 | ADMIN | `?q=&status=` |
| `/admin/decks` | D20 | ADMIN | Tạo/sửa dùng lại D9–D12 với prefix `/admin` |
| `/admin/reports` | D21 | ADMIN | `?status=OPEN` |
| `/403`, `*` | D22 | Tất cả | |

## Ma trận quyền (role × màn hình × hành động)

Ký hiệu: ✅ được · 👁 chỉ xem · ❌ không · — không áp dụng

| Màn / tài nguyên | Guest | Learner (chủ) | Learner (khác) | Admin | Quy tắc |
|---|---|---|---|---|---|
| D4–D5 Deck PUBLIC/OFFICIAL | 👁 | 👁 | 👁 | 👁 | |
| Deck PRIVATE | ❌ | ✅ | ❌ (404) | ❌ | Không lộ sự tồn tại → trả 404 |
| Tạo/sửa/xóa deck PRIVATE/PUBLIC | ❌ | ✅ | ❌ | ❌ | R3 |
| Tạo/sửa/xóa deck OFFICIAL | ❌ | ❌ | ❌ | ✅ | R3 |
| Thêm/sửa/xóa/import card | ❌ | ✅ | ❌ | ✅ (deck OFFICIAL) | R2, R3 |
| Subscribe deck | ❌ | ❌ (deck của mình) | ✅ (PUBLIC/OFFICIAL) | — | R4 |
| Ôn tập thẻ của deck | ❌ | ✅ | ✅ nếu đã subscribe | — | R6 |
| Report deck | ❌ | ❌ | ✅ | — | |
| D15 Thống kê cá nhân | ❌ | ✅ (của mình) | ❌ | — | |
| D19 Khóa/mở user | ❌ | ❌ | ❌ | ✅ (trừ chính mình) | R1 |
| D21 Xử lý report | ❌ | ❌ | ❌ | ✅ | |

## Ánh xạ màn hình ↔ API (dự kiến — chốt lại ở từng milestone)

| Màn | Endpoint | Milestone |
|---|---|---|
| D2 | `POST /api/auth/register` `{email, password, displayName?, timezone?}` → 201 `AuthResponse` | M1 ✅ |
| D3 | `POST /api/auth/login` `{email, password}`, `POST /api/auth/refresh` `{refreshToken}`, `POST /api/auth/logout` `{refreshToken}` → 204 | M1 ✅ |
| D6, D16 | `GET /api/me`, `PUT /api/me` `{displayName}`, `PUT /api/me/settings` `{dailyNewLimit, timezone}`, `PUT /api/me/password` `{currentPassword, newPassword}` → 204 | M1 ✅ |
| D8 | `GET /api/decks?scope=owned|subscribed` | M2 / M4 |
| D9 | `POST /api/decks`, `PUT /api/decks/{id}`, `DELETE /api/decks/{id}` | M2 |
| D10 | `GET /api/decks/{id}`, `GET /api/decks/{id}/cards?q=&page=&size=` | M2 |
| D11 | `POST /api/decks/{id}/cards`, `PUT/DELETE /api/decks/{id}/cards/{cardId}` | M2 |
| D12 | `POST /api/decks/{id}/cards/import?dryRun=true|false` (multipart) | M4 |
| D7, D13 | `GET /api/reviews/due?deckId=&limit=` — mỗi thẻ kèm `previewIntervals` cho 4 nút | M3 |
| D13 | `POST /api/reviews` `{cardId, quality}` → trả `ScheduleResult` | M3 |
| D4, D5 | `GET /api/public/decks?q=&sort=&page=`, `GET /api/public/decks/{id}` | M4 |
| D5, D10 | `POST/DELETE /api/decks/{id}/subscription` | M4 |
| D17 | `POST /api/decks/{id}/reports` `{reason, detail}` | M4 |
| D18 | `GET /api/admin/stats` | M4 |
| D19 | `GET /api/admin/users?q=&status=&page=`, `PATCH /api/admin/users/{id}/status` | M4 |
| D20 | `POST/PUT/DELETE /api/admin/decks` (OFFICIAL) | M4 |
| D21 | `GET /api/admin/reports?status=`, `PATCH /api/admin/reports/{id}` `{action: HIDE_DECK|DISMISS}` | M4 |
| D7, D14, D15 | `GET /api/stats/overview`, `GET /api/stats/heatmap?from=&to=` | M5 |
