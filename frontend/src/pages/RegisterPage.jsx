import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { registerUser } from '../api/authApi'
import useAuthStore from '../store/authStore'

export default function RegisterPage() {
  const navigate = useNavigate()
  const setAuth  = useAuthStore((state) => state.setAuth)

  const [fullName, setFullName] = useState('')
  const [email,    setEmail]    = useState('')
  const [password, setPassword] = useState('')
  const [error,    setError]    = useState('')
  const [loading,  setLoading]  = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }
    setLoading(true)
    try {
      const data = await registerUser(email, password, fullName)
      setAuth(data.token, {
        userId:   data.userId,
        email:    data.email,
        fullName: data.fullName,
      })
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed. Please try again.')
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
          <p style={styles.subtitle}>Create your account</p>
        </div>

        <form onSubmit={handleSubmit} style={styles.form}>

          <div style={styles.field}>
            <label style={styles.label}>Full Name</label>
            <input
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="John Doe"
              required
              style={styles.input}
            />
          </div>

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
              placeholder="Min. 8 characters"
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
            {loading ? 'Creating account...' : 'Create Account'}
          </button>

        </form>

        <p style={styles.footer}>
          Already have an account?{' '}
          <Link to="/login">Sign in</Link>
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
