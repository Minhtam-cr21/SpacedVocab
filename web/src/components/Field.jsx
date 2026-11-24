import { useId } from 'react';

/** Labelled input with hint and error, wired for screen readers. Pass `as="textarea"` for multi-line. */
export default function Field({ label, hint, error, as = 'input', ...inputProps }) {
  const id = useId();
  const Control = as;
  const describedBy = [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(' ') || undefined;
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <Control
        id={id}
        className={as === 'textarea' ? 'textarea' : 'input'}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={describedBy}
        {...inputProps}
      />
      {hint && <span id={`${id}-hint`} className="hint">{hint}</span>}
      {error && <span id={`${id}-error`} className="field-error" role="alert">{error}</span>}
    </div>
  );
}
