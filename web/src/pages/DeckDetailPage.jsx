import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { cardApi, deckApi } from '../api/endpoints.js';
import ConfirmButton from '../components/ConfirmButton.jsx';
import Field from '../components/Field.jsx';
import Loading from '../components/Loading.jsx';
import SpeakButton from '../components/SpeakButton.jsx';
import Mascot from '../mascot/Robot.jsx';

const EMPTY_CARD = { word: '', meaning: '', ipa: '', partOfSpeech: '', example: '' };
const PAGE_SIZE = 20;

/** D11 · Thêm thẻ. Keeps focus on "word" after saving so many words can be typed in a row. */
function NewCardForm({ deckId, onSaved }) {
  const [form, setForm] = useState(EMPTY_CARD);
  const [errors, setErrors] = useState({});
  const [saved, setSaved] = useState(null);
  const [busy, setBusy] = useState(false);
  const wordRef = useRef(null);
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value });

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setSaved(null);
    try {
      const blankToNull = (v) => (v.trim() ? v.trim() : null);
      const card = await cardApi.create(deckId, {
        word: form.word, meaning: form.meaning,
        ipa: blankToNull(form.ipa), partOfSpeech: blankToNull(form.partOfSpeech), example: blankToNull(form.example),
      });
      setForm(EMPTY_CARD);
      setErrors({});
      setSaved(card.word);
      onSaved();
      wordRef.current?.focus();
    } catch (err) {
      const fieldErrors = err.fieldErrors();
      if (err.code === 'CARD_WORD_TAKEN') fieldErrors.word = `"${form.word.trim()}" đã có trong deck này.`;
      setErrors(Object.keys(fieldErrors).length ? fieldErrors : { form: err.message });
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="sheet" onSubmit={submit} noValidate>
      <h2>Thêm thẻ mới</h2>
      {saved && <p className="alert alert--ok" role="status">Đã lưu thẻ "{saved}". Thêm từ tiếp theo nhé!</p>}
      {errors.form && <p className="alert" role="alert">{errors.form}</p>}
      <Field label="Từ tiếng Anh" lang="en" required maxLength={100} ref={wordRef} value={form.word} onChange={update('word')}
        placeholder="cat" error={errors.word} />
      <Field label="Nghĩa tiếng Việt" required maxLength={500} value={form.meaning} onChange={update('meaning')}
        placeholder="con mèo" error={errors.meaning} />
      <Field label="Phiên âm (IPA)" maxLength={100} value={form.ipa} onChange={update('ipa')} placeholder="/kæt/" hint="Không bắt buộc." error={errors.ipa} />
      <Field label="Từ loại" maxLength={30} value={form.partOfSpeech} onChange={update('partOfSpeech')} placeholder="noun" hint="Không bắt buộc." error={errors.partOfSpeech} />
      <Field label="Câu ví dụ" lang="en" as="textarea" maxLength={500} value={form.example} onChange={update('example')}
        placeholder="The cat is sleeping on the sofa." hint="Không bắt buộc." error={errors.example} />
      <button className="btn btn--primary" type="submit" disabled={busy} style={{ marginTop: 'var(--space-5)' }}>
        {busy ? 'Đang lưu…' : 'Lưu & thêm tiếp'}
      </button>
    </form>
  );
}

