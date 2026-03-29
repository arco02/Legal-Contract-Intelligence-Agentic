import { useState } from 'react'
import { deleteDocument } from '../../api/documentApi'
import { FileText, Trash2, Clock, CheckCircle, XCircle, Loader } from 'lucide-react'

const STATUS_ICON = {
  COMPLETED:  <CheckCircle size={14} color="var(--success)" />,
  PROCESSING: <Loader      size={14} color="var(--warning)" />,
  PENDING:    <Clock       size={14} color="var(--text-secondary)" />,
  FAILED:     <XCircle     size={14} color="var(--danger)" />,
}

const TYPE_COLORS = {
  COMMERCIAL:   '#5b7cfa',
  CORPORATE_IP: '#9c6fda',
  OPERATIONAL:  '#4caf82',
}

export default function DocumentList({ documents, onDeleted }) {
  const [deletingId, setDeletingId] = useState(null)

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this document and all its data?')) return
    setDeletingId(id)
    try {
      await deleteDocument(id)
      if (onDeleted) onDeleted(id)
    } catch {
      alert('Failed to delete document.')
    } finally {
      setDeletingId(null)
    }
  }

  if (documents.length === 0) {
    return (
      <div style={styles.empty}>
        <FileText size={32} color="var(--text-secondary)" />
        <p>No documents uploaded yet.</p>
      </div>
    )
  }

  return (
    <div style={styles.list}>
      {documents.map((doc) => (
        <div key={doc.id} style={styles.item}>

          <div style={styles.icon}>
            <FileText size={18} color="var(--text-secondary)" />
          </div>

          <div style={styles.info}>
            <p style={styles.title}>{doc.title}</p>
            <div style={styles.meta}>
              <span style={{
                ...styles.typeBadge,
                background: TYPE_COLORS[doc.contractType] + '22',
                color: TYPE_COLORS[doc.contractType],
              }}>
                {doc.contractType}
              </span>
              <span style={styles.metaText}>
                {doc.totalPages ? `${doc.totalPages} pages` : ''}
              </span>
              <span style={styles.metaText}>
                {new Date(doc.uploadedAt).toLocaleDateString()}
              </span>
            </div>
          </div>

          <div style={styles.status}>
            {STATUS_ICON[doc.ingestionStatus] || STATUS_ICON.PENDING}
            <span style={styles.statusText}>{doc.ingestionStatus}</span>
          </div>

          <button
            style={{
              ...styles.deleteBtn,
              opacity: deletingId === doc.id ? 0.5 : 1,
            }}
            onClick={() => handleDelete(doc.id)}
            disabled={deletingId === doc.id}
            title="Delete document"
          >
            <Trash2 size={15} />
          </button>

        </div>
      ))}
    </div>
  )
}

const styles = {
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
  },
  item: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius)',
    padding: '12px 14px',
  },
  icon: {
    flexShrink: 0,
  },
  info: {
    flex: 1,
    overflow: 'hidden',
  },
  title: {
    fontSize: '14px',
    fontWeight: '500',
    color: 'var(--text-primary)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    marginBottom: '4px',
  },
  meta: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    flexWrap: 'wrap',
  },
  typeBadge: {
    fontSize: '11px',
    fontWeight: '600',
    padding: '2px 7px',
    borderRadius: '4px',
  },
  metaText: {
    fontSize: '12px',
    color: 'var(--text-secondary)',
  },
  status: {
    display: 'flex',
    alignItems: 'center',
    gap: '5px',
    flexShrink: 0,
  },
  statusText: {
    fontSize: '12px',
    color: 'var(--text-secondary)',
  },
  deleteBtn: {
    background: 'none',
    border: 'none',
    color: 'var(--danger)',
    display: 'flex',
    alignItems: 'center',
    padding: '4px',
    flexShrink: 0,
  },
  empty: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '12px',
    padding: '40px',
    color: 'var(--text-secondary)',
    fontSize: '14px',
  },
}
