import { useEffect, useState } from 'react';
import { greetingFor } from './greeting.js';

const SCREEN_LIGHT = '#ffe3a3';

/** Eyes and mouth for each mood, drawn on the robot's face screen (viewBox 0 0 120 140). */
function Face({ mood }) {
  switch (mood) {
    case 'sleepy':
      return (
        <g stroke={SCREEN_LIGHT} strokeWidth="3" strokeLinecap="round" fill="none">
          <path d="M41 49q6 4 12 0" />
          <path d="M67 49q6 4 12 0" />
          <circle cx="60" cy="60" r="2.5" fill={SCREEN_LIGHT} />
        </g>
      );
    case 'wow':
      return (
        <g fill={SCREEN_LIGHT}>
          <circle className="robot__eye" cx="47" cy="47" r="6.5" />
          <circle className="robot__eye" cx="73" cy="47" r="6.5" />
          <circle cx="60" cy="61" r="4" />
        </g>
      );
    case 'oops':
      return (
        <g stroke={SCREEN_LIGHT} strokeWidth="3" strokeLinecap="round" fill="none">
          <path d="M42 43l9 7M51 43l-9 7" />
          <path d="M69 43l9 7M78 43l-9 7" />
          <path d="M51 61q4.5-4 9 0t9 0" />
        </g>
      );
    case 'proud':
      return (
        <g stroke={SCREEN_LIGHT} strokeWidth="3" strokeLinecap="round" fill="none">
          <path d="M41 49q6-7 12 0" />
          <path d="M67 49q6-7 12 0" />
          <path d="M50 57q10 9 20 0" />
        </g>
      );
    default: // happy
      return (
        <g>
          <ellipse className="robot__eye" cx="47" cy="47" rx="5" ry="6" fill={SCREEN_LIGHT} />
          <ellipse className="robot__eye" cx="73" cy="47" rx="5" ry="6" fill={SCREEN_LIGHT} />
          <path d="M51 57q9 7 18 0" stroke={SCREEN_LIGHT} strokeWidth="3" strokeLinecap="round" fill="none" />
        </g>
      );
  }
}

/** The SpacedVocab robot — an original character: cream body, boba-brown face screen, caramel antenna. */
export function RobotSvg({ mood = 'happy', size = 140, title = 'Robot SpacedVocab' }) {
  const ink = 'var(--ink)';
  return (
    <svg className="robot" width={size} height={size * (140 / 120)} viewBox="0 0 120 140" role="img" aria-label={title}>
      <g className="robot__float">
        {/* antenna */}
        <path d="M60 22V10" stroke={ink} strokeWidth="3" strokeLinecap="round" />
        <circle className="robot__antenna-light" cx="60" cy="8" r="5.5" fill="var(--caramel)" stroke={ink} strokeWidth="2.5" />
        {/* ears */}
        <rect x="11" y="40" width="10" height="20" rx="4" fill="var(--sky)" stroke={ink} strokeWidth="2.5" />
        <rect x="99" y="40" width="10" height="20" rx="4" fill="var(--sky)" stroke={ink} strokeWidth="2.5" />
        {/* head */}
        <rect x="20" y="22" width="80" height="56" rx="20" fill="var(--paper)" stroke={ink} strokeWidth="3" />
        <rect x="29" y="31" width="62" height="38" rx="13" fill="var(--boba)" />
        <circle cx="38" cy="60" r="4" fill="var(--pink)" opacity=".85" />
        <circle cx="82" cy="60" r="4" fill="var(--pink)" opacity=".85" />
        <Face mood={mood} />
        {/* neck + body */}
        <rect x="52" y="77" width="16" height="7" fill="var(--caramel)" stroke={ink} strokeWidth="2.5" />
        <rect x="33" y="83" width="54" height="38" rx="14" fill="var(--paper)" stroke={ink} strokeWidth="3" />
        <path d="M60 107l-6.5-6.5a4.3 4.3 0 0 1 6.5-5.6 4.3 4.3 0 0 1 6.5 5.6z" fill="var(--coral)" stroke={ink} strokeWidth="2" />
        <circle cx="44" cy="93" r="2.5" fill="var(--matcha)" stroke={ink} strokeWidth="1.5" />
        <circle cx="76" cy="93" r="2.5" fill="var(--honey)" stroke={ink} strokeWidth="1.5" />
        {/* arms: the right one waves */}
        <rect x="19" y="88" width="13" height="25" rx="6.5" fill="var(--honey)" stroke={ink} strokeWidth="2.5" transform="rotate(12 25 90)" />
        {/* the rotation lives on the <g> so the CSS wave animation does not override it */}
        <g transform="rotate(-150 94 84)">
          <rect className="robot__arm--wave" x="88" y="80" width="13" height="25" rx="6.5" fill="var(--honey)" stroke={ink} strokeWidth="2.5" />
        </g>
        {/* wheels */}
        <rect x="38" y="120" width="16" height="11" rx="5" fill="var(--boba)" stroke={ink} strokeWidth="2.5" />
        <rect x="66" y="120" width="16" height="11" rx="5" fill="var(--boba)" stroke={ink} strokeWidth="2.5" />
        {mood === 'sleepy' && (
          <text className="robot__zzz" x="98" y="20" fontFamily="var(--font-display)" fontWeight="800" fontSize="16" fill={ink}>
            z<tspan fontSize="12" dy="-6">z</tspan>
          </text>
        )}
      </g>
    </svg>
  );
}

/**
 * Robot with a speech bubble above its head. Without `message` it greets by the time of day
 * and refreshes every minute, so the greeting changes from morning to night on its own.
 */
export default function Mascot({ message, mood, size = 140, small = false }) {
  const [greeting, setGreeting] = useState(() => greetingFor());

  useEffect(() => {
    if (message) return undefined;
    const timer = setInterval(() => setGreeting(greetingFor()), 60_000);
    return () => clearInterval(timer);
  }, [message]);

  const text = message ?? greeting.text;
  const face = mood ?? (message ? 'happy' : greeting.mood);

  return (
    <div className={`mascot mascot--${face}${small ? ' mascot--small' : ''}`}>
      <p className="mascot__bubble" lang="en" key={text} aria-live="polite">{text}</p>
      <RobotSvg mood={face} size={size} />
    </div>
  );
}
