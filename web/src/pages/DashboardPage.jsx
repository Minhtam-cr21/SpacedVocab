import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { deckApi, reviewApi } from '../api/endpoints.js';
import Loading from '../components/Loading.jsx';
import Mascot from '../mascot/Robot.jsx';
import { formatDateTime } from '../util/format.js';

/** D7 · Trang chủ: lời chào của robot + số thẻ cần ôn hôm nay. */
export default function DashboardPage() {
  const { user } = useAuth();
  const [state, setState] = useState({ loading: true });

  useEffect(() => {
    Promise.all([reviewApi.due({ limit: 0 }), deckApi.listOwned()])
      .then(([due, decks]) => setState({ due, decks }))
      .catch((error) => setState({ error }));
  }, []);

  if (state.loading) return <Loading />;

  const { due, decks, error } = state;
  const total = due ? due.reviewCount + due.newCount : 0;
  const cardCount = decks?.reduce((sum, d) => sum + d.cardCount, 0) ?? 0;

  return (
    <div className="stack">
      <section className="sheet hero">
        <div className="hero__mascot"><Mascot size={150} /></div>
        <div>
          <h1>Xin chào, {user?.displayName}!</h1>
          {error && <p className="alert" role="alert">{error.message}</p>}

          {due && decks.length === 0 && (
            <>
              <p className="muted">Thư viện còn trống. Tạo một deck rồi thêm vài từ để bắt đầu học nhé.</p>
              <Link className="btn btn--primary btn--big" to="/app/library?new=1">+ Tạo deck đầu tiên</Link>
            </>
          )}

          {due && decks.length > 0 && total > 0 && (
            <>
              <p className="muted">Hôm nay bạn có <strong>{total}</strong> thẻ đang chờ. Mỗi ngày một chút là nhớ lâu!</p>
              <Link className="btn btn--primary btn--big" to="/app/review">Bắt đầu ôn ▶</Link>
            </>
          )}

          {due && decks.length > 0 && total === 0 && (
            <>
              <p><strong>Hôm nay xong rồi! 🎉</strong></p>
              <p className="muted">
                {due.nextDueAt ? `Thẻ tiếp theo đến hạn vào ${formatDateTime(due.nextDueAt)}.` : 'Thêm thẻ mới vào deck để học tiếp nhé.'}
              </p>
              <Link className="btn btn--big" to="/app/library">Mở thư viện</Link>
            </>
          )}
        </div>
      </section>

      <Link to="/app/path" className="stage-card is-current">
        <span className="level-badge level-badge--A1" aria-hidden="true">🗺️</span>
        <span className="stage-card__body">
          <span className="stage-card__title">Lộ trình CEFR A1 → C2</span>
          <span className="stage-card__count">Học theo từng chặng, mỗi bài một chủ đề</span>
        </span>
        <span className="stage-card__status" aria-hidden="true">→</span>
      </Link>

      {due && (
        <section className="tiles tiles--3" aria-label="Tổng quan hôm nay">
          <div className="tile tile--honey">
            <div className="tile__num">{due.reviewCount}</div>
            <div className="tile__label">Thẻ đến hạn</div>
          </div>
          <div className="tile tile--sky">
            <div className="tile__num">{due.newCount}</div>
            <div className="tile__label">Thẻ mới hôm nay</div>
          </div>
          <div className="tile tile--matcha">
            <div className="tile__num">{cardCount}</div>
            <div className="tile__label">Tổng số thẻ · {decks.length} deck</div>
          </div>
        </section>
      )}
    </div>
  );
}
