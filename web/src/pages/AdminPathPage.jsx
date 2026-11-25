import { useCallback, useEffect, useState } from 'react';
import { adminPathApi, pathApi } from '../api/endpoints.js';
import ConfirmButton from '../components/ConfirmButton.jsx';
import Loading from '../components/Loading.jsx';

/** R12: a stage needs at least this many words for its stage test. */
const TEST_MIN_WORDS = 20;

/** One stage: rename, reorder / remove lessons, add an OFFICIAL deck that is not a lesson yet. */
function StageEditor({ stage, lessons, freeDecks, onChanged, onError }) {
  const [title, setTitle] = useState(stage.title);
  const [deckId, setDeckId] = useState('');
  const [busy, setBusy] = useState(false);
  const words = lessons.reduce((sum, l) => sum + l.cardCount, 0);

  useEffect(() => setTitle(stage.title), [stage.title]);

  async function run(action) {
    setBusy(true);
    try {
      await action();
      await onChanged();
    } catch (err) {
      onError(err);
    } finally {
      setBusy(false);
    }
  }

  /** Swaps two lessons, then renumbers the stage 1..n so positions stay clean. */
  const move = (index, offset) => run(async () => {
    const order = [...lessons];
    [order[index], order[index + offset]] = [order[index + offset], order[index]];
    for (let i = 0; i < order.length; i += 1) {
      if (order[i].position !== i + 1) await adminPathApi.moveLesson(order[i].id, i + 1);
    }
  });

  const rename = (event) => {
    event.preventDefault();
    run(() => adminPathApi.renameStage(stage.id, title.trim()));
  };

  const add = (event) => {
    event.preventDefault();
    if (!deckId) return;
    run(async () => {
      await adminPathApi.addLesson(stage.id, Number(deckId), lessons.length + 1);
      setDeckId('');
    });
  };

  return (
    <section className="sheet admin-stage" aria-labelledby={`stage-${stage.id}`}>
      <div className="stage-head">
        <span className={`level-badge level-badge--${stage.cefrLevel}`}>{stage.cefrLevel}</span>
        <form className="admin-stage__title" onSubmit={rename}>
          <label className="visually-hidden" htmlFor={`stage-${stage.id}`}>Tên chặng {stage.cefrLevel}</label>
          <input id={`stage-${stage.id}`} className="input" maxLength={100} value={title} onChange={(e) => setTitle(e.target.value)} />
          <button className="btn btn--small" type="submit" disabled={busy || !title.trim() || title.trim() === stage.title}>Lưu tên</button>
        </form>
      </div>

      <p className="muted" style={{ margin: 'var(--space-3) 0' }}>
        {lessons.length} bài · {words} từ
        {words < TEST_MIN_WORDS && <span className="admin-warn"> · ⚠️ Chưa đủ {TEST_MIN_WORDS} từ cho bài vượt chặng</span>}
      </p>

      {lessons.length === 0 && <p className="muted">Chặng chưa có bài nào.</p>}
      <ol className="lesson-list">
        {lessons.map((lesson, i) => (
          <li key={lesson.id} className="lesson-row">
            <span className="lesson-row__num" aria-hidden="true">{i + 1}</span>
            <span className="lesson-row__body">
              <span className="lesson-row__name">{lesson.name}</span>
              <span className="muted">{lesson.cardCount} từ</span>
            </span>
            <span className="row admin-stage__actions">
              <button className="icon-btn" type="button" aria-label={`Đưa "${lesson.name}" lên`} disabled={busy || i === 0}
                onClick={() => move(i, -1)}>↑</button>
              <button className="icon-btn" type="button" aria-label={`Đưa "${lesson.name}" xuống`} disabled={busy || i === lessons.length - 1}
                onClick={() => move(i, 1)}>↓</button>
              <ConfirmButton confirmLabel="Chắc chắn gỡ?" onConfirm={() => run(() => adminPathApi.removeLesson(lesson.id))}>Gỡ</ConfirmButton>
            </span>
          </li>
        ))}
      </ol>

      <form className="row admin-stage__add" onSubmit={add}>
        <label className="visually-hidden" htmlFor={`add-${stage.id}`}>Deck để thêm vào chặng {stage.cefrLevel}</label>
        <select id={`add-${stage.id}`} className="select" value={deckId} onChange={(e) => setDeckId(e.target.value)} disabled={busy}>
          <option value="">{freeDecks.length ? 'Chọn deck chính thức…' : 'Không còn deck chính thức trống'}</option>
          {freeDecks.map((deck) => <option key={deck.id} value={deck.id}>{deck.name} ({deck.cardCount} từ)</option>)}
        </select>
        <button className="btn btn--matcha" type="submit" disabled={busy || !deckId}>+ Thêm bài</button>
      </form>
    </section>
  );
}

/** D26 · Quản lý lộ trình (Admin): stages, their lessons and which OFFICIAL decks are lessons (R10). */
export default function AdminPathPage() {
  const [path, setPath] = useState(null);
  const [lessonsByStage, setLessonsByStage] = useState({});
  const [decks, setDecks] = useState([]);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    const paths = await pathApi.list();
    if (!paths.length) {
      setPath({ stages: [] });
      return;
    }
    const [detail, officialDecks] = await Promise.all([pathApi.get(paths[0].id), adminPathApi.officialDecks()]);
    const stages = await Promise.all(detail.stages.map((s) => pathApi.stage(s.id)));
    setLessonsByStage(Object.fromEntries(stages.map((s) => [s.stage.id, s.lessons])));
    setDecks(officialDecks);
    setPath(detail);
    setError(null);
  }, []);

  useEffect(() => {
    load().catch(setError);
  }, [load]);

  if (!path) return error ? <p className="alert" role="alert">{error.message}</p> : <Loading />;

  const freeDecks = decks.filter((d) => d.lessonId == null);

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Quản lý lộ trình</h1>
          <p className="muted" style={{ margin: 0 }}>
            Mỗi bài là một deck chính thức, một deck chỉ thuộc một bài. Gỡ bài không khóa lại chặng người học đã mở.
          </p>
        </div>
      </div>

      {error && <p className="alert" role="alert">{error.message}</p>}
      <p className="alert alert--info" role="note">
        Còn <strong>{freeDecks.length}</strong> deck chính thức chưa gắn vào bài. Tạo deck chính thức mới trên web sẽ có ở M4;
        hiện nội dung được nạp từ <code>seed/cefr/*.txt</code>.
      </p>

      {path.stages.map((stage) => (
        <StageEditor key={stage.id} stage={stage} lessons={lessonsByStage[stage.id] ?? []} freeDecks={freeDecks}
          onChanged={load} onError={setError} />
      ))}
    </div>
  );
}
