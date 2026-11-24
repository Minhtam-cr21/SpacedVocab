import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { reviewApi } from '../api/endpoints.js';
import Loading from '../components/Loading.jsx';
import SpeakButton, { speak } from '../components/SpeakButton.jsx';
import Mascot from '../mascot/Robot.jsx';
import { formatDays } from '../util/format.js';

/** The 4 rating buttons and their quality (business-spec §5). */
const RATINGS = [
  { quality: 1, label: 'Quên', emoji: '😵', key: '1' },
  { quality: 3, label: 'Khó', emoji: '😅', key: '2' },
  { quality: 4, label: 'Nhớ', emoji: '😊', key: '3' },
  { quality: 5, label: 'Dễ', emoji: '🤩', key: '4' },
];

const CHEERS = ['Great job! ⭐', 'Awesome! 🎉', 'You got it! 💪', 'Super! 🌟', 'Nice work! 👏'];
const ENCOURAGE = ["That's okay! We'll see it again soon. 💛", "No worries, practice makes perfect! 🌱"];
const pick = (list) => list[Math.floor(Math.random() * list.length)];

/** Little stars flying out of the rating button (skipped when the user prefers reduced motion). */
function burstFrom(element) {
  if (!element || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const rect = element.getBoundingClientRect();
  for (let i = 0; i < 6; i += 1) {
    const star = document.createElement('span');
    star.className = 'burst';
    star.textContent = i % 2 ? '⭐' : '✨';
    star.style.left = `${rect.left + rect.width / 2}px`;
    star.style.top = `${rect.top}px`;
    const angle = (Math.PI * (i + 1)) / 7;
    star.style.setProperty('--dx', `${Math.cos(angle) * 90}px`);
    star.style.setProperty('--dy', `${-Math.sin(angle) * 90}px`);
    document.body.appendChild(star);
    setTimeout(() => star.remove(), 950);
  }
}

/** D13 · Phiên ôn tập + D14 · Tổng kết. */
export default function ReviewPage() {
  const [params] = useSearchParams();
  const deckId = params.get('deckId');
  const [session, setSession] = useState({ loading: true });
  const [index, setIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [mascotLine, setMascotLine] = useState(null);
  const [stats, setStats] = useState({ done: 0, remembered: 0 });

  useEffect(() => {
    reviewApi.due({ deckId, limit: 50 })
      .then((due) => setSession({ due }))
      .catch((err) => setSession({ error: err }));
  }, [deckId]);

  const cards = session.due?.cards ?? [];
  const card = cards[index];

  const grade = useCallback(async (rating, element) => {
    if (!card || busy) return;
    setBusy(true);
    setError(null);
    try {
      await reviewApi.grade(card.cardId, rating.quality);
      const remembered = rating.quality >= 3;
      if (remembered) burstFrom(element);
      setMascotLine(remembered ? pick(CHEERS) : pick(ENCOURAGE));
      setStats((s) => ({ done: s.done + 1, remembered: s.remembered + (remembered ? 1 : 0) }));
      setFlipped(false);
      setIndex((i) => i + 1);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }, [card, busy]);

  // Keyboard: Space / Enter flips, 1–4 rate once the card is flipped.
  useEffect(() => {
    function onKey(event) {
      if (!card || event.target.closest('input, textarea')) return;
      // links and buttons keep their own Space/Enter behaviour (the card itself is a div)
      if ((event.key === ' ' || event.key === 'Enter') && !event.target.closest('a, button')) {
        event.preventDefault();
        setFlipped((f) => !f);
        return;
      }
      const rating = RATINGS.find((r) => r.key === event.key);
      if (rating && flipped) grade(rating, document.querySelector(`.rate-btn--${rating.quality}`));
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [card, flipped, grade]);

  if (session.loading) return <Loading message="Getting your cards ready… 🃏" />;

  if (session.error) {
    const brainMissing = session.error.status >= 500;
    return (
      <div className="sheet center-state">
        <Mascot message={brainMissing ? 'Oops! My memory chip (SM-2) is not ready yet. 🔧' : 'Oh no, something went wrong. 😟'} mood="oops" size={130} small />
        <h1>Chưa ôn được</h1>
        <p className="muted">
          {brainMissing
            ? 'Thuật toán lên lịch ôn (SM-2) chưa được cài đặt xong, nên phiên ôn tập chưa mở được. Bạn vẫn có thể tạo deck và thêm thẻ trước.'
            : session.error.message}
        </p>
        <Link className="btn btn--primary" to="/app/library">Mở thư viện</Link>
      </div>
    );
  }

  // D14 · Tổng kết (or nothing to study)
  if (!card) {
    const nothingToday = stats.done === 0;
    return (
      <div className="sheet center-state">
        <Mascot
          message={nothingToday ? 'All done for today! Take a rest. 😴' : `Yay! You reviewed ${stats.done} cards! 🎉`}
          mood={nothingToday ? 'sleepy' : 'proud'}
          size={140}
          small
        />
        <h1>{nothingToday ? 'Hôm nay xong rồi! 🎉' : 'Hoàn thành phiên ôn!'}</h1>
        {!nothingToday && (
          <section className="tiles" style={{ width: '100%', maxWidth: 420 }}>
            <div className="tile tile--honey"><div className="tile__num">{stats.done}</div><div className="tile__label">Thẻ đã ôn</div></div>
            <div className="tile tile--matcha"><div className="tile__num">{Math.round((stats.remembered / stats.done) * 100)}%</div><div className="tile__label">Nhớ được</div></div>
          </section>
        )}
        <div className="row" style={{ justifyContent: 'center' }}>
          <Link className="btn btn--primary" to="/app">Về trang chủ</Link>
          <Link className="btn" to="/app/library">Thêm thẻ mới</Link>
        </div>
      </div>
    );
  }

  const percent = Math.round((index / cards.length) * 100);

  return (
    <div className="review">
      <div className="review__top">
        <Link className="icon-btn" to="/app" aria-label="Thoát phiên ôn">✕</Link>
        <div className="progress" role="progressbar" aria-valuemin={0} aria-valuemax={cards.length} aria-valuenow={index} aria-label="Tiến độ phiên ôn">
          <div className="progress__bar" style={{ width: `${percent}%` }} />
        </div>
        <span style={{ fontWeight: 800 }}>{index + 1}/{cards.length}</span>
      </div>

      {mascotLine && (
        <div className="row" style={{ justifyContent: 'center', marginBottom: 'var(--space-4)' }}>
          <Mascot message={mascotLine} mood={mascotLine.includes('okay') || mascotLine.includes('worries') ? 'happy' : 'proud'} size={70} small />
        </div>
      )}

      <div
        className={`flashcard${flipped ? ' is-flipped' : ''}`}
        onClick={() => setFlipped((f) => !f)}
        role="button"
        tabIndex={0}
        aria-label={flipped ? 'Mặt sau thẻ — bấm để lật lại' : 'Mặt trước thẻ — bấm để xem nghĩa'}
      >
        <div className="flashcard__inner">
          <div className="flashcard__face" aria-hidden={flipped}>
            <span className={`badge badge--${card.state.toLowerCase()}`}>{{ NEW: 'Thẻ mới', LEARNING: 'Đang học', REVIEW: 'Ôn lại' }[card.state]}</span>
            <div className="flashcard__word" lang="en">{card.word}</div>
            {card.ipa && <div className="muted">{card.ipa}</div>}
            <SpeakButton text={card.word} />
            <div className="flashcard__hint">Bấm vào thẻ hoặc nhấn <span className="kbd">Space</span> để xem nghĩa</div>
          </div>
          <div className="flashcard__face flashcard__face--back" aria-hidden={!flipped}>
            <div className="flashcard__word" lang="en" style={{ fontSize: 'var(--text-2xl)' }}>{card.word}</div>
            {card.partOfSpeech && <span className="badge" lang="en">{card.partOfSpeech}</span>}
            <div className="flashcard__meaning">{card.meaning}</div>
            {card.example && (
              <p className="muted" lang="en" style={{ fontStyle: 'italic' }}>
                “{card.example}”{' '}
                <button type="button" className="icon-btn" style={{ width: 36, height: 36, fontSize: '1rem' }}
                  aria-label="Nghe câu ví dụ" onClick={(e) => { e.stopPropagation(); speak(card.example); }}>🔊</button>
              </p>
            )}
          </div>
        </div>
      </div>

      {error && <p className="alert" role="alert" style={{ marginBottom: 'var(--space-4)' }}>{error.message}</p>}

      {flipped ? (
        <div className="rating" role="group" aria-label="Bạn nhớ từ này thế nào?">
          {RATINGS.map((rating) => (
            <button
              key={rating.quality}
              type="button"
              className={`rate-btn rate-btn--${rating.quality}`}
              disabled={busy}
              onClick={(e) => grade(rating, e.currentTarget)}
            >
              <span className="rate-btn__emoji" aria-hidden="true">{rating.emoji}</span>
              {rating.label} <span className="kbd" aria-hidden="true">{rating.key}</span>
              <span className="rate-btn__days">{formatDays(card.previewIntervals?.[rating.quality])}</span>
            </button>
          ))}
        </div>
      ) : (
        <button type="button" className="btn btn--primary btn--block btn--big" onClick={() => setFlipped(true)}>
          Lật thẻ 🔄
        </button>
      )}
    </div>
  );
}
