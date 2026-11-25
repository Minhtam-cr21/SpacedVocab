import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { pathApi } from '../api/endpoints.js';
import Loading from '../components/Loading.jsx';
import Mascot from '../mascot/Robot.jsx';
import { formatDateTime } from '../util/format.js';
import { StageProgress } from './PathPage.jsx';

function lessonAction(lesson) {
  if (lesson.completed) return { label: 'Ôn lại', className: 'btn btn--small' };
  if (lesson.learnedCount > 0) return { label: 'Học tiếp ▶', className: 'btn btn--small btn--primary' };
  return { label: 'Bắt đầu ▶', className: 'btn btn--small btn--primary' };
}

/** D24 · Chi tiết chặng: lessons of the stage and the "skip this stage" test. */
export default function StagePage() {
  const { stageId } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const [busyLesson, setBusyLesson] = useState(null);
  const [confirmTest, setConfirmTest] = useState(false);

  useEffect(() => {
    pathApi.stage(stageId).then(setData).catch(setError);
  }, [stageId]);

  async function start(lesson) {
    setBusyLesson(lesson.id);
    try {
      const { deckId } = await pathApi.startLesson(lesson.id);
      navigate(`/app/review?deckId=${deckId}`);
    } catch (err) {
      setError(err);
      setBusyLesson(null);
    }
  }

  if (error?.status === 404) {
    return (
      <div className="sheet center-state">
        <Mascot message="Hmm… this stage is missing. 🔍" mood="oops" size={110} small />
        <Link className="btn" to="/app/path">Về lộ trình</Link>
      </div>
    );
  }
  if (!data) return error ? <p className="alert" role="alert">{error.message}</p> : <Loading />;

  const { stage, lessons } = data;

  return (
    <div className="stack">
      <p><Link to="/app/path">← Lộ trình</Link></p>

      <section className="sheet stage-head">
        <span className={`level-badge level-badge--${stage.cefrLevel} level-badge--big`}>{stage.cefrLevel}</span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <h1 style={{ margin: 0 }}>{stage.title}</h1>
          <StageProgress stage={stage} />
        </div>
      </section>

      {error && <p className="alert" role="alert">{error.message}</p>}

      {!stage.unlocked && (
        <p className="alert alert--info" role="status">
          🔒 Chặng này đang khóa. Hoàn thành chặng trước, hoặc làm bài vượt chặng ở chặng trước, để mở nhé!
        </p>
      )}

      {stage.testAvailable && (
        <section className="sheet skip-test">
          <h2>⚡ Nhảy chặng</h2>
          <p>Bạn đã biết hết các từ này? Làm <strong>bài vượt chặng</strong>: <strong>20 câu</strong>, nhìn nghĩa tiếng Việt rồi
            <strong> gõ từ tiếng Anh</strong>, trong <strong>10 phút</strong>. Đúng từ <strong>18 câu</strong> là mở chặng tiếp theo.</p>
          {!confirmTest && <button className="btn btn--primary" type="button" onClick={() => setConfirmTest(true)}>Làm bài vượt chặng</button>}
          {confirmTest && (
            <div className="row">
              <span className="muted">Trượt thì phải chờ 24 giờ mới làm lại. Sẵn sàng chưa?</span>
              <button className="btn btn--primary" type="button" onClick={() => navigate(`/app/path/stages/${stage.id}/test`)}>Bắt đầu ngay</button>
              <button className="btn btn--ghost" type="button" onClick={() => setConfirmTest(false)}>Để sau</button>
            </div>
          )}
        </section>
      )}

      {stage.testRetryAt && (
        <p className="alert alert--info" role="status">
          ⏳ Bạn làm lại bài vượt chặng được lúc <strong>{formatDateTime(stage.testRetryAt)}</strong>. Trong lúc chờ, học thêm vài bài nhé!
        </p>
      )}

      <section className="sheet">
        <h2>Các bài trong chặng ({lessons.length})</h2>
        {lessons.length === 0 && <p className="muted">Chặng này chưa có bài nào. Bạn quay lại sau nhé!</p>}
        <ol className="lesson-list">
          {lessons.map((lesson, i) => {
            const action = lessonAction(lesson);
            return (
              <li key={lesson.id} className={`lesson-row${lesson.completed ? ' is-done' : ''}`}>
                <span className="lesson-row__num" aria-hidden="true">{lesson.completed ? '✓' : i + 1}</span>
                <span className="lesson-row__body">
                  <span className="lesson-row__name">{lesson.name}</span>
                  <span className="muted">Đã học {lesson.learnedCount}/{lesson.cardCount} từ</span>
                </span>
                {stage.unlocked && (
                  <button className={action.className} type="button" disabled={busyLesson !== null} onClick={() => start(lesson)}>
                    {busyLesson === lesson.id ? 'Đang mở…' : action.label}
                  </button>
                )}
              </li>
            );
          })}
        </ol>
      </section>
    </div>
  );
}
