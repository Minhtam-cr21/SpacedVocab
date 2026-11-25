import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { pathApi } from '../api/endpoints.js';
import Loading from '../components/Loading.jsx';
import Mascot from '../mascot/Robot.jsx';

/** Progress bar + "n/N bài" of one stage. */
export function StageProgress({ stage }) {
  const percent = stage.lessonCount ? Math.round((stage.completedLessonCount / stage.lessonCount) * 100) : 0;
  return (
    <>
      <div className="stage-card__count">{stage.completedLessonCount}/{stage.lessonCount} bài đã hoàn thành</div>
      <div className="progress progress--thin" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={percent}
        aria-label={`Tiến độ chặng ${stage.cefrLevel}`}>
        <div className="progress__bar" style={{ width: `${percent}%` }} />
      </div>
    </>
  );
}

export const isStageDone = (stage) => stage.lessonCount > 0 && stage.completedLessonCount === stage.lessonCount;

/** The stage the learner is on: the highest open one. */
export function currentStage(stages) {
  const open = stages.filter((s) => s.unlocked);
  return open[open.length - 1];
}

function stageStatus(stage, current, next) {
  if (!stage.unlocked) return { label: 'Khóa', icon: '🔒', className: 'is-locked' };
  if (next?.unlockMethod === 'TEST') return { label: 'Đã vượt bằng bài kiểm tra', icon: '🏅', className: 'is-done' };
  if (isStageDone(stage) && stage !== current) return { label: 'Đã xong', icon: '✓', className: 'is-done' };
  if (stage === current) return { label: 'Đang học', icon: '→', className: 'is-current' };
  return { label: 'Đã mở', icon: '→', className: '' };
}

/** D23 · Lộ trình CEFR: six stages from A1 to C2, locked until the previous one is completed or skipped. */
export default function PathPage() {
  const [path, setPath] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    pathApi.list()
      .then((paths) => (paths.length ? pathApi.get(paths[0].id) : null))
      .then((data) => setPath(data ?? { stages: [] }))
      .catch(setError);
  }, []);

  if (error) return <p className="alert" role="alert">{error.message}</p>;
  if (!path) return <Loading />;
  if (path.stages.length === 0) {
    return (
      <div className="sheet center-state">
        <Mascot message="The path is coming soon! 🛠️" mood="sleepy" size={110} small />
        <p>Lộ trình đang được chuẩn bị. Bạn quay lại sau nhé!</p>
      </div>
    );
  }

  const current = currentStage(path.stages);
  const cheer = current?.unlockMethod ? `Level up! ${current.cefrLevel} unlocked! 🎉` : `Let's go, ${current?.cefrLevel}! 🚀`;

  return (
    <div className="stack path">
      <section className="sheet path__head">
        <div>
          <p className="path__eyebrow">{path.name}</p>
          <h1>Chọn chặng học</h1>
          <p className="muted" style={{ margin: 0 }}>Sáu bậc từ A1 đến C2. Xong hết bài của một chặng, hoặc làm bài vượt chặng, để mở chặng tiếp theo.</p>
        </div>
        <Mascot message={cheer} mood="proud" size={100} small />
      </section>

      <ol className="stage-list">
        {path.stages.map((stage, i) => {
          const status = stageStatus(stage, current, path.stages[i + 1]);
          return (
            <li key={stage.id}>
              <Link to={`/app/path/stages/${stage.id}`} className={`stage-card ${status.className}`}
                aria-label={`Chặng ${stage.cefrLevel}, ${stage.title}, ${status.label}, ${stage.completedLessonCount} trên ${stage.lessonCount} bài`}>
                <span className={`level-badge level-badge--${stage.cefrLevel}`}>{stage.cefrLevel}</span>
                <span className="stage-card__body">
                  <span className="stage-card__title">{stage.title}</span>
                  <StageProgress stage={stage} />
                </span>
                <span className="stage-card__status" title={status.label} aria-hidden="true">{status.icon}</span>
              </Link>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
