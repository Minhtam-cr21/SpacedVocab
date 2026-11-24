import Mascot from '../mascot/Robot.jsx';

export default function Loading({ message = 'Loading… ⏳' }) {
  return (
    <div className="center-state" role="status">
      <Mascot message={message} mood="wow" size={110} small />
    </div>
  );
}