/** D10 · Chi tiết deck: tiến độ, danh sách thẻ (tìm kiếm + phân trang), thêm/xóa thẻ. */
export default function DeckDetailPage() {
  const { deckId } = useParams();
  const navigate = useNavigate();
  const [deck, setDeck] = useState(null);
  const [cards, setCards] = useState(null);
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [error, setError] = useState(null);
  const [adding, setAdding] = useState(false);

  const loadDeck = useCallback(() => deckApi.get(deckId).then(setDeck).catch(setError), [deckId]);
  const loadCards = useCallback(
    () => cardApi.search(deckId, { q: query, page, size: PAGE_SIZE }).then(setCards).catch(setError),
    [deckId, query, page],
  );

  useEffect(() => { loadDeck(); }, [loadDeck]);
  useEffect(() => {
    const timer = setTimeout(loadCards, 250); // debounce typing in the search box
    return () => clearTimeout(timer);
  }, [loadCards]);

  const refresh = () => { loadDeck(); loadCards(); };

  if (error?.status === 404) {
    return (
      <div className="sheet center-state">
        <Mascot message="Hmm… I can't find this deck. 🔍" mood="oops" size={110} small />
        <p>Không tìm thấy deck này, có thể nó đã bị xóa.</p>
        <Link className="btn" to="/app/library">Về thư viện</Link>
      </div>
    );
  }
  if (!deck) return error ? <p className="alert" role="alert">{error.message}</p> : <Loading />;

  const progress = deck.progress ?? { newCount: 0, learningCount: 0, reviewCount: 0 };

  async function removeDeck() {
    try {
      await deckApi.remove(deck.id);
      navigate('/app/library', { replace: true });
    } catch (err) {
      setError(err);
    }
  }

  async function removeCard(cardId) {
    try {
      await cardApi.remove(deck.id, cardId);
      refresh();
    } catch (err) {
      setError(err);
    }
  }

  return (
    <div className="stack">
      <p><Link to="/app/library">← Thư viện</Link></p>
      <div className="page-head">
        <div>
          <h1>{deck.name}</h1>
          {deck.description && <p className="muted" style={{ margin: 0 }}>{deck.description}</p>}
        </div>
        <div className="row">
          {deck.cardCount > 0 && <Link className="btn btn--primary" to={`/app/review?deckId=${deck.id}`}>Ôn deck này ▶</Link>}
          {deck.editable && !adding && <button className="btn btn--matcha" type="button" onClick={() => setAdding(true)}>+ Thêm thẻ</button>}
        </div>
      </div>

      {error && <p className="alert" role="alert">{error.message}</p>}

      <section className="tiles tiles--3" aria-label="Tiến độ của bạn">
        <div className="tile tile--sky"><div className="tile__num">{progress.newCount}</div><div className="tile__label">Mới</div></div>
        <div className="tile tile--honey"><div className="tile__num">{progress.learningCount}</div><div className="tile__label">Đang học</div></div>
        <div className="tile tile--matcha"><div className="tile__num">{progress.reviewCount}</div><div className="tile__label">Đã thuộc</div></div>
      </section>

      {adding && <NewCardForm deckId={deck.id} onSaved={refresh} />}
      {adding && <button className="btn btn--ghost" type="button" onClick={() => setAdding(false)}>Xong, đóng form</button>}

      <section className="sheet">
        <div className="page-head" style={{ marginBottom: 'var(--space-3)' }}>
          <h2 style={{ margin: 0 }}>Các thẻ ({deck.cardCount})</h2>
          <input className="input" type="search" placeholder="🔍 Tìm từ hoặc nghĩa…" aria-label="Tìm thẻ"
            style={{ maxWidth: 320 }} value={query} onChange={(e) => { setQuery(e.target.value); setPage(0); }} />
        </div>

        {cards?.items.length === 0 && (
          <div className="center-state">
            <p className="muted">{query ? 'Không tìm thấy thẻ nào. Thử từ khóa khác.' : 'Deck chưa có thẻ. Thêm thẻ đầu tiên nhé!'}</p>
            {!query && deck.editable && !adding && <button className="btn btn--matcha" type="button" onClick={() => setAdding(true)}>+ Thêm thẻ</button>}
          </div>
        )}

        {cards?.items.length > 0 && (
          <ul className="card-list">
            {cards.items.map((card) => (
              <li key={card.id} className="card-item">
                <SpeakButton text={card.word} />
                <div className="card-item__body">
                  <div>
                    <span className="card-item__word" lang="en">{card.word}</span>
                    {card.ipa && <span className="card-item__ipa">{card.ipa}</span>}
                    {card.partOfSpeech && <span className="badge" style={{ marginLeft: 8 }} lang="en">{card.partOfSpeech}</span>}
                  </div>
                  <div>{card.meaning}</div>
                  {card.example && <div className="card-item__example" lang="en">“{card.example}”</div>}
                </div>
                {deck.editable && <ConfirmButton onConfirm={() => removeCard(card.id)}>Xóa</ConfirmButton>}
              </li>
            ))}
          </ul>
        )}

        {cards?.totalPages > 1 && (
          <nav className="pager" aria-label="Phân trang">
            <button className="btn btn--small" type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>← Trước</button>
            <span>Trang {page + 1}/{cards.totalPages}</span>
            <button className="btn btn--small" type="button" disabled={page + 1 >= cards.totalPages} onClick={() => setPage(page + 1)}>Sau →</button>
          </nav>
        )}
      </section>

      {deck.editable && (
        <section className="row" style={{ justifyContent: 'flex-end' }}>
          <ConfirmButton confirmLabel={`Xóa deck "${deck.name}"?`} onConfirm={removeDeck}>🗑 Xóa deck</ConfirmButton>
        </section>
      )}
    </div>
  );
}
