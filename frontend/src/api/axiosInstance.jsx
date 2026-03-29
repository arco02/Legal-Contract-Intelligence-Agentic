import axios from 'axios'

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

// ── Request interceptor — attach JWT to every request ─────────────────────
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response interceptor — handle 401 globally ────────────────────────────
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('jwt')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default axiosInstance