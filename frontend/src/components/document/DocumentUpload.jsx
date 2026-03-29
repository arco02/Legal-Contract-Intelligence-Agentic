import { useState } from 'react'
import { uploadDocument } from '../../api/documentApi'
import { UploadCloud, X } from 'lucide-react'

const CONTRACT_TYPES = [
  { value: 'COMMERCIAL',   label: 'Commercial' },
  { value: 'CORPORATE_IP', label: 'Corporate / IP' },
  { value: 'OPERATIONAL',  label: 'Operational' },
]

export default function DocumentUpload({ onUploadSuccess }) {
  const [file,         setFile]         = useState(null)
  const [title,        setTitle]        = useState('')
  const [contractType, setContractType] = useState('COMMERCIAL')
  const [loading,      setLoading]      = useState(false)
  const [error,        setError]        = useState('')
  const [success,      setSuccess]      = useState('')

  const handleFileChange = (e) => {
    const selected = e.target.files[0]
    if (!selected) return
    if (selected.type !== 'application/pdf') {
      setError('Only PDF files are supported.')
      return
    }
    setFile(selected)
    setError('')
    // Auto-fill title from filename if empty
    if (!title) {
      setTitle(selected.name.replace('.pdf', ''))
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!file) { setError('Please select a PDF file.'); return }
    if (!title.trim()) { setError('Please enter a title.'); return }

    setError('')
    setSuccess('')
    setLoading(true)

    try {
      await uploadDocument(file, contractType, title.trim())
      setSuccess('Document uploaded and processing started.')
      setFile(null)
      setTitle('')
      setContractType('COMMERCIAL')
      if (onUploadSuccess) onUploadSuccess()
    } catch (err) {
      setError(err.response?.data?.error || 'Upload failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.card}>
      <h2 style={styles.heading}>Upload Contract</h2>

      <form onSubmit={handleSubmit} style={styles.form}>

        {/* File drop zone */}
        <label style={styles.dropZone}>
          <input
            type="file"
            accept="application/pdf"
            onChange={handleFileChange}
            style={{ display: 'none' }}
          />
          {file ? (
            <div style={styles.fileSelected}>
              <span style={styles.fileName}>{file.name}</span>
              <button
                type="button"
                style={styles.clearFile}
                onClick={(e) => { e.preventDefault(); setFile(null); setTitle('') }}
              >
                <X size={14} />
              </button>
            </div>
          ) : (
            <div style={styles.dropPrompt}>
              <UploadCloud size={28} color="var(--text-secondary)" />
              <p style={styles.dropText}>Click to select a PDF</p>
              <p style={styles.dropSubtext}>Max 20 MB</p>
            </div>
          )}
        </label>

        {/* Title */}
        <div style={styles.field}>
          <label style={styles.label}>Document Title</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Vendor NDA 2024"
            style={styles.input}
          />
        </div>

        {/* Contract type */}
        <div style={styles.field}>
          <label style={styles.label}>Contract Type</label>
          <select
            value={contractType}
            onChange={(e) => setContractType(e.target.value)}
            style={styles.select}
          >
            {CONTRACT_TYPES.map((t) => (
              <option key={t.value} value={t.value}>{t.label}</option>
            ))}
          </select>
        </div>

        {error   && <p style={styles.error}>{error}</p>}
        {success && <p style={styles.success}>{success}</p>}

        <button
          type="submit"
          disabled={loading}
          style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}
        >
          {loading ? 'Uploading...' : 'Upload'}
        </button>

      </form>
    </div>
  )
}

const styles = {
  card: {
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius-lg)',
    padding: '24px',
  },
  heading: {
    fontSize: '16px',
    fontWeight: '600',
    color: 'var(--text-primary)',
    marginBottom: '20px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },
  dropZone: {
    display: 'block',
    border: '2px dashed var(--border)',
    borderRadius: 'var(--radius)',
    padding: '24px',
    cursor: 'pointer',
    textAlign: 'center',
    background: 'var(--bg-tertiary)',
    transition: 'border-color 0.2s',
  },
  dropPrompt: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '8px',
  },
  dropText: {
    color: 'var(--text-secondary)',
    fontSize: '14px',
  },
  dropSubtext: {
    color: 'var(--text-secondary)',
    fontSize: '12px',
  },
  fileSelected: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '8px',
  },
  fileName: {
    fontSize: '13px',
    color: 'var(--text-primary)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  clearFile: {
    background: 'none',
    border: 'none',
    color: 'var(--text-secondary)',
    display: 'flex',
    alignItems: 'center',
    flexShrink: 0,
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
    padding: '9px 12px',
    color: 'var(--text-primary)',
    outline: 'none',
  },
  select: {
    background: 'var(--bg-tertiary)',
    border: '1px solid var(--border)',
    borderRadius: 'var(--radius)',
    padding: '9px 12px',
    color: 'var(--text-primary)',
    outline: 'none',
  },
  error: {
    color: 'var(--danger)',
    fontSize: '13px',
  },
  success: {
    color: 'var(--success)',
    fontSize: '13px',
  },
  button: {
    background: 'var(--accent)',
    color: '#fff',
    border: 'none',
    borderRadius: 'var(--radius)',
    padding: '10px',
    fontWeight: '600',
    fontSize: '14px',
  },
}
