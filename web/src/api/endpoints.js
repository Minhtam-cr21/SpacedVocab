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
