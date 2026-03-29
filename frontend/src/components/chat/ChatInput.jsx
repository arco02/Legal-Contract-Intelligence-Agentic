import { useState, useRef, useEffect } from 'react'
import { Send } from 'lucide-react'

// CHANGED: onSend is now onSubmit
export default function ChatInput({ onSubmit, disabled }) {
  const [text, setText] = useState('')
  const textareaRef     = useRef(null)

  // Auto-resize textarea as user types
  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 160) + 'px'
  }, [text])

  const handleSend = () => {
    const trimmed = text.trim()
    if (!trimmed || disabled) return
    // CHANGED: Call onSubmit instead of onSend
    onSubmit(trimmed)
    setText('')
  }

  const handleKeyDown = (e) => {
    // Send on Enter, new line on Shift+Enter
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div style={styles.wrapper}>
      <div style={{
        ...styles.inputRow,
        opacity: disabled ? 0.6 : 1,
      }}>
        <textarea
          ref={textareaRef}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Ask a question about your contracts..."
          disabled={disabled}
          rows={1}
          style={styles.textarea}
        />
        <button
          onClick={handleSend}
          disabled={disabled || !text.trim()}
          style={{
            ...styles.sendBtn,
            opacity: disabled || !text.trim() ? 0.4 : 1,
          }}
        >
          <Send size={16} />
        </button>
      </div>
      <p style={styles.disclaimer}>
        Informational only — not legal advice.
      </p>
    </div>
  )
}

const styles = {
  wrapper: {
    padding: '12px 16px 16px',
    borderTop: '1px solid var(--border)',
    background: 'var(--bg-primary)',
  },
  inputRow: {
    display: 'flex',
    alignItems: 'flex-end',
    gap: '10px',
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '10px 12px',
  },
  textarea: {
    flex: 1,
    background: 'transparent',
    border: 'none',
    outline: 'none',
    color: 'var(--text-primary)',
    fontSize: '14px',
    lineHeight: '1.5',
    resize: 'none',
    maxHeight: '160px',
    overflowY: 'auto',
    fontFamily: 'inherit',
  },
  sendBtn: {
    background: 'var(--accent)',
    border: 'none',
    borderRadius: '8px',
    width: '34px',
    height: '34px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#fff',
    flexShrink: 0,
    transition: 'opacity 0.15s',
  },
  disclaimer: {
    fontSize: '11px',
    color: 'var(--text-secondary)',
    textAlign: 'center',
    marginTop: '8px',
  },
}