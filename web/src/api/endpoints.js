import { api } from './client.js';

/** One function per backend endpoint (see docs/design/03-information-architecture.md). */
const json = (method, body) => ({ method, body });

export const authApi = {
  register: (body) => api('/api/auth/register', { ...json('POST', body), auth: false }),
  login: (body) => api('/api/auth/login', { ...json('POST', body), auth: false }),
  logout: (refreshToken) => api('/api/auth/logout', { ...json('POST', { refreshToken }), auth: false }),
};

export const meApi = {
  get: () => api('/api/me'),
};

export const deckApi = {
  listOwned: () => api('/api/decks?scope=owned'),
  get: (id) => api(`/api/decks/${id}`),
  create: (body) => api('/api/decks', json('POST', body)),
  remove: (id) => api(`/api/decks/${id}`, { method: 'DELETE' }),
};

export const cardApi = {
  search: (deckId, { q = '', page = 0, size = 20 } = {}) =>
    api(`/api/decks/${deckId}/cards?${new URLSearchParams({ q, page, size })}`),
  create: (deckId, body) => api(`/api/decks/${deckId}/cards`, json('POST', body)),
  remove: (deckId, cardId) => api(`/api/decks/${deckId}/cards/${cardId}`, { method: 'DELETE' }),
};

export const reviewApi = {
  due: ({ deckId, limit = 50 } = {}) => {
    const params = new URLSearchParams({ limit });
    if (deckId) params.set('deckId', deckId);
    return api(`/api/reviews/due?${params}`);
  },
  grade: (cardId, quality) => api('/api/reviews', json('POST', { cardId, quality })),
};

export const pathApi = {
  list: () => api('/api/paths'),
  get: (id) => api(`/api/paths/${id}`),
  stage: (id) => api(`/api/stages/${id}`),
  startLesson: (lessonId) => api(`/api/lessons/${lessonId}/start`, json('POST', {})),
  startTest: (stageId) => api(`/api/stages/${stageId}/test-attempts`, json('POST', {})),
  submitTest: (attemptId, answers) => api(`/api/stage-test-attempts/${attemptId}/submit`, json('POST', { answers })),
};

export const adminPathApi = {
  officialDecks: () => api('/api/admin/decks'),
  renameStage: (stageId, title) => api(`/api/admin/stages/${stageId}`, json('PUT', { title })),
  addLesson: (stageId, deckId, position) => api(`/api/admin/stages/${stageId}/lessons`, json('POST', { deckId, position })),
  moveLesson: (lessonId, position) => api(`/api/admin/lessons/${lessonId}`, json('PUT', { position })),
  removeLesson: (lessonId) => api(`/api/admin/lessons/${lessonId}`, { method: 'DELETE' }),
};
