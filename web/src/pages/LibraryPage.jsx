import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { deckApi } from '../api/endpoints.js';
import Field from '../components/Field.jsx';
import Loading from '../components/Loading.jsx';
import Mascot from '../mascot/Robot.jsx';

const VISIBILITY_LABEL = { PRIVATE: '🔒 Riêng tư', PUBLIC: '🌍 Công khai', OFFICIAL: '⭐ Chính thức' };

/** D9 · Tạo deck (inline form). */
function NewDeckForm({ onCancel }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', description: '', visibility: 'PRIVATE' });
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value });

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    try {
      const deck = await deckApi.create({ ...form, description: form.description.trim() || null });
      navigate(`/app/decks/${deck.id}`);
    } catch (err) {
      const fieldErrors = err.fieldErrors();
      if (err.code === 'DECK_NAME_TAKEN') fieldErrors.name = `Bạn đã có deck tên "${form.name.trim()}". Hãy đặt tên khác.`;
      setErrors(Object.keys(fieldErrors).length ? fieldErrors : { form: err.message });
      setBusy(false);
    }
  }

  return (
    <form className="sheet" onSubmit={submit} noValidate>
      <h2>Tạo deck mới</h2>
      {errors.form && <p className="alert" role="alert">{errors.form}</p>}
      <Field label="Tên deck" required maxLength={100} autoFocus value={form.name} onChange={update('name')}
        placeholder="Ví dụ: Animals, Fruits, IELTS…" error={errors.name} />
      <Field label="Mô tả" as="textarea" maxLength={500} value={form.description} onChange={update('description')}
        hint="Không bắt buộc." error={errors.description} />
      <fieldset className="field" style={{ border: 0, padding: 0, margin: 'var(--space-4) 0 0' }}>
        <legend style={{ fontWeight: 800, marginBottom: 'var(--space-2)' }}>Ai được xem?</legend>
        <div className="choice-group">
          {['PRIVATE', 'PUBLIC'].map((value) => (
            <label key={value} className="choice">
              <input type="radio" name="visibility" value={value} checked={form.visibility === value} onChange={update('visibility')} />
              {VISIBILITY_LABEL[value]}
            </label>
          ))}
        </div>
      </fieldset>
      <div className="row" style={{ marginTop: 'var(--space-6)' }}>
        <button className="btn btn--primary" type="submit" disabled={busy}>{busy ? 'Đang tạo…' : 'Tạo deck'}</button>
        <button className="btn btn--ghost" type="button" onClick={onCancel}>Hủy</button>
      </div>
    </form>
  );
}

/** D8 · Thư viện (tab "Deck của tôi"; deck đã đăng ký đến ở M4). */
export default function LibraryPage() {
  const [params, setParams] = useSearchParams();
  const creating = params.get('new') === '1';
  const [state, setState] = useState({ loading: true });

  useEffect(() => {
    deckApi.listOwned().then((decks) => setState({ decks })).catch((error) => setState({ error }));
  }, []);

  const setCreating = (on) => setParams(on ? { new: '1' } : {});

  return (
    <div className="stack">
      <div className="page-head">
        <h1>Thư viện</h1>
        {!creating && <button className="btn btn--primary" type="button" onClick={() => setCreating(true)}>+ Tạo deck</button>}
      </div>

      {creating && <NewDeckForm onCancel={() => setCreating(false)} />}

      {state.loading && <Loading />}
      {state.error && <p className="alert" role="alert">{state.error.message}</p>}

      {state.decks?.length === 0 && !creating && (
        <div className="sheet center-state">
          <Mascot message="Your library is empty. Let's make a deck! ✏️" size={110} small />
          <h2>Bạn chưa tạo deck nào</h2>
          <p className="muted">Mỗi deck là một bộ thẻ theo chủ đề, ví dụ "Con vật" hay "Đồ ăn".</p>
          <button className="btn btn--primary" type="button" onClick={() => setCreating(true)}>+ Tạo deck</button>
        </div>
      )}

      {state.decks?.length > 0 && (
        <div className="deck-grid">
          {state.decks.map((deck) => (
            <Link key={deck.id} to={`/app/decks/${deck.id}`} className="deck-card">
              <h3>{deck.name}</h3>
              {deck.description && <p className="muted" style={{ margin: 0 }}>{deck.description}</p>}
              <div className="deck-card__meta">
                <span className="badge">🃏 {deck.cardCount} thẻ</span>
                <span className="badge">{VISIBILITY_LABEL[deck.visibility]}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
