import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { fetchConversations, deleteConversation } from '../../api/chatApi'
import { fetchDocuments } from '../../api/documentApi'
import useAuthStore from '../../store/authStore'
import { MessageSquare, FileText, Plus, Trash2, LogOut, ChevronDown } from 'lucide-react'

export default function Sidebar({ onDocumentSelect, selectedDocumentId }) {
  const navigate    = useNavigate()
  const { conversationId } = useParams()
  const clearAuth   = useAuthStore((state) => state.clearAuth)
  const user        = useAuthStore((state) => state.user)

  const [conversations, setConversations] = useState([])
  const [documents,     setDocuments]     = useState([])
  const [docOpen,       setDocOpen]       = useState(false)

  useEffect(() => {
    loadConversations()
    loadDocuments()
  }, [])

  const loadConversations = async () => {
    try {
      const data = await fetchConversations()
      setConversations(data)
    } catch {
      // silently fail — user just won't see history
    }
  }

  const loadDocuments = async () => {
    try {
      const data = await fetchDocuments()
      setDocuments(data)
    } catch {}
  }

  const handleDeleteConversation = async (e, id) => {
    e.stopPropagation()
    try {
      await deleteConversation(id)
      setConversations((prev) => prev.filter((c) => c.id !== id))
      if (conversationId === id) navigate('/chat')
    } catch {}
  }

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  const handleDocumentSelect = (docId) => {
    onDocumentSelect(docId)
    setDocOpen(false)
  }

  const selectedDoc = documents.find((d) => d.id === selectedDocumentId)
  const dropdownLabel = selectedDoc ? selectedDoc.title : 'All Documents'

  return (
    <aside style={styles.sidebar}>

      {/* Header - Now clickable to return to Dashboard */}
      <div 
        style={{...styles.header, cursor: 'pointer'}} 
        onClick={() => navigate('/')}
        title="Go to Dashboard (Upload Contracts)"
      >
        <span style={styles.logo}>⚖️</span>
        <span style={styles.appName}>Legal RAG</span>
      </div>

      {/* Document scope selector */}
      <div style={styles.section}>
        <p style={styles.sectionLabel}>SCOPE</p>
        <div style={styles.dropdownWrapper}>
          <button
            style={styles.dropdownBtn}
            onClick={() => setDocOpen((v) => !v)}
          >
            <FileText size={14} color="var(--text-secondary)" />
            <span style={styles.dropdownLabel}>{dropdownLabel}</span>
            <ChevronDown size={14} color="var(--text-secondary)" />
          </button>

          {docOpen && (
            <div style={styles.dropdownMenu}>
              <div
                style={{
                  ...styles.dropdownItem,
                  ...(selectedDocumentId === null ? styles.dropdownItemActive : {}),
                }}
                onClick={() => handleDocumentSelect(null)}
              >
                All Documents
              </div>
              {documents.map((doc) => (
                <div
                  key={doc.id}
                  style={{
                    ...styles.dropdownItem,
                    ...(selectedDocumentId === doc.id ? styles.dropdownItemActive : {}),
                  }}
                  onClick={() => handleDocumentSelect(doc.id)}
                >
                  <span style={styles.dropdownDocTitle}>{doc.title}</span>
                  <span style={styles.dropdownDocType}>{doc.contractType}</span>
                </div>
              ))}
              {documents.length === 0 && (
                <div style={styles.dropdownEmpty}>No documents uploaded</div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* New chat button */}
      <button 
        style={styles.newChatBtn} 
        onClick={() => navigate('/chat')}
      >
        <Plus size={15} />
        New Chat
      </button>

      {/* Conversations */}
      <div style={styles.section}>
        <p style={styles.sectionLabel}>CONVERSATIONS</p>
        <div style={styles.list}>
          {conversations.length === 0 && (
            <p style={styles.empty}>No conversations yet</p>
          )}
          {conversations.map((conv) => (
            <div
              key={conv.id}
              style={{
                ...styles.convItem,
                ...(conversationId === conv.id ? styles.convItemActive : {}),
              }}
              onClick={() => navigate(`/chat/${conv.id}`)}
            >
              <MessageSquare size={13} style={{ flexShrink: 0 }} />
              <span style={styles.convTitle}>{conv.title}</span>
              <button
                style={styles.deleteBtn}
                onClick={(e) => handleDeleteConversation(e, conv.id)}
              >
                <Trash2 size={12} />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Footer — user info + logout */}
      <div style={styles.footer}>
        <div style={styles.userInfo}>
          <div style={styles.avatar}>
            {user?.fullName?.[0]?.toUpperCase() || '?'}
          </div>
          <div style={styles.userDetails}>
            <p style={styles.userName}>{user?.fullName}</p>
            <p style={styles.userEmail}>{user?.email}</p>
          </div>
        </div>
        <button style={styles.logoutBtn} onClick={handleLogout} title="Sign out">
          <LogOut size={16} />
        </button>
      </div>

    </aside>
  )
}

const styles = {
  sidebar: {
    width: '260px',
    minWidth: '260px',
    height: '100vh',
    background: 'var(--bg-secondary)',
    borderRight: '1px solid var(--border)',
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '20px 16px 16px',
    borderBottom: '1px solid var(--border)',
  },
  logo: {
    fontSize: '22px',
  },
  appName: {
    fontWeight: '700',
    fontSize: '16px',
    color: 'var(--text-primary)',
  },
  section: {
    padding: '12px 16px 4px',
  },
  sectionLabel: {
    fontSize: '11px',
    fontWeight: '600',
    color: 'var(--text-secondary)',
    letterSpacing: '0.08em',
    marginBottom: '8px',
  },
  dropdownWrapper: {
    position: 'relative',
  },
  dropdownBtn: {
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    background: 'var(--bg-tertiary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius)',
    padding: '8px 10px',
    color: 'var(--text-primary)',
    cursor: 'pointer',
  },
  dropdownLabel: {
    flex: 1,
    textAlign: 'left',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    fontSize: '13px',
  },
  dropdownMenu: {
    position: 'absolute',
    top: 'calc(100% + 4px)',
    left: 0,
    right: 0,
    background: 'var(--bg-tertiary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius)',
    zIndex: 100,
    maxHeight: '200px',
    overflowY: 'auto',
    boxShadow: 'var(--shadow)',
  },
  dropdownItem: {
    padding: '8px 12px',
    cursor: 'pointer',
    display: 'flex',
    flexDirection: 'column',
    gap: '2px',
    borderBottom: '1px solid var(--border)',
  },
  dropdownItemActive: {
    background: 'var(--accent)',
    color: '#fff',
  },
  dropdownDocTitle: {
    fontSize: '13px',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  dropdownDocType: {
    fontSize: '11px',
    color: 'var(--text-secondary)',
  },
  dropdownEmpty: {
    padding: '10px 12px',
    color: 'var(--text-secondary)',
    fontSize: '12px',
  },
  newChatBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    margin: '12px 16px 4px',
    padding: '9px 14px',
    background: 'var(--accent)',
    color: '#fff',
    border: 'none',
    borderRadius: 'var(--radius)',
    fontWeight: '600',
    fontSize: '13px',
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px',
    maxHeight: 'calc(100vh - 380px)',
    overflowY: 'auto',
  },
  empty: {
    color: 'var(--text-secondary)',
    fontSize: '12px',
    padding: '4px 2px',
  },
  convItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '8px 10px',
    borderRadius: 'var(--radius)',
    cursor: 'pointer',
    color: 'var(--text-secondary)',
    fontSize: '13px',
  },
  convItemActive: {
    background: 'var(--bg-tertiary)',
    color: 'var(--text-primary)',
  },
  convTitle: {
    flex: 1,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  deleteBtn: {
    background: 'none',
    border: 'none',
    color: 'var(--text-secondary)',
    padding: '2px',
    display: 'flex',
    alignItems: 'center',
    opacity: 0,
    transition: 'opacity 0.15s',
  },
  footer: {
    marginTop: 'auto',
    borderTop: '1px solid var(--border)',
    padding: '12px 16px',
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
  },
  userInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    flex: 1,
    overflow: 'hidden',
  },
  avatar: {
    width: '32px',
    height: '32px',
    borderRadius: '50%',
    background: 'var(--accent)',
    color: '#fff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: '700',
    fontSize: '13px',
    flexShrink: 0,
  },
  userDetails: {
    overflow: 'hidden',
  },
  userName: {
    fontSize: '13px',
    fontWeight: '600',
    color: 'var(--text-primary)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  userEmail: {
    fontSize: '11px',
    color: 'var(--text-secondary)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  logoutBtn: {
    background: 'none',
    border: 'none',
    color: 'var(--text-secondary)',
    display: 'flex',
    alignItems: 'center',
    padding: '4px',
    flexShrink: 0,
  },
}