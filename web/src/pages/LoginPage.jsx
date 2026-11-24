import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import Field from '../components/Field.jsx';
import Mascot from '../mascot/Robot.jsx';

/** D3 · Đăng nhập */
export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value });

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(form.email, form.password);
      navigate('/app', { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  const fieldErrors = error?.fieldErrors?.() ?? {};

  return (
    <div className="auth">
      <div className="auth__card">
        <div className="auth__mascot">
          <Mascot message={error ? 'Hmm… let’s try again! 🤔' : 'Welcome back, friend! 👋'} mood={error ? 'oops' : 'happy'} size={120} small />
        </div>
        <form className="sheet" onSubmit={submit} noValidate>
          <h1 className="auth__title">Đăng nhập</h1>
          {error && !error.errors.length && <p className="alert" role="alert">{error.message}</p>}
          <Field label="Email" type="email" autoComplete="email" required value={form.email} onChange={update('email')} error={fieldErrors.email} />
          <Field label="Mật khẩu" type="password" autoComplete="current-password" required value={form.password} onChange={update('password')} error={fieldErrors.password} />
          <button className="btn btn--primary btn--block btn--big" type="submit" disabled={busy} style={{ marginTop: 'var(--space-6)' }}>
            {busy ? 'Đang đăng nhập…' : 'Vào học thôi! 🚀'}
          </button>
          <p className="auth__foot">Chưa có tài khoản? <Link to="/register">Đăng ký miễn phí</Link></p>
        </form>
      </div>
    </div>
  );
}
