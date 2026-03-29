import { create } from 'zustand'

const useAuthStore = create((set) => ({
  token: localStorage.getItem('jwt') || null,
  user: JSON.parse(localStorage.getItem('user') || 'null'),

  setAuth: (token, user) => {
    localStorage.setItem('jwt', token)
    localStorage.setItem('user', JSON.stringify(user))
    set({ token, user })
  },

  clearAuth: () => {
    localStorage.removeItem('jwt')
    localStorage.removeItem('user')
    set({ token: null, user: null })
  },

  isAuthenticated: () => !!localStorage.getItem('jwt'),
}))

export default useAuthStore
