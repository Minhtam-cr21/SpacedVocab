import { Link } from 'react-router-dom';
import Mascot from '../mascot/Robot.jsx';

/** D22 · Không tìm thấy trang */
export default function NotFoundPage() {
  return (
    <div className="auth">
      <div className="sheet center-state auth__card">
        <Mascot message="Beep boop… I can't find this page! 🔍" mood="oops" size={130} small />
        <h1>Không tìm thấy trang</h1>
        <p className="muted">Đường dẫn có thể đã sai hoặc trang đã bị xóa.</p>
        <Link className="btn btn--primary" to="/">Về trang chủ</Link>
      </div>
    </div>
  );
}
