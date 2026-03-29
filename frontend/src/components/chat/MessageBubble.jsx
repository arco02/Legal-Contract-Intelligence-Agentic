import ReactMarkdown from 'react-markdown'
import SourceCard from './SourceCard'
import { Globe } from 'lucide-react'

export default function MessageBubble({ message }) {
  const isUser = message.role === 'user'

  return (
    <div style={{
      ...styles.wrapper,
      justifyContent: isUser ? 'flex-end' : 'flex-start',
    }}>
      <div style={{
        ...styles.bubble,
        ...(isUser ? styles.userBubble : styles.assistantBubble),
      }}>

        {/* Message content */}
        {isUser ? (
          <p style={styles.userText}>{message.content}</p>
        ) : (
          <div style={styles.markdown}>
            <ReactMarkdown>{message.content}</ReactMarkdown>
          </div>
        )}

        {/* Web search indicator */}
        {message.isWebSearch && (
          <div style={styles.webBadge}>
            <Globe size={12} />
            <span>Answer from web search</span>
          </div>
        )}

        {/* Sources */}
        {message.sources && message.sources.length > 0 && (
          <div style={styles.sourcesSection}>
            <p style={styles.sourcesLabel}>Sources</p>
            <div style={styles.sourcesRow}>
              {message.sources.map((source, i) => (
                <SourceCard key={i} source={source} />
              ))}
            </div>
          </div>
        )}

      </div>
    </div>
  )
}

const styles = {
  wrapper: {
    display: 'flex',
    marginBottom: '16px',
    padding: '0 16px',
  },
  bubble: {
    maxWidth: '75%',
    borderRadius: 'var(--radius-lg)',
    padding: '12px 16px',
    lineHeight: '1.6',
  },
  userBubble: {
    background: 'var(--accent)',
    color: '#fff',
    borderBottomRightRadius: '4px',
  },
  assistantBubble: {
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border)',
    color: 'var(--text-primary)',
    borderBottomLeftRadius: '4px',
    maxWidth: '85%',
  },
  userText: {
    fontSize: '14px',
    margin: 0,
  },
  markdown: {
    fontSize: '14px',
    color: 'var(--text-primary)',
    lineHeight: '1.7',
  },
  webBadge: {
    display: 'flex',
    alignItems: 'center',
    gap: '5px',
    marginTop: '10px',
    fontSize: '11px',
    color: 'var(--warning)',
  },
  sourcesSection: {
    marginTop: '14px',
    borderTop: '1px solid var(--border)',
    paddingTop: '12px',
  },
  sourcesLabel: {
    fontSize: '11px',
    fontWeight: '600',
    color: 'var(--text-secondary)',
    letterSpacing: '0.06em',
    marginBottom: '8px',
  },
  sourcesRow: {
    display: 'flex',
    gap: '8px',
    flexWrap: 'wrap',
  },
}
