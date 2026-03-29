import { FileText, BookOpen } from 'lucide-react'

const TYPE_COLORS = {
  COMMERCIAL:   { bg: '#5b7cfa22', color: '#5b7cfa' },
  CORPORATE_IP: { bg: '#9c6fda22', color: '#9c6fda' },
  OPERATIONAL:  { bg: '#4caf8222', color: '#4caf82' },
  STATUTE:      { bg: '#f0a04b22', color: '#f0a04b' },
  CASE_LAW:     { bg: '#e05c5c22', color: '#e05c5c' },
  REGULATION:   { bg: '#4caf8222', color: '#4caf82' },
}

export default function SourceCard({ source }) {
  const isContract = source.type === 'CONTRACT'
  const colors     = TYPE_COLORS[source.contractType || source.lawType] ||
                     { bg: 'var(--bg-tertiary)', color: 'var(--text-secondary)' }

  return (
    <div style={styles.card}>

      <div style={styles.iconWrap}>
        {isContract
          ? <FileText size={14} color={colors.color} />
          : <BookOpen  size={14} color={colors.color} />
        }
      </div>

      <div style={styles.info}>
        <p style={styles.title}>{source.title}</p>
        <div style={styles.meta}>
          {isContract && source.page && (
            <span style={styles.metaText}>p. {source.page}</span>
          )}
          {!isContract && source.section && (
            <span style={styles.metaText}>{source.section}</span>
          )}
          <span style={{
            ...styles.badge,
            background: colors.bg,
            color: colors.color,
          }}>
            {source.contractType || source.lawType}
          </span>
          {source.score !== undefined && (
            <span style={styles.score}>
              {(source.score * 100).toFixed(0)}% match
            </span>
          )}
        </div>
      </div>

    </div>
  )
}

const styles = {
  card: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '10px',
    background: 'var(--bg-tertiary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius)',
    padding: '10px 12px',
    minWidth: '180px',
    maxWidth: '240px',
    flexShrink: 0,
  },
  iconWrap: {
    marginTop: '2px',
    flexShrink: 0,
  },
  info: {
    overflow: 'hidden',
  },
  title: {
    fontSize: '12px',
    fontWeight: '600',
    color: 'var(--text-primary)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    marginBottom: '4px',
  },
  meta: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    flexWrap: 'wrap',
  },
  metaText: {
    fontSize: '11px',
    color: 'var(--text-secondary)',
  },
  badge: {
    fontSize: '10px',
    fontWeight: '600',
    padding: '1px 6px',
    borderRadius: '4px',
  },
  score: {
    fontSize: '11px',
    color: 'var(--text-secondary)',
  },
}
