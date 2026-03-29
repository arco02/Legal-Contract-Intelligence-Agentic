import { useEffect, useRef } from 'react'
import MessageBubble from './MessageBubble'
import SourceCard from './SourceCard'
import { streamingContentRef } from '../../hooks/useChat'
import { Loader } from 'lucide-react'
import ReactMarkdown from 'react-markdown'

export default function ChatWindow({
  messages,
  streamingContent,
  isStreaming,
  isLoading,
  mode,
  searchResults,
}) {
  const bottomRef = useRef(null)

  // Keep latest streaming content in ref so useChat's onDone can read it
  useEffect(() => {
    streamingContentRef.current = streamingContent
  }, [streamingContent])

  // Auto-scroll to bottom on new messages or streaming tokens
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, streamingContent])

  // ── Loading spinner (initial conversation load) ───────────────────────────
  if (isLoading) {
    return (
      <div style={styles.centered}>
        <Loader size={24} color="var(--text-secondary)" />
      </div>
    )
  }

  // ── SEARCH mode results ───────────────────────────────────────────────────
  if (mode === 'SEARCH') {
    return (
      <div style={styles.window}>
        {searchResults.length === 0 ? (
          <div style={styles.centered}>
            <p style={styles.hint}>
              Search returns ranked contract clauses — no AI synthesis.
            </p>
          </div>
        ) : (
          <div style={styles.searchResults}>
            {searchResults.map((result, i) => (
              <div key={i} style={styles.searchItem}>
                <div style={styles.searchMeta}>
                  <SourceCard source={{
                    type:         'CONTRACT',
                    title:        result.documentTitle,
                    page:         result.pageNumber,
                    contractType: result.contractType,
                    score:        result.score,
                  }} />
                </div>
                <p style={styles.searchContent}>{result.content}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    )
  }

  // ── ASK mode — empty state ────────────────────────────────────────────────
  if (messages.length === 0 && !isStreaming) {
    return (
      <div style={styles.centered}>
        <div style={styles.emptyState}>
          <span style={styles.emptyIcon}>⚖️</span>
          <h2 style={styles.emptyTitle}>Legal RAG Assistant</h2>
          <p style={styles.emptySubtitle}>
            Ask questions about your contracts or query Indian law.
          </p>
          <div style={styles.examples}>
            {[
              'Which contracts allow termination without cause?',
              'Is this indemnification clause enforceable under Indian law?',
              'Do any contracts expire in the next 3 months?',
            ].map((ex) => (
              <p key={ex} style={styles.exampleChip}>{ex}</p>
            ))}
          </div>
        </div>
      </div>
    )
  }

  // ── ASK mode — messages + streaming ──────────────────────────────────────
  return (
    <div style={styles.window}>

      {messages.map((msg) => (
        <MessageBubble key={msg.id} message={msg} />
      ))}

      {/* Streaming assistant bubble */}
      {isStreaming && (
        <div style={styles.streamingWrapper}>
          <div style={styles.streamingBubble}>
            {streamingContent ? (
              <div style={styles.markdown}>
                <ReactMarkdown>{streamingContent}</ReactMarkdown>
              </div>
            ) : (
              <div style={styles.typingDots}>
                <span /><span /><span />
              </div>
            )}
          </div>
        </div>
      )}

      <div ref={bottomRef} />
    </div>
  )
}

const styles = {
  window: {
    flex: 1,
    overflowY: 'auto',
    padding: '20px 0 8px',
  },
  centered: {
    flex: 1,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '40px 24px',
  },
  hint: {
    color: 'var(--text-secondary)',
    fontSize: '14px',
    textAlign: 'center',
  },
  emptyState: {
    textAlign: 'center',
    maxWidth: '480px',
  },
  emptyIcon: {
    fontSize: '48px',
    display: 'block',
    marginBottom: '16px',
  },
  emptyTitle: {
    fontSize: '22px',
    fontWeight: '700',
    color: 'var(--text-primary)',
    marginBottom: '8px',
  },
  emptySubtitle: {
    fontSize: '14px',
    color: 'var(--text-secondary)',
    marginBottom: '24px',
  },
  examples: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
  },
  exampleChip: {
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius)',
    padding: '10px 14px',
    fontSize: '13px',
    color: 'var(--text-secondary)',
    cursor: 'default',
  },
  searchResults: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    padding: '16px',
  },
  searchItem: {
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  searchMeta: {
    display: 'flex',
  },
  searchContent: {
    fontSize: '13px',
    color: 'var(--text-primary)',
    lineHeight: '1.7',
    whiteSpace: 'pre-wrap',
  },
  streamingWrapper: {
    display: 'flex',
    justifyContent: 'flex-start',
    marginBottom: '16px',
    padding: '0 16px',
  },
  streamingBubble: {
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    borderBottomLeftRadius: '4px',
    padding: '12px 16px',
    maxWidth: '85%',
  },
  markdown: {
    fontSize: '14px',
    color: 'var(--text-primary)',
    lineHeight: '1.7',
  },
  typingDots: {
    display: 'flex',
    gap: '4px',
    alignItems: 'center',
    height: '20px',
    '& span': {
      width: '6px',
      height: '6px',
      borderRadius: '50%',
      background: 'var(--text-secondary)',
      animation: 'pulse 1.2s infinite',
    },
  },
}
