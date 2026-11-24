/** Reads an English word aloud with the browser's built-in speech engine (no server, no key). */
const canSpeak = typeof window !== 'undefined' && 'speechSynthesis' in window;

export function speak(text) {
  if (!canSpeak || !text) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'en-US';
  utterance.rate = 0.85;
  window.speechSynthesis.speak(utterance);
}

export default function SpeakButton({ text }) {
  if (!canSpeak) return null;
  return (
    <button
      type="button"
      className="icon-btn"
      aria-label={`Nghe phát âm "${text}"`}
      title="Nghe phát âm"
      onClick={(event) => {
        event.stopPropagation();
        speak(text);
      }}
    >
      🔊
    </button>
  );
}
