export const API_BASE_URL = import.meta.env.DEV
  ? 'http://localhost:8123/api'
  : '/api'

export const resolveApiUrl = (path) => {
  if (!path) {
    return ''
  }
  if (/^https?:\/\//.test(path)) {
    return path
  }
  return `${API_BASE_URL}${path}`
}

export const connectSSE = (url, params = {}) => {
  const queryString = new URLSearchParams(params).toString()
  const fullUrl = `${API_BASE_URL}${url}${queryString ? `?${queryString}` : ''}`
  return new EventSource(fullUrl)
}

export const chatWithLoveApp = (message, chatId) => {
  return connectSSE('/ai/love_app/chat/sse', { message, chatId })
}

export const chatWithManus = (message) => {
  return connectSSE('/ai/manus/chat', { message })
}

export default {
  chatWithLoveApp,
  chatWithManus,
  resolveApiUrl
}