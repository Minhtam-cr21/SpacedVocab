import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import Field from '../components/Field.jsx';
import Mascot from '../mascot/Robot.jsx';

/** D2 · Đăng ký */
export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ displayName: '', email: '', password: '', confirm: '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState(null);
  const [busy, setBusy] = useState(false);

  const update = (key) => (event) => setForm({ ...form, [key]: event.target.value });

  async function submit(event) {
    event.preventDefault();
    setMessage(null);
    if (form.password !== form.confirm) {
      setErrors({ confirm: 'Mật khẩu nhập lại chưa khớp.' });
      return;
    }
    setErrors({});
    setBusy(true);
    try {
      await register({
        email: form.email,
        password: form.password,
        displayName: form.displayName.trim() || undefined,
      });
      navigate('/app', { replace: true });
    } catch (err) {
      const fieldErrors = err.fieldErrors();
      if (err.code === 'EMAIL_TAKEN') fieldErrors.email = err.message;
      setErrors(fieldErrors);
      if (!Object.keys(fieldErrors).length) setMessage(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth">
      <div className="auth__card">
        <div className="auth__mascot">
          <Mascot message="Hi! I'm Bit. Let's learn English together! 🤖" size={120} small />
        </div>
        <form className="sheet" onSubmit={submit} noValidate>
          <h1 className="auth__title">Tạo tài khoản</h1>
          {message && <p className="alert" role="alert">{message}</p>}
          <Field label="Tên của bạn" autoComplete="nickname" maxLength={50} value={form.displayName} onChange={update('displayName')}
            hint="Không bắt buộc. Robot sẽ gọi bạn bằng tên này." error={errors.displayName} />
          <Field label="Email" type="email" autoComplete="email" required value={form.email} onChange={update('email')} error={errors.email} />
          <Field label="Mật khẩu" type="password" autoComplete="new-password" required value={form.password} onChange={update('password')}
            hint="Ít nhất 8 ký tự." error={errors.password} />
          <Field label="Nhập lại mật khẩu" type="password" autoComplete="new-password" required value={form.confirm} onChange={update('confirm')} error={errors.confirm} />
          <button className="btn btn--primary btn--block btn--big" type="submit" disabled={busy} style={{ marginTop: 'var(--space-6)' }}>
            {busy ? 'Đang tạo…' : 'Bắt đầu học 🎒'}
          </button>
          <p className="auth__foot">Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
        </form>
      </div>
    </div>
  );
}
