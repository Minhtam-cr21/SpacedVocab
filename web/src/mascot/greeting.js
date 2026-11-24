/**
 * What the robot says depending on the learner's local time. The mascot always speaks English
 * (the rest of the UI is Vietnamese) so learners read a little English every visit.
 */
export function greetingFor(date = new Date()) {
  const hour = date.getHours();
  if (hour >= 5 && hour < 11) {
    return { period: 'morning', mood: 'happy', text: 'Good morning! ☀️ Ready to learn some new words?' };
  }
  if (hour >= 11 && hour < 14) {
    return { period: 'noon', mood: 'wow', text: 'Hi there! 🍜 What did you have for lunch?' };
  }
  if (hour >= 14 && hour < 18) {
    return { period: 'afternoon', mood: 'happy', text: "Good afternoon! 🌤️ Let's review a few cards." };
  }
  return { period: 'night', mood: 'sleepy', text: 'How are you? 🌙 Are you sleepy yet?' };
}
