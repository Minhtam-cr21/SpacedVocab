/* =========================================================
   SpacedVocab prototype — shell, mock data, SM-2, tương tác
   Chỉ phục vụ prototype; app React sẽ viết lại theo cùng hành vi.
   ========================================================= */
(function () {
  "use strict";

  /* ---------- Theme (light / dark) ---------- */
  const THEME_KEY = "sv-proto-theme";
  function storedTheme() { try { return localStorage.getItem(THEME_KEY); } catch (e) { return null; } }
  function applyTheme(t) {
    if (t === "light" || t === "dark") document.documentElement.dataset.theme = t;
    else delete document.documentElement.dataset.theme;
  }
  applyTheme(storedTheme());
  function toggleTheme() {
    const isDark = document.documentElement.dataset.theme === "dark" ||
      (!document.documentElement.dataset.theme && matchMedia("(prefers-color-scheme: dark)").matches);
    const next = isDark ? "light" : "dark";
    applyTheme(next);
    try { localStorage.setItem(THEME_KEY, next); } catch (e) { /* private mode */ }
  }

  /* ---------- Mock data ---------- */
  const DUE_TODAY = 32;
  const data = {
    user: { name: "Minh Anh", email: "minhanh@example.com", streak: 12, dailyNewLimit: 20, timezone: "Asia/Ho_Chi_Minh" },
    decks: [
      { id: 1, name: "IELTS Academic Core", owner: "SpacedVocab", visibility: "OFFICIAL", cards: 600, due: 18, learners: 1240 },
      { id: 2, name: "Từ vựng Spring Boot", owner: "Bạn", visibility: "PRIVATE", cards: 86, due: 9, learners: 0 },
      { id: 3, name: "Phrasal verbs hằng ngày", owner: "Bạn", visibility: "PUBLIC", cards: 140, due: 5, learners: 37 },
      { id: 4, name: "Oxford 3000", owner: "SpacedVocab", visibility: "OFFICIAL", cards: 3000, due: 0, learners: 5320 }
    ],
    reviewQueue: [
      { word: "ubiquitous", ipa: "/juːˈbɪk.wɪ.təs/", pos: "adj", meaning: "có mặt ở khắp nơi, phổ biến", example: "Smartphones have become ubiquitous in modern life.", ef: 2.5, reps: 0, interval: 0, state: "NEW" },
      { word: "mitigate", ipa: "/ˈmɪt.ɪ.ɡeɪt/", pos: "verb", meaning: "giảm nhẹ, làm dịu bớt", example: "The new policy aims to mitigate the risks of flooding.", ef: 2.5, reps: 1, interval: 1, state: "LEARNING" },
      { word: "resilient", ipa: "/rɪˈzɪl.i.ənt/", pos: "adj", meaning: "kiên cường, mau phục hồi", example: "Children are often remarkably resilient.", ef: 2.36, reps: 2, interval: 6, state: "REVIEW" },
      { word: "deploy", ipa: "/dɪˈplɔɪ/", pos: "verb", meaning: "triển khai (ứng dụng, lực lượng)", example: "We deploy the API with Docker Compose.", ef: 2.6, reps: 4, interval: 31, state: "REVIEW" },
      { word: "scrutinize", ipa: "/ˈskruː.tɪ.naɪz/", pos: "verb", meaning: "xem xét kỹ lưỡng", example: "Reviewers scrutinize every pull request.", ef: 1.96, reps: 3, interval: 12, state: "REVIEW" }
    ]
  };

  /* ---------- SM-2 (khớp docs/domain/business-spec.md §5) ---------- */
  function sm2(state, q) {
    if (q < 0 || q > 5) throw new RangeError("quality phải trong 0..5");
    if (q < 3) return { ef: state.ef, reps: 0, interval: 1 };
    const interval = state.reps === 0 ? 1 : state.reps === 1 ? 6 : Math.round(state.interval * state.ef);
    const ef = Math.max(1.3, state.ef + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)));
    return { ef: Math.round(ef * 100) / 100, reps: state.reps + 1, interval };
  }
  function formatInterval(days) {
    if (days < 30) return days + " ngày";
    if (days < 365) return Math.round(days / 30) + " tháng";
    return (Math.round(days / 36.5) / 10) + " năm";
  }
  const RATINGS = [
    { key: "1", q: 1, label: "Quên", cls: "rate-again" },
    { key: "2", q: 3, label: "Khó", cls: "rate-hard" },
    { key: "3", q: 4, label: "Nhớ", cls: "rate-good" },
    { key: "4", q: 5, label: "Dễ", cls: "rate-easy" }
  ];

  /* ---------- Helpers ---------- */
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

  function toast(message, actionLabel) {
    let region = $(".toast-region");
    if (!region) {
      region = document.createElement("div");
      region.className = "toast-region"; region.setAttribute("role", "status"); region.setAttribute("aria-live", "polite");
      document.body.appendChild(region);
    }
    const el = document.createElement("div");
    el.className = "toast";
    el.innerHTML = `<span class="grow">${esc(message)}</span>` + (actionLabel ? `<button type="button">${esc(actionLabel)}</button>` : "");
    region.appendChild(el);
    el.querySelector("button")?.addEventListener("click", () => el.remove());
    setTimeout(() => el.remove(), 4000);
  }

  /* ---------- Shell ---------- */
  const LEARNER_NAV = [
    { id: "dashboard", href: "dashboard.html", icon: "🏠", label: "Trang chủ" },
    { id: "library", href: "library.html", icon: "📚", label: "Thư viện" },
    { id: "review", href: "review.html", icon: "▶", label: "Ôn tập", primary: true, badge: DUE_TODAY },
    { id: "stats", href: "stats.html", icon: "📊", label: "Thống kê" },
    { id: "settings", href: "settings.html", icon: "👤", label: "Tôi" }
  ];
  const ADMIN_NAV = [
    { id: "admin-dashboard", href: "admin-dashboard.html", icon: "📈", label: "Tổng quan" },
    { id: "admin-users", href: "admin-users.html", icon: "👥", label: "Người dùng" },
    { id: "admin-decks", href: "admin-decks.html", icon: "🗂", label: "Deck chính thức" },
    { id: "admin-reports", href: "admin-reports.html", icon: "🚩", label: "Report", badge: 3 }
  ];
  const brand = (href) => `<a class="brand" href="${href}"><span class="brand-mark" aria-hidden="true">S</span>SpacedVocab</a>`;
  const navItem = (n, active) =>
    `<a class="nav-item" href="${n.href}"${n.id === active ? ' aria-current="page"' : ""}><span class="icon" aria-hidden="true">${n.icon}</span>${n.label}${n.badge ? `<span class="badge badge-count" aria-label="${n.badge} mục">${n.badge}</span>` : ""}</a>`;

  function renderShell() {
    const body = document.body;
    const shell = body.dataset.shell;
    const active = body.dataset.active;
    const main = $("main");
    if (!main) return;

    if (shell === "guest") {
      main.insertAdjacentHTML("beforebegin", `
        <header class="topbar">
          ${brand("landing.html")}
          <nav class="topbar-links" aria-label="Điều hướng chính">
            <a href="explore.html">Khám phá</a>
            <a href="login.html">Đăng nhập</a>
            <a class="btn btn-primary btn-sm" href="register.html">Đăng ký</a>
          </nav>
          <a class="btn btn-secondary btn-sm topbar-mobile-only" href="login.html">Đăng nhập</a>
        </header>`);
    }

    if (shell === "learner" || shell === "admin") {
      const nav = shell === "learner" ? LEARNER_NAV : ADMIN_NAV;
      const wrapper = document.createElement("div");
      wrapper.className = "app-shell has-sidebar";
      main.parentNode.insertBefore(wrapper, main);
      const extra = shell === "learner"
        ? `<a class="nav-item" href="explore.html"><span class="icon" aria-hidden="true">🔎</span>Khám phá</a>`
        : `<a class="nav-item" href="dashboard.html"><span class="icon" aria-hidden="true">↩</span>Về app học</a>`;
      wrapper.innerHTML = `
        <aside class="sidebar" aria-label="Điều hướng chính">
          ${brand(shell === "learner" ? "dashboard.html" : "admin-dashboard.html")}
          ${nav.map((n) => navItem(n, active)).join("")}
          ${extra}
          <div class="sidebar-footer stack-sm">
            ${shell === "admin" ? '<span class="badge badge-warning" style="align-self:flex-start">ADMIN</span>' : ""}
            <a class="nav-item" href="login.html"><span class="icon" aria-hidden="true">⎋</span>Đăng xuất</a>
          </div>
        </aside>
        <div class="shell-main">
          <header class="topbar">
            ${brand(shell === "learner" ? "dashboard.html" : "admin-dashboard.html")}
            ${shell === "learner"
              ? `<span class="badge badge-warning" title="Chuỗi ngày học">🔥 ${data.user.streak} ngày</span>`
              : `<button class="btn btn-ghost btn-icon" type="button" aria-label="Mở menu" aria-expanded="false" data-drawer-toggle>☰</button>`}
          </header>
          ${shell === "admin" ? `<nav class="drawer" aria-label="Menu admin">${nav.map((n) => navItem(n, active)).join("")}${extra}</nav>` : ""}
        </div>`;
      $(".shell-main", wrapper).appendChild(main);
      if (shell === "learner") {
        wrapper.insertAdjacentHTML("beforeend", `
          <nav class="bottom-nav" aria-label="Điều hướng nhanh">
            ${LEARNER_NAV.map((n) => `<a href="${n.href}" class="${n.primary ? "nav-primary" : ""}"${n.id === active ? ' aria-current="page"' : ""}>
              <span class="icon" aria-hidden="true">${n.icon}</span>${n.label}
              ${n.badge ? `<span class="badge badge-count">${n.badge}</span>` : ""}</a>`).join("")}
          </nav>`);
      }
      $("[data-drawer-toggle]", wrapper)?.addEventListener("click", (e) => {
        const d = $(".drawer", wrapper); const open = d.classList.toggle("open");
        e.currentTarget.setAttribute("aria-expanded", String(open));
      });
    }

    // Thanh công cụ chỉ có trong prototype
    body.insertAdjacentHTML("beforeend", `
      <div class="proto-bar" aria-label="Công cụ prototype">
        <a href="../index.html">☰ Mục lục</a>
        <button type="button" data-theme-toggle aria-label="Đổi giao diện sáng/tối">◐ Giao diện</button>
      </div>`);
    $("[data-theme-toggle]").addEventListener("click", toggleTheme);
  }

  /* ---------- Generic behaviors ---------- */
  function wireCommon() {
    // Mở dialog: <button data-open="id">
    $$("[data-open]").forEach((b) => b.addEventListener("click", () => document.getElementById(b.dataset.open)?.showModal()));
    $$("[data-close]").forEach((b) => b.addEventListener("click", () => b.closest("dialog")?.close()));
    // Toast: <button data-toast="nội dung">
    $$("[data-toast]").forEach((b) => b.addEventListener("click", () => {
      b.closest("dialog")?.close();
      toast(b.dataset.toast, b.dataset.toastAction);
    }));
    // Tabs: .tabs > .tab[aria-controls]
    $$(".tabs").forEach((tabs) => {
      const all = $$(".tab", tabs);
      all.forEach((t) => t.addEventListener("click", () => {
        all.forEach((x) => { x.setAttribute("aria-selected", String(x === t)); const p = document.getElementById(x.getAttribute("aria-controls")); if (p) p.hidden = x !== t; });
      }));
    });
    // Ngăn form prototype submit thật
    $$("form[data-proto]").forEach((f) => f.addEventListener("submit", (e) => {
      e.preventDefault();
      const target = f.dataset.next;
      if (f.dataset.validate === "register" && !validateRegister(f)) return;
      if (f.dataset.toastOk) toast(f.dataset.toastOk);
      if (target) setTimeout(() => { location.href = target; }, f.dataset.toastOk ? 600 : 0);
    }));
    // Demo state switcher: <select data-state-switch> đổi [data-state] hiển thị
    $$("[data-state-switch]").forEach((sel) => {
      const scope = document.getElementById(sel.dataset.stateSwitch);
      const apply = () => $$("[data-state]", scope).forEach((el) => { el.hidden = el.dataset.state !== sel.value; });
      sel.addEventListener("change", apply); apply();
    });
  }

  function validateRegister(form) {
    let ok = true;
    const setErr = (name, msg) => {
      const field = form.querySelector(`[name="${name}"]`).closest(".field");
      field.classList.toggle("has-error", !!msg);
      const out = field.querySelector(".error-text");
      out.hidden = !msg; out.textContent = msg ? "⚠ " + msg : "";
      if (msg) ok = false;
    };
    const email = form.email.value.trim();
    const pw = form.password.value;
    setErr("email", !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email) ? "Email chưa đúng định dạng." :
      email.toLowerCase() === "taken@example.com" ? "Email này đã được đăng ký. Bạn muốn đăng nhập?" : "");
    setErr("password", pw.length < 8 ? "Mật khẩu cần ít nhất 8 ký tự." : "");
    setErr("confirm", form.confirm.value !== pw ? "Mật khẩu nhập lại chưa khớp." : "");
    if (!ok) form.querySelector(".has-error input")?.focus();
    return ok;
  }

  /* ---------- Page: review (D13) ---------- */
  function initReview() {
    const queue = data.reviewQueue.map((c) => ({ ...c }));
    const total = queue.length;
    let done = 0, remembered = 0, flipped = false;
    const card = $("#flashcard");
    const ratingBar = $("#rating-bar");
    const revealBar = $("#reveal-bar");
    const live = $("#review-live");

    function render() {
      const c = queue[0];
      flipped = false;
      card.classList.remove("flipped");
      card.setAttribute("aria-label", `Thẻ: ${c.word}. Nhấn Space để lật.`);
      $("#front-word").textContent = c.word;
      $("#front-state").className = "badge card-state badge-" + c.state.toLowerCase();
      $("#front-state").textContent = c.state;
      $("#back-word").textContent = c.word;
      $("#back-ipa").textContent = c.ipa + " · " + c.pos;
      $("#back-meaning").textContent = c.meaning;
      $("#back-example").textContent = "“" + c.example + "”";
      RATINGS.forEach((r) => {
        const next = sm2(c, r.q);
        $(`#rate-${r.key} small`).textContent = r.q < 3 ? "Ôn lại ngay" : formatInterval(next.interval);
      });
      ratingBar.hidden = true; revealBar.hidden = false;
      $("#review-count").textContent = `${done}/${total}`;
      $("#review-progress").style.width = (done / total) * 100 + "%";
    }
    function flip() {
      if (flipped) return;
      flipped = true;
      card.classList.add("flipped");
      ratingBar.hidden = false; revealBar.hidden = true;
      live.textContent = `Nghĩa: ${queue[0].meaning}. Chọn mức độ nhớ từ 1 đến 4.`;
      $("#rate-3").focus();
    }
    function rate(q) {
      if (!flipped) return;
      const c = queue.shift();
      const next = sm2(c, q);
      if (q < 3) { queue.push({ ...c, ...next, state: "LEARNING" }); }
      else { done++; remembered++; }
      if (queue.length === 0) {
        location.href = `review-summary.html?reviewed=${total}&remembered=${remembered}`;
        return;
      }
      render();
      card.focus();
    }
    card.addEventListener("click", flip);
    $("#reveal-btn").addEventListener("click", flip);
    RATINGS.forEach((r) => $(`#rate-${r.key}`).addEventListener("click", () => rate(r.q)));
    document.addEventListener("keydown", (e) => {
      if (e.target.closest("input, textarea, select")) return;
      if (e.code === "Space" || e.key === "Enter") { if (!flipped) { e.preventDefault(); flip(); } }
      const r = RATINGS.find((x) => x.key === e.key);
      if (r && flipped) { e.preventDefault(); rate(r.q); }
      if (e.key === "Escape") location.href = "dashboard.html";
    });
    render();
  }

  /* ---------- Page: review summary (D14) ---------- */
  function initSummary() {
    const p = new URLSearchParams(location.search);
    const reviewed = Number(p.get("reviewed") || 32);
    const rememberedRaw = Number(p.get("remembered") || 27);
    const pct = reviewed ? Math.round((rememberedRaw / reviewed) * 100) : 0;
    $("#sum-reviewed").textContent = reviewed;
    $("#sum-rate").textContent = pct + "%";
    $("#sum-ring").style.setProperty("--value", pct);
  }

  /* ---------- Page: stats (D15) — heatmap ---------- */
  function initHeatmap() {
    $$(".heatmap[data-days]").forEach((h) => {
      const days = Number(h.dataset.days);
      let seed = 7;
      const rnd = () => { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; };
      let html = "";
      for (let i = 0; i < days; i++) {
        const v = rnd();
        const l = i > days - 13 ? 1 + Math.floor(v * 4) : v < 0.25 ? 0 : 1 + Math.floor(v * 4);
        html += `<span data-l="${Math.min(4, l)}" title="${Math.round(v * 60)} lượt ôn"></span>`;
      }
      h.innerHTML = html;
    });
  }

  /* ---------- Page: import (D12) — stepper ---------- */
  function initImport() {
    const steps = $$("[data-step]");
    const items = $$(".stepper li");
    const go = (n) => {
      steps.forEach((s) => { s.hidden = Number(s.dataset.step) !== n; });
      items.forEach((li, i) => { li.classList.toggle("done", i < n - 1); if (i === n - 1) li.setAttribute("aria-current", "step"); else li.removeAttribute("aria-current"); });
      window.scrollTo({ top: 0 });
    };
    $$("[data-go]").forEach((b) => b.addEventListener("click", () => go(Number(b.dataset.go))));
    const dz = $(".dropzone");
    if (dz) {
      ["dragenter", "dragover"].forEach((ev) => dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.add("dragover"); }));
      ["dragleave", "drop"].forEach((ev) => dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.remove("dragover"); if (ev === "drop") go(2); }));
      $("input[type=file]", dz)?.addEventListener("change", () => go(2));
    }
    go(1);
  }

  /* ---------- Page: admin users (D19) — confirm lock ---------- */
  function initAdminUsers() {
    const dlg = $("#confirm-lock");
    let row = null;
    $$("[data-lock]").forEach((b) => b.addEventListener("click", () => {
      row = b.closest("tr, .list-item");
      const locking = b.dataset.lock === "lock";
      $("#lock-title").textContent = locking ? "Khóa tài khoản này?" : "Mở khóa tài khoản này?";
      $("#lock-email").textContent = b.dataset.email;
      $("#lock-consequence").textContent = locking
        ? "Người dùng sẽ bị đăng xuất và không thể đăng nhập cho tới khi được mở khóa. Dữ liệu học tập vẫn được giữ nguyên."
        : "Người dùng có thể đăng nhập và học lại bình thường.";
      $("#lock-confirm").textContent = locking ? "Khóa tài khoản" : "Mở khóa";
      $("#lock-confirm").className = "btn " + (locking ? "btn-danger" : "btn-primary");
      $("#lock-confirm").dataset.locking = String(locking);
      dlg.showModal();
    }));
    $("#lock-confirm")?.addEventListener("click", (e) => {
      const locking = e.currentTarget.dataset.locking === "true";
      dlg.close();
      if (row) {
        const badge = $(".status-badge", row);
        badge.className = "badge status-badge " + (locking ? "badge-danger" : "badge-success");
        badge.textContent = locking ? "LOCKED" : "ACTIVE";
        const btn = $("[data-lock]", row);
        btn.dataset.lock = locking ? "unlock" : "lock";
        btn.textContent = locking ? "Mở khóa" : "Khóa";
      }
      toast(locking ? "Đã khóa tài khoản." : "Đã mở khóa tài khoản.", "Đóng");
    });
  }

  /* ---------- Page: admin reports (D21) ---------- */
  function initAdminReports() {
    $$("[data-report]").forEach((b) => b.addEventListener("click", () => {
      $$("[data-report]").forEach((x) => x.setAttribute("aria-pressed", String(x === b)));
      $("#report-detail-title").textContent = b.dataset.report;
      $("#report-detail-reason").textContent = b.dataset.reason;
    }));
  }

  /* ---------- Page: deck detail (D10) — owner vs subscriber ---------- */
  function initDeckDetail() {
    const sel = $("#view-as");
    if (!sel) return;
    const apply = () => $$("[data-view]").forEach((el) => { el.hidden = el.dataset.view !== sel.value; });
    sel.addEventListener("change", apply);
    if (new URLSearchParams(location.search).get("view") === "subscriber") sel.value = "subscriber";
    apply();
  }

  /* ---------- Page: login (D3) — demo error states ---------- */
  function initLogin() {
    const f = $("#login-form");
    f?.addEventListener("submit", (e) => {
      e.preventDefault();
      const email = f.email.value.trim().toLowerCase();
      $("#login-error").hidden = true; $("#login-locked").hidden = true;
      if (email === "locked@example.com") { $("#login-locked").hidden = false; return; }
      if (f.password.value !== "password123") { $("#login-error").hidden = false; f.password.focus(); return; }
      location.href = email.startsWith("admin") ? "admin-dashboard.html" : "dashboard.html";
    });
  }

  /* ---------- Boot ---------- */
  const PAGES = {
    review: initReview, "review-summary": initSummary, stats: initHeatmap, import: initImport,
    "admin-users": initAdminUsers, "admin-reports": initAdminReports, "deck-detail": initDeckDetail,
    login: initLogin, "style-guide": initHeatmap
  };
  document.addEventListener("DOMContentLoaded", () => {
    renderShell();
    wireCommon();
    PAGES[document.body.dataset.page]?.();
  });

  // Dùng cho kiểm thử trong console: SV.sm2({ef:2.5,reps:0,interval:0}, 4)
  window.SV = { sm2, formatInterval, toast, data };
})();
