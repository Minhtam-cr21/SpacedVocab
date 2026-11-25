import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { RobotSvg } from '../mascot/Robot.jsx';

const NAV = [
  { to: '/app', label: 'Trang chủ', icon: '🏠', end: true },
  { to: '/app/path', label: 'Lộ trình', icon: '🗺️' },
  { to: '/app/library', label: 'Thư viện', icon: '📚' },
  { to: '/app/review', label: 'Ôn tập', icon: '🃏' },
];

/** Learner shell: top bar (desktop nav) + bottom nav on phones. */
export default function Layout() {
  const { user, logout } = useAuth();
  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar__inner">
          <Link to="/app" className="brand" aria-label="SpacedVocab — Trang chủ">
            <RobotSvg size={36} title="" />
            SpacedVocab
          </Link>
          <nav className="topnav" aria-label="Điều hướng chính">
            {NAV.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} className="navlink">
                <span aria-hidden="true">{item.icon}</span> {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="topbar__user">
            <span className="topbar__name">{user?.displayName}</span>
            <button type="button" className="btn btn--small" onClick={logout}>Đăng xuất</button>
          </div>
        </div>
      </header>

      <main className="main" id="main">
        <Outlet />
      </main>

      <nav className="bottomnav" aria-label="Điều hướng chính">
        {NAV.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.end}>
            <span className="nav-icon" aria-hidden="true">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
