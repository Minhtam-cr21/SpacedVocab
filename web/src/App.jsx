import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext.jsx';
import AdminPathPage from './pages/AdminPathPage.jsx';
import Layout from './components/Layout.jsx';
import Loading from './components/Loading.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import DeckDetailPage from './pages/DeckDetailPage.jsx';
import LibraryPage from './pages/LibraryPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import PathPage from './pages/PathPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import ReviewPage from './pages/ReviewPage.jsx';
import StagePage from './pages/StagePage.jsx';
import StageTestPage from './pages/StageTestPage.jsx';

/** Learner pages need a session; guest pages send signed-in users to /app (docs/design/03 routes). */
function RequireUser({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;
  return user ? children : <Navigate to="/login" replace />;
}

function RequireAdmin({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;
  if (!user) return <Navigate to="/login" replace />;
  return user.role === 'ADMIN' ? children : <NotFoundPage />;
}

function GuestOnly({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <Loading />;
  return user ? <Navigate to="/app" replace /> : children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/app" replace />} />
      <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><RegisterPage /></GuestOnly>} />
      <Route path="/app" element={<RequireUser><Layout /></RequireUser>}>
        <Route index element={<DashboardPage />} />
        <Route path="library" element={<LibraryPage />} />
        <Route path="decks/:deckId" element={<DeckDetailPage />} />
        <Route path="review" element={<ReviewPage />} />
        <Route path="path" element={<PathPage />} />
        <Route path="path/stages/:stageId" element={<StagePage />} />
        <Route path="path/stages/:stageId/test" element={<StageTestPage />} />
      </Route>
      <Route path="/admin" element={<RequireAdmin><Layout /></RequireAdmin>}>
        <Route index element={<Navigate to="/admin/path" replace />} />
        <Route path="path" element={<AdminPathPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
