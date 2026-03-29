import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { loginUser } from '../api/authApi'
import useAuthStore from '../store/authStore'

export default function LoginPage() {
  const navigate = useNavigate()
  const setAuth  = useAuthStore((state) => state.setAuth)

  const [email,    setEmail]    = useState('')
  const [password, setPassword] = useState('')
  const [error,    setError]    = useState('')
  const [loading,  setLoading]  = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await loginUser(email, password)
      setAuth(data.token, {
        userId:   data.userId,
        email:    data.email,
        fullName: data.fullName,
      })
      navigate('/chat')
    } catch (err) {
      setError(err.response?.data?.error || 'Login failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.card}>

        <div style={styles.header}>
          <div style={styles.logo}>⚖️</div>
          <h1 style={styles.title}>Legal RAG Assistant</h1>
          <p style={styles.subtitle}>Sign in to your account</p>
        </div>

        <form onSubmit={handleSubmit} style={styles.form}>

          <div style={styles.field}>
            <label style={styles.label}>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              style={styles.input}
            />
          </div>

          <div style={styles.field}>
            <label style={styles.label}>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              style={styles.input}
            />
          </div>

          {error && <p style={styles.error}>{error}</p>}

          <button
            type="submit"
            disabled={loading}
            style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </button>

        </form>

        <p style={styles.footer}>
          Don't have an account?{' '}
          <Link to="/register">Register</Link>
        </p>

      </div>
    </div>
  )
}

const styles = {
  page: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'var(--bg-primary)',
    padding: '24px',
  },
  card: {
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '40px',
    width: '100%',
    maxWidth: '420px',
    boxShadow: 'var(--shadow)',
  },
  header: {
    textAlign: 'center',
    marginBottom: '32px',
  },
  logo: {
    fontSize: '40px',
    marginBottom: '12px',
  },
  title: {
    fontSize: '22px',
    fontWeight: '700',
    color: 'var(--text-primary)',
    marginBottom: '6px',
  },
  subtitle: {
    color: 'var(--text-secondary)',
    fontSize: '14px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '18px',
  },
  field: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  label: {
    fontSize: '13px',
    fontWeight: '500',
    color: 'var(--text-secondary)',
  },
  input: {
    background: 'var(--bg-tertiary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius)',
    padding: '10px 14px',
    color: 'var(--text-primary)',
    outline: 'none',
    width: '100%',
  },
  error: {
    color: 'var(--danger)',
    fontSize: '13px',
    textAlign: 'center',
  },
  button: {
    background: 'var(--accent)',
    color: '#fff',
    border: 'none',
    borderRadius: 'var(--radius)',
    padding: '11px',
    fontWeight: '600',
    fontSize: '14px',
    marginTop: '4px',
    transition: 'background 0.2s',
  },
  footer: {
    textAlign: 'center',
    marginTop: '24px',
    color: 'var(--text-secondary)',
    fontSize: '13px',
  },
}
