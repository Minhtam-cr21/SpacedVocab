import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { pathApi } from '../api/endpoints.js';
import Loading from '../components/Loading.jsx';
import Mascot from '../mascot/Robot.jsx';

const draftKey = (attemptId) => `spacedvocab.stageTest.${attemptId}`;

function loadDraft(attemptId) {
  try {
    return JSON.parse(sessionStorage.getItem(draftKey(attemptId))) ?? {};
  } catch {
    return {};
  }
}

function saveDraft(attemptId, answers) {
  try {
    sessionStorage.setItem(draftKey(attemptId), JSON.stringify(answers));
  } catch {
    // storage blocked: answers then live only in memory
  }
}

function clearDraft(attemptId) {
  try {
    sessionStorage.removeItem(draftKey(attemptId));
  } catch {
    // ignore
  }
}

const formatClock = (ms) => {
  const total = Math.max(0, Math.ceil(ms / 1000));
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`;
};

/** D25 result: score, cheer from Bit, and the questions that were wrong. */
function TestResult({ result, stageId }) {
  const wrong = result.questions.filter((q) => !q.correct);
  return (
    <div className="review stack">
      <section className="sheet center-state">
        {result.passed
          ? <Mascot message="Level up! You did it! 🏆" mood="proud" size={130} />
          : <Mascot message="So close! Practice and try again tomorrow. 💛" mood="oops" size={130} />}
        <div className="score" aria-label={`Đúng ${result.correctCount} trên ${result.total} câu`}>
          <span className="score__num">{result.correctCount}</span>/{result.total}
        </div>
        {result.passed
          ? <p><strong>Chúc mừng!</strong> Bạn đã mở chặng tiếp theo.</p>
          : <p>Cần đúng ít nhất <strong>18 câu</strong>. Bạn làm lại được sau <strong>24 giờ</strong>.</p>}
        <div className="row" style={{ justifyContent: 'center' }}>
          <Link className="btn btn--primary btn--big" to="/app/path">{result.passed ? 'Tới chặng tiếp theo ▶' : 'Về lộ trình'}</Link>
          {!result.passed && <Link className="btn" to={`/app/path/stages/${stageId}`}>Học tiếp chặng này</Link>}
        </div>
      </section>

      {wrong.length > 0 && (
        <section className="sheet">
          <h2>Các câu chưa đúng ({wrong.length})</h2>
          <ul className="card-list">
            {wrong.map((q) => (
              <li key={q.position} className="card-item">
                <div className="card-item__body">
                  <div>{q.meaning} {q.partOfSpeech && <span className="badge" lang="en">{q.partOfSpeech}</span>}</div>
                  <div className="muted">Bạn gõ: <span lang="en">{q.answer ?? '(bỏ trống)'}</span></div>
                  <div>Đáp án: <span className="card-item__word" lang="en">{q.correctWord}</span></div>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}

/** D25 · Bài vượt chặng: 20 questions "Vietnamese meaning → type the English word", 10 minutes (R12). */
export default function StageTestPage() {
  const { stageId } = useParams();
  const navigate = useNavigate();
  const [test, setTest] = useState(null);
  const [answers, setAnswers] = useState({});
  const [index, setIndex] = useState(0);
  const [now, setNow] = useState(Date.now());
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [confirmExit, setConfirmExit] = useState(false);
  const inputRef = useRef(null);
  const submittedRef = useRef(false);

  useEffect(() => {
    pathApi.startTest(stageId)
      .then((data) => {
        setTest(data);
        setAnswers(loadDraft(data.attemptId));
      })
      .catch(setError);
  }, [stageId]);

  const submit = useCallback(async () => {
    if (!test || submittedRef.current) return;
    submittedRef.current = true;
    setSubmitting(true);
    setError(null);
    try {
      const payload = test.questions.map((q) => ({ position: q.position, answer: answers[q.position] ?? null }));
      const data = await pathApi.submitTest(test.attemptId, payload);
      clearDraft(test.attemptId);
      setResult(data);
    } catch (err) {
      submittedRef.current = false;
      setError(err.status === 0 ? { message: 'Chưa nộp được, kiểm tra mạng rồi thử lại.' } : err);
    } finally {
      setSubmitting(false);
    }
  }, [test, answers]);

  const remaining = test ? new Date(test.expiresAt).getTime() - now : 0;

  useEffect(() => {
    if (!test || result) return undefined;
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [test, result]);

  useEffect(() => {
    if (test && !result && remaining <= 0) submit(); // time is up: hand in what we have
  }, [test, result, remaining, submit]);

  useEffect(() => {
    inputRef.current?.focus();
  }, [index, test]);

  if (result) return <TestResult result={result} stageId={stageId} />;
  if (!test) {
    if (!error) return <Loading message="Preparing your test… 📝" />;
    return (
      <div className="sheet center-state">
        <Mascot message="Oops! Not now. 🙈" mood="oops" size={110} small />
        <p>{error.message}</p>
        <Link className="btn" to={`/app/path/stages/${stageId}`}>Quay lại chặng</Link>
      </div>
    );
  }

  const question = test.questions[index];
  const isLast = index === test.questions.length - 1;
  const answeredCount = test.questions.filter((q) => (answers[q.position] ?? '').trim()).length;

  function setAnswer(value) {
    const next = { ...answers, [question.position]: value };
    setAnswers(next);
    saveDraft(test.attemptId, next);
  }

  function goNext(event) {
    event.preventDefault();
    if (isLast) submit();
    else setIndex(index + 1);
  }

  return (
    <div className="review">
      <div className="review__top">
        {!confirmExit && (
          <button className="icon-btn" type="button" aria-label="Thoát bài" onClick={() => setConfirmExit(true)}>✕</button>
        )}
        <div className="progress" role="progressbar" aria-valuemin={0} aria-valuemax={test.questions.length}
          aria-valuenow={index + 1} aria-label="Tiến độ bài">
          <div className="progress__bar" style={{ width: `${((index + 1) / test.questions.length) * 100}%` }} />
        </div>
        <span className="badge">{index + 1}/{test.questions.length}</span>
        <span className={`timer${remaining < 60000 ? ' timer--low' : ''}`} role="timer" aria-label="Thời gian còn lại">
          ⏱ {formatClock(remaining)}
        </span>
      </div>

      {confirmExit && (
        <div className="alert alert--info row" role="alertdialog" aria-label="Xác nhận thoát">
          <span>Đồng hồ vẫn chạy khi bạn thoát. Hết giờ mà chưa nộp thì tính là trượt.</span>
          <button className="btn btn--small btn--danger" type="button" onClick={() => navigate(`/app/path/stages/${stageId}`)}>Vẫn thoát</button>
          <button className="btn btn--small" type="button" onClick={() => setConfirmExit(false)}>Làm tiếp</button>
        </div>
      )}

      {error && <p className="alert" role="alert">{error.message}</p>}

      <form className="sheet test-question" onSubmit={goNext}>
        <p className="muted" style={{ margin: 0 }}>Gõ từ tiếng Anh có nghĩa là:</p>
        <p className="test-question__meaning" aria-live="polite">{question.meaning}</p>
        {question.partOfSpeech && <span className="badge" lang="en">{question.partOfSpeech}</span>}
        <label className="visually-hidden" htmlFor="answer">Câu trả lời</label>
        <input id="answer" ref={inputRef} className="input test-question__input" lang="en" maxLength={100}
          autoComplete="off" autoCorrect="off" autoCapitalize="none" spellCheck={false}
          value={answers[question.position] ?? ''} onChange={(e) => setAnswer(e.target.value)} placeholder="Type here…" />
        <div className="row" style={{ justifyContent: 'space-between', marginTop: 'var(--space-5)' }}>
          <button className="btn" type="button" disabled={index === 0} onClick={() => setIndex(index - 1)}>← Câu trước</button>
          {!isLast && (
            <button className="btn btn--ghost" type="button" onClick={() => setIndex(index + 1)}>Bỏ qua</button>
          )}
          <button className="btn btn--primary" type="submit" disabled={submitting}>
            {isLast ? (submitting ? 'Đang nộp…' : `Nộp bài (${answeredCount}/${test.questions.length})`) : 'Tiếp →'}
          </button>
        </div>
      </form>

      <nav className="test-dots" aria-label="Chọn câu">
        {test.questions.map((q, i) => (
          <button key={q.position} type="button" aria-label={`Câu ${i + 1}`} aria-current={i === index ? 'step' : undefined}
            className={`test-dot${(answers[q.position] ?? '').trim() ? ' is-answered' : ''}${i === index ? ' is-current' : ''}`}
            onClick={() => setIndex(i)}>
            {i + 1}
          </button>
        ))}
      </nav>
      {!isLast && (
        <p className="center-state" style={{ padding: 'var(--space-4) 0' }}>
          <button className="btn btn--small" type="button" disabled={submitting} onClick={submit}>Nộp bài sớm ({answeredCount}/{test.questions.length})</button>
        </p>
      )}
    </div>
  );
}
