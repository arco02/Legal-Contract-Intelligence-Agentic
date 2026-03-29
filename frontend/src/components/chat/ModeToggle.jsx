export default function ModeToggle({ mode, onChange }) {
  return (
    <div style={styles.wrapper}>
      <button
        style={{
          ...styles.btn,
          ...(mode === 'ASK' ? styles.active : styles.inactive),
        }}
        onClick={() => onChange('ASK')}
      >
        Ask
      </button>
      <button
        style={{
          ...styles.btn,
          ...(mode === 'SEARCH' ? styles.active : styles.inactive),
        }}
        onClick={() => onChange('SEARCH')}
      >
        Search
      </button>
    </div>
  )
}

const styles = {
  wrapper: {
    display: 'flex',
    background: 'var(--bg-tertiary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius)',
    padding: '3px',
    gap: '2px',
  },
  btn: {
    padding: '5px 14px',
    border: 'none',
    borderRadius: '6px',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'background 0.15s, color 0.15s',
  },
  active: {
    background: 'var(--accent)',
    color: '#fff',
  },
  inactive: {
    background: 'transparent',
    color: 'var(--text-secondary)',
  },
}
