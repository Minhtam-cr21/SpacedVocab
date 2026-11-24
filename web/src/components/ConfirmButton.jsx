import { useEffect, useState } from 'react';

/** Two-step destructive button: the first click asks "Chắc chắn?", the second one runs `onConfirm`. */
export default function ConfirmButton({ children, confirmLabel = 'Chắc chắn xóa?', onConfirm, className = 'btn btn--small' }) {
  const [armed, setArmed] = useState(false);

  useEffect(() => {
    if (!armed) return undefined;
    const timer = setTimeout(() => setArmed(false), 4000);
    return () => clearTimeout(timer);
  }, [armed]);

  return (
    <button
      type="button"
      className={armed ? `${className} btn--danger` : className}
      onClick={() => (armed ? onConfirm() : setArmed(true))}
    >
      {armed ? confirmLabel : children}
    </button>
  );
}
